package com.ruoyi.web.controller.activiti;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Leaveapply;
import com.ruoyi.system.domain.ModelParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.query.QueryProperty;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ModelQuery;
import org.apache.poi.util.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Api(value = "模型管理接口")
@Controller
@RequestMapping("/model/manage")
public class ModelManageController extends BaseController {

    @Resource
    RepositoryService repositoryService;

    @Resource
    private ObjectMapper objectMapper;

    private String prefix = "activiti/manage";


    @ApiOperation("查询所有模型")
    @RequestMapping(value = "/modelLists", method = RequestMethod.POST)
    @ResponseBody
    public TableDataInfo modelLists(@RequestParam(required = false) String key, @RequestParam(required = false) String name,
                                    Integer pageSize, Integer pageNum) {
        ModelQuery query = repositoryService.createModelQuery();
        if (StringUtils.isNotEmpty(key)) {
            query.modelKey(key);
        }
        if (StringUtils.isNotEmpty(name)) {
            query.modelName(name);
        }
        int start = (pageNum - 1) * pageSize;
        List<Model> page = query.orderByCreateTime().desc().listPage(start, pageSize);
        int total = repositoryService.createModelQuery().list().size();
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(0);
        rspData.setRows(page);
        rspData.setTotal(total);
        return rspData;
    }

    /**
     * 新增模型
     */
    @ApiOperation("新建模型")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(ModelParam modelRequest) throws JsonProcessingException {
        String key = modelRequest.getKey();

        // 检查模型 key 是否已存在
        ModelQuery modelQuery = repositoryService.createModelQuery();
        List<Model> existingModels = modelQuery.modelKey(key).list();
        if (existingModels.size() > 0) {
            return AjaxResult.error("模型标识已存在，请使用其他标识");
        }

        // 检查已部署流程的 key 是否已存在
        long deployedCount = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .count();
        if (deployedCount > 0) {
            return AjaxResult.error("该流程标识已被已部署的流程使用，请使用其他标识");
        }

        Model model = repositoryService.newModel();
        model.setCategory(modelRequest.getCategory());
        model.setKey(key);
        ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put(ModelDataJsonConstants.MODEL_NAME, modelRequest.getName());
        modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, modelRequest.getDescription());
        modelNode.put(ModelDataJsonConstants.MODEL_REVISION, modelRequest.getVersion());
        model.setMetaInfo(modelNode.toString());
        model.setName(modelRequest.getName());
        model.setVersion(modelRequest.getVersion());

        // 保存模型到act_re_model表
        repositoryService.saveModel(model);
        // 创建空的 BPMN 2.0 XML 模板
        String bpmnXml = createEmptyBpmnXml(key, modelRequest.getName(), modelRequest.getCategory());
        // 保存模型文件到act_ge_bytearray表
        repositoryService.addModelEditorSource(model.getId(), bpmnXml.getBytes(StandardCharsets.UTF_8));
        return AjaxResult.success(model);
    }

    @ApiOperation("发布模型")
    @RequestMapping("/deploy/{modelId}")
    @ResponseBody
    public AjaxResult modelDeployment(@PathVariable String modelId) {
        try {
            Model model = repositoryService.getModel(modelId);
            if (model == null) {
                return AjaxResult.error("模型不存在");
            }

            byte[] modelData = repositoryService.getModelEditorSource(modelId);
            if (modelData == null || modelData.length == 0) {
                return AjaxResult.error("模型数据为空，请先保存模型");
            }

            BpmnModel bpmnModel;
            String modelContent = new String(modelData, java.nio.charset.StandardCharsets.UTF_8);

            // 检测格式：JSON 还是 XML
            if (modelContent.trim().startsWith("{")) {
                // 旧的 JSON 格式
                JsonNode jsonNode = objectMapper.readTree(modelData);
                bpmnModel = (new BpmnJsonConverter()).convertToBpmnModel(jsonNode);
            } else {
                // 新的 XML 格式
                BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(modelData);
                javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
                javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
                bpmnModel = xmlConverter.convertToBpmnModel(reader);
            }

            // 检查模型是否有错误
            if (bpmnModel.getErrors() != null && !bpmnModel.getErrors().isEmpty()) {
                return AjaxResult.error("模型有错误: " + bpmnModel.getErrors().values());
            }

            // 检查是否有流程
            if (bpmnModel.getProcesses() == null || bpmnModel.getProcesses().isEmpty()) {
                return AjaxResult.error("模型中没有流程定义");
            }

            // 获取流程定义的 key（流程ID）
            String processKey = bpmnModel.getProcesses().get(0).getId();

            // 检查流程ID是否与模型key一致
            if (!processKey.equals(model.getKey())) {
                // 检查流程ID是否已被其他模型或已部署流程使用
                long deployedCount = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(processKey)
                        .count();
                if (deployedCount > 0) {
                    return AjaxResult.error("流程ID '" + processKey + "' 已被已部署的流程使用，请修改流程ID或使用新模型");
                }

                // 检查是否有其他模型使用了相同的key
                List<Model> otherModels = repositoryService.createModelQuery()
                        .modelKey(processKey)
                        .list();
                for (Model otherModel : otherModels) {
                    if (!otherModel.getId().equals(modelId)) {
                        return AjaxResult.error("流程ID '" + processKey + "' 已被其他模型使用，请修改流程ID");
                    }
                }
            }

            Deployment deploy = repositoryService.createDeployment()
                    .category(model.getCategory())
                    .name(model.getName())
                    .key(model.getKey())
                    .addBpmnModel(model.getKey() + ".bpmn20.xml", bpmnModel)
                    .deploy();

            model.setDeploymentId(deploy.getId());
            repositoryService.saveModel(model);

            return AjaxResult.success("发布成功，部署ID: " + deploy.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("发布失败: " + e.getMessage());
        }
    }

    @ApiOperation("删除模型")
    @PostMapping("/remove/{modelId}")
    @ResponseBody
    public AjaxResult removeModel(@PathVariable String modelId) {
        repositoryService.deleteModel(modelId);
        return AjaxResult.success("删除成功");
    }

    /**
     * 创建空的 BPMN 2.0 XML 模板
     *
     * @param processId   流程ID
     * @param processName 流程名称
     * @param category    分类
     * @return BPMN 2.0 XML 字符串
     */
    private String createEmptyBpmnXml(String processId, String processName, String category) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        xml.append("xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" ");
        xml.append("xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" ");
        xml.append("xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" ");
        xml.append("xmlns:activiti=\"http://activiti.org/bpmn\" ");
        xml.append("xmlns:modeler=\"http://activiti.org/modeler\" ");
        xml.append("targetNamespace=\"http://activiti.org/bpmn\">\n");
        xml.append("  <bpmn:process id=\"").append(escapeXml(processId)).append("\" ");
        xml.append("name=\"").append(escapeXml(processName)).append("\" ");
        xml.append("isExecutable=\"true\">\n");
        xml.append("  </bpmn:process>\n");
        xml.append("</bpmn:definitions>");
        return xml.toString();
    }

    /**
     * 转义 XML 特殊字符
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    @ApiOperation("导出模型")
    @GetMapping("/export/{modelId}")
    public void modelExport(@PathVariable String modelId, HttpServletResponse response) throws IOException {
        byte[] modelData = repositoryService.getModelEditorSource(modelId);
        String modelContent = new String(modelData, java.nio.charset.StandardCharsets.UTF_8);

        BpmnModel bpmnModel;
        byte[] xmlBytes;

        // 检测格式：JSON 还是 XML
        if (modelContent.trim().startsWith("{")) {
            // 旧的 JSON 格式，需要转换为 XML
            JsonNode jsonNode = objectMapper.readTree(modelData);
            bpmnModel = (new BpmnJsonConverter()).convertToBpmnModel(jsonNode);
            xmlBytes = (new BpmnXMLConverter()).convertToXML(bpmnModel, "UTF-8");
        } else {
            // 已经是 XML 格式，直接使用
            xmlBytes = modelData;
            // 解析以获取文件名
            BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(modelData);
            javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
            try {
                javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
                bpmnModel = xmlConverter.convertToBpmnModel(reader);
            } catch (Exception e) {
                throw new IOException("解析 BPMN XML 失败", e);
            }
        }

        ByteArrayInputStream in = new ByteArrayInputStream(xmlBytes);
        String filename = bpmnModel.getMainProcess().getId() + ".bpmn20.xml";
        response.setHeader("Content-Disposition","attachment;filename=" + filename);
        response.setHeader("content-Type", "application/xml");
        IOUtils.copy(in, response.getOutputStream());
    }


}
