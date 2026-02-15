package com.ruoyi.web.controller.activiti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * BPMN 设计器控制器
 * 为新的 Vue 设计器提供 API 接口
 */
@Api(value = "BPMN设计器接口")
@RestController
@RequestMapping("/bpmn/designer")
public class BpmnDesignerController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(BpmnDesignerController.class);

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 获取模型 XML
     * 兼容旧的 JSON 格式，自动转换为 BPMN XML
     *
     * @param modelId 模型ID
     * @return BPMN XML
     */
    @ApiOperation("获取模型XML")
    @GetMapping("/model/{modelId}/xml")
    public AjaxResult getModelXml(@PathVariable String modelId) {
        try {
            Model model = repositoryService.getModel(modelId);
            if (model == null) {
                return AjaxResult.error("模型不存在");
            }

            byte[] modelData = repositoryService.getModelEditorSource(modelId);
            if (modelData == null || modelData.length == 0) {
                // 返回空的 BPMN XML 模板
                String emptyXml = createEmptyBpmnXml(model.getKey(), model.getName());
                Map<String, Object> result = new HashMap<>();
                result.put("xml", emptyXml);
                result.put("name", model.getName());
                result.put("key", model.getKey());
                return AjaxResult.success(result);
            }

            String modelContent = new String(modelData, StandardCharsets.UTF_8);

            // 判断是 JSON 格式还是 XML 格式
            String xml;
            if (modelContent.trim().startsWith("{")) {
                // 旧的 JSON 格式，需要转换为 XML
                JsonNode jsonNode = objectMapper.readTree(modelData);
                BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(jsonNode);
                BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
                xml = new String(xmlConverter.convertToXML(bpmnModel, "UTF-8"), StandardCharsets.UTF_8);
                logger.info("Converted model {} from JSON to XML", modelId);
            } else {
                // 已经是 XML 格式
                xml = modelContent;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("xml", xml);
            result.put("name", model.getName());
            result.put("key", model.getKey());
            return AjaxResult.success(result);

        } catch (Exception e) {
            logger.error("Failed to get model XML: {}", modelId, e);
            return AjaxResult.error("获取模型失败: " + e.getMessage());
        }
    }

    /**
     * 保存模型
     * 直接保存 BPMN XML 格式
     *
     * @param modelId 模型ID
     * @param data    包含 xml 和 svg 的数据
     * @return 保存结果
     */
    @ApiOperation("保存模型")
    @PutMapping("/model/{modelId}/save")
    public AjaxResult saveModel(@PathVariable String modelId, @RequestBody Map<String, String> data) {
        try {
            Model model = repositoryService.getModel(modelId);
            if (model == null) {
                return AjaxResult.error("模型不存在");
            }

            String xml = data.get("xml");
            String svg = data.get("svg");

            if (xml == null || xml.isEmpty()) {
                return AjaxResult.error("XML 内容不能为空");
            }

            // 先更新模型版本并保存（避免乐观锁冲突）
            Integer version = model.getVersion();
            if (version == null) {
                version = 1;
            } else {
                version++;
            }
            model.setVersion(version);
            model.setDeploymentId(null); // 清除部署ID，表示模型已修改
            repositoryService.saveModel(model);

            // 然后保存 XML 内容
            repositoryService.addModelEditorSource(modelId, xml.getBytes(StandardCharsets.UTF_8));

            // 保存 SVG（如果提供）
            if (svg != null && !svg.isEmpty()) {
                repositoryService.addModelEditorSourceExtra(modelId, svg.getBytes(StandardCharsets.UTF_8));
            }

            logger.info("Model {} saved successfully with version {}", modelId, version);
            return AjaxResult.success("保存成功");

        } catch (Exception e) {
            logger.error("Failed to save model: {}", modelId, e);
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 验证模型
     *
     * @param modelId 模型ID
     * @param data    包含 xml 的数据
     * @return 验证结果
     */
    @ApiOperation("验证模型")
    @PostMapping("/model/{modelId}/validate")
    public AjaxResult validateModel(@PathVariable String modelId, @RequestBody Map<String, String> data) {
        try {
            String xml = data.get("xml");
            if (xml == null || xml.isEmpty()) {
                return AjaxResult.error("XML 内容不能为空");
            }

            // 尝试解析 XML 验证格式
            BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(
                    xml.getBytes(StandardCharsets.UTF_8));
            javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
            javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            BpmnModel bpmnModel = xmlConverter.convertToBpmnModel(reader);

            // 检查是否有解析错误
            if (bpmnModel.getErrors() != null && !bpmnModel.getErrors().isEmpty()) {
                return AjaxResult.error("模型验证失败: " + bpmnModel.getErrors().values().toString());
            }

            return AjaxResult.success("模型验证通过");

        } catch (Exception e) {
            logger.error("Failed to validate model: {}", modelId, e);
            return AjaxResult.error("模型验证失败: " + e.getMessage());
        }
    }

    /**
     * 创建空的 BPMN XML 模板
     */
    private String createEmptyBpmnXml(String processKey, String processName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "  xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                "  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n" +
                "  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n" +
                "  xmlns:activiti=\"http://activiti.org/bpmn\"\n" +
                "  id=\"Definitions_1\"\n" +
                "  targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"" + processKey + "\" name=\"" + processName + "\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"开始\"/>\n" +
                "  </bpmn:process>\n" +
                "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n" +
                "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"" + processKey + "\">\n" +
                "      <bpmndi:BPMNShape id=\"StartEvent_1_di\" bpmnElement=\"StartEvent_1\">\n" +
                "        <dc:Bounds x=\"180\" y=\"160\" width=\"36\" height=\"36\"/>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "    </bpmndi:BPMNPlane>\n" +
                "  </bpmndi:BPMNDiagram>\n" +
                "</bpmn:definitions>";
    }
}
