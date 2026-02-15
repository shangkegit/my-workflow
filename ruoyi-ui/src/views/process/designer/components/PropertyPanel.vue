<template>
  <div class="property-panel">
    <!-- 选中元素时显示元素属性 -->
    <div v-if="selectedElement" class="property-content">
      <div class="property-header">
        <span class="element-type">{{ elementTypeLabel }}</span>
        <span class="element-id">{{ selectedElement.id }}</span>
      </div>

      <el-form label-width="80px" size="small">
        <!-- 基本信息 -->
        <el-divider content-position="left">基本信息</el-divider>

        <el-form-item label="ID">
          <el-input v-model="elementId" @change="updateElementId" />
        </el-form-item>

        <el-form-item label="名称">
          <el-input v-model="elementName" @change="updateElementName" />
        </el-form-item>

        <!-- 用户任务特有属性 -->
        <template v-if="isUserTask">
          <el-divider content-position="left">任务配置</el-divider>

          <el-form-item label="办理人">
            <el-input v-model="assignee" @change="updateAssignee" placeholder="${用户ID}" />
          </el-form-item>

          <el-form-item label="候选用户">
            <el-input v-model="candidateUsers" @change="updateCandidateUsers" placeholder="用户1,用户2" />
          </el-form-item>

          <el-form-item label="候选组">
            <el-input v-model="candidateGroups" @change="updateCandidateGroups" placeholder="组1,组2" />
          </el-form-item>

          <el-form-item label="表单Key">
            <el-input v-model="formKey" @change="updateFormKey" placeholder="/path/to/form" />
          </el-form-item>

          <el-form-item label="优先级">
            <el-input v-model="priority" @change="updatePriority" />
          </el-form-item>

          <el-form-item label="到期日">
            <el-input v-model="dueDate" @change="updateDueDate" placeholder="${变量} 或 ISO日期" />
          </el-form-item>

          <el-form-item label="分类">
            <el-input v-model="category" @change="updateCategory" />
          </el-form-item>
        </template>

        <!-- 服务任务特有属性 -->
        <template v-if="isServiceTask">
          <el-divider content-position="left">服务配置</el-divider>

          <el-form-item label="实现类">
            <el-input v-model="serviceClass" @change="updateServiceClass" placeholder="完整类名" />
          </el-form-item>

          <el-form-item label="表达式">
            <el-input v-model="serviceExpression" @change="updateServiceExpression" placeholder="${表达式}" />
          </el-form-item>

          <el-form-item label="代理表达式">
            <el-input v-model="serviceDelegateExpression" @change="updateServiceDelegateExpression" placeholder="${delegate}" />
          </el-form-item>

          <el-form-item label="结果变量">
            <el-input v-model="resultVariable" @change="updateResultVariable" />
          </el-form-item>
        </template>

        <!-- 序列流特有属性 -->
        <template v-if="isSequenceFlow">
          <el-divider content-position="left">流转条件</el-divider>

          <el-form-item label="条件表达式">
            <el-input
              type="textarea"
              v-model="conditionExpression"
              @change="updateConditionExpression"
              :rows="3"
              placeholder="${变量 == 值}"
            />
          </el-form-item>
        </template>

        <!-- 异步配置 -->
        <template v-if="supportsAsync">
          <el-divider content-position="left">异步配置</el-divider>

          <el-form-item label="异步前置">
            <el-switch v-model="asyncBefore" @change="updateAsyncBefore" />
          </el-form-item>

          <el-form-item label="异步后置">
            <el-switch v-model="asyncAfter" @change="updateAsyncAfter" />
          </el-form-item>

          <el-form-item label="独占" v-if="asyncBefore || asyncAfter">
            <el-switch v-model="exclusive" @change="updateExclusive" />
          </el-form-item>
        </template>

        <!-- 多实例配置 -->
        <template v-if="supportsMultiInstance">
          <el-divider content-position="left">多实例</el-divider>

          <el-form-item label="集合">
            <el-input v-model="loopCollection" @change="updateLoopCollection" placeholder="${集合变量}" />
          </el-form-item>

          <el-form-item label="元素变量">
            <el-input v-model="loopVariable" @change="updateLoopVariable" placeholder="item" />
          </el-form-item>

          <el-form-item label="完成条件">
            <el-input v-model="completionCondition" @change="updateCompletionCondition" placeholder="${nrOfCompletedInstances >= 3}" />
          </el-form-item>
        </template>
      </el-form>
    </div>

    <!-- 没有选中元素时显示流程属性 -->
    <div v-else-if="rootElement" class="property-content">
      <div class="property-header">
        <span class="element-type">流程属性</span>
        <span class="element-id">{{ processId }}</span>
      </div>

      <el-form label-width="80px" size="small">
        <el-divider content-position="left">基本信息</el-divider>

        <el-form-item label="流程ID">
          <el-input v-model="processId" @change="updateProcessId" />
        </el-form-item>

        <el-form-item label="流程名称">
          <el-input v-model="processName" @change="updateProcessName" />
        </el-form-item>

        <el-form-item label="可执行">
          <el-switch v-model="isExecutable" @change="updateIsExecutable" />
        </el-form-item>

        <el-divider content-position="left">流程配置</el-divider>

        <el-form-item label="流程分类">
          <el-input v-model="processCategory" @change="updateProcessCategory" placeholder="流程分类" />
        </el-form-item>

        <el-form-item label="发起人">
          <el-input v-model="processCandidateStarterUsers" @change="updateProcessCandidateStarterUsers" placeholder="用户1,用户2" />
        </el-form-item>

        <el-form-item label="发起组">
          <el-input v-model="processCandidateStarterGroups" @change="updateProcessCandidateStarterGroups" placeholder="组1,组2" />
        </el-form-item>
      </el-form>
    </div>

    <div v-else class="no-selection">
      <i class="el-icon-info"></i>
      <p>请选择一个元素查看属性</p>
    </div>
  </div>
</template>

<script>
import { getModeler } from '../hooks/useBpmnModeler'

export default {
  name: 'PropertyPanel',
  props: {
    selectedElement: {
      type: Object,
      default: null
    },
    rootElement: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      elementId: '',
      elementName: '',
      // 用户任务属性
      assignee: '',
      candidateUsers: '',
      candidateGroups: '',
      formKey: '',
      priority: '',
      dueDate: '',
      category: '',
      // 服务任务属性
      serviceClass: '',
      serviceExpression: '',
      serviceDelegateExpression: '',
      resultVariable: '',
      // 序列流属性
      conditionExpression: '',
      // 异步属性
      asyncBefore: false,
      asyncAfter: false,
      exclusive: true,
      // 多实例属性
      loopCollection: '',
      loopVariable: '',
      completionCondition: '',
      // 流程属性
      processId: '',
      processName: '',
      isExecutable: true,
      processCategory: '',
      processCandidateStarterUsers: '',
      processCandidateStarterGroups: ''
    }
  },
  computed: {
    elementTypeLabel() {
      if (!this.selectedElement) return ''
      const typeMap = {
        'bpmn:Task': '任务',
        'bpmn:UserTask': '用户任务',
        'bpmn:ServiceTask': '服务任务',
        'bpmn:ScriptTask': '脚本任务',
        'bpmn:SendTask': '发送任务',
        'bpmn:ReceiveTask': '接收任务',
        'bpmn:ManualTask': '手工任务',
        'bpmn:BusinessRuleTask': '业务规则任务',
        'bpmn:ExclusiveGateway': '排他网关',
        'bpmn:ParallelGateway': '并行网关',
        'bpmn:InclusiveGateway': '包容网关',
        'bpmn:EventBasedGateway': '事件网关',
        'bpmn:StartEvent': '开始事件',
        'bpmn:EndEvent': '结束事件',
        'bpmn:IntermediateCatchEvent': '中间捕获事件',
        'bpmn:IntermediateThrowEvent': '中间抛出事件',
        'bpmn:BoundaryEvent': '边界事件',
        'bpmn:SequenceFlow': '序列流',
        'bpmn:Process': '流程',
        'bpmn:SubProcess': '子流程',
        'bpmn:CallActivity': '调用活动'
      }
      return typeMap[this.selectedElement.type] || this.selectedElement.type
    },
    isUserTask() {
      return this.selectedElement?.type === 'bpmn:UserTask'
    },
    isServiceTask() {
      return this.selectedElement?.type === 'bpmn:ServiceTask'
    },
    isSequenceFlow() {
      return this.selectedElement?.type === 'bpmn:SequenceFlow'
    },
    supportsAsync() {
      const asyncTypes = [
        'bpmn:Task', 'bpmn:UserTask', 'bpmn:ServiceTask', 'bpmn:ScriptTask',
        'bpmn:SendTask', 'bpmn:ReceiveTask', 'bpmn:ManualTask',
        'bpmn:BusinessRuleTask', 'bpmn:CallActivity', 'bpmn:SubProcess'
      ]
      return asyncTypes.includes(this.selectedElement?.type)
    },
    supportsMultiInstance() {
      const multiInstanceTypes = [
        'bpmn:Task', 'bpmn:UserTask', 'bpmn:ServiceTask', 'bpmn:ScriptTask',
        'bpmn:SendTask', 'bpmn:ReceiveTask', 'bpmn:ManualTask',
        'bpmn:BusinessRuleTask', 'bpmn:CallActivity', 'bpmn:SubProcess'
      ]
      return multiInstanceTypes.includes(this.selectedElement?.type)
    }
  },
  watch: {
    selectedElement: {
      handler(element) {
        if (element) {
          this.loadElementProperties(element)
        }
      },
      immediate: true
    },
    rootElement: {
      handler(element) {
        if (element && !this.selectedElement) {
          this.loadProcessProperties(element)
        }
      },
      immediate: true
    }
  },
  methods: {
    loadElementProperties(element) {
      const bo = element.businessObject
      this.elementId = bo.id || ''
      this.elementName = bo.name || ''

      // 加载 Activiti 扩展属性
      if (bo.$attrs) {
        this.assignee = bo.$attrs['activiti:assignee'] || ''
        this.candidateUsers = bo.$attrs['activiti:candidateUsers'] || ''
        this.candidateGroups = bo.$attrs['activiti:candidateGroups'] || ''
        this.formKey = bo.$attrs['activiti:formKey'] || ''
        this.priority = bo.$attrs['activiti:priority'] || ''
        this.dueDate = bo.$attrs['activiti:dueDate'] || ''
        this.category = bo.$attrs['activiti:category'] || ''
        this.asyncBefore = bo.$attrs['activiti:asyncBefore'] === 'true'
        this.asyncAfter = bo.$attrs['activiti:asyncAfter'] === 'true'
        this.exclusive = bo.$attrs['activiti:exclusive'] !== 'false'
      }

      // 服务任务属性
      if (this.isServiceTask) {
        this.serviceClass = bo.$attrs['activiti:class'] || ''
        this.serviceExpression = bo.$attrs['activiti:expression'] || ''
        this.serviceDelegateExpression = bo.$attrs['activiti:delegateExpression'] || ''
        this.resultVariable = bo.$attrs['activiti:resultVariable'] || ''
      }

      // 序列流条件
      if (this.isSequenceFlow && bo.conditionExpression) {
        this.conditionExpression = bo.conditionExpression.body || ''
      }

      // 多实例配置
      if (bo.loopCharacteristics) {
        this.loopCollection = bo.loopCharacteristics.$attrs['activiti:collection'] || ''
        this.loopVariable = bo.loopCharacteristics.$attrs['activiti:elementVariable'] || ''
        if (bo.loopCharacteristics.completionCondition) {
          this.completionCondition = bo.loopCharacteristics.completionCondition.body || ''
        }
      }
    },
    loadProcessProperties(element) {
      const bo = element.businessObject
      this.processId = bo.id || ''
      this.processName = bo.name || ''
      this.isExecutable = bo.isExecutable !== false

      if (bo.$attrs) {
        this.processCategory = bo.$attrs['activiti:category'] || ''
        this.processCandidateStarterUsers = bo.$attrs['activiti:candidateStarterUsers'] || ''
        this.processCandidateStarterGroups = bo.$attrs['activiti:candidateStarterGroups'] || ''
      }
    },
    getModeling() {
      return getModeler()?.get('modeling')
    },
    getModdle() {
      return getModeler()?.get('moddle')
    },
    updateElementId() {
      const modeling = this.getModeling()
      if (modeling && this.selectedElement) {
        modeling.updateProperties(this.selectedElement, { id: this.elementId })
      }
    },
    updateElementName() {
      const modeling = this.getModeling()
      if (modeling && this.selectedElement) {
        modeling.updateProperties(this.selectedElement, { name: this.elementName })
      }
    },
    // 流程属性更新方法
    updateProcessId() {
      const modeling = this.getModeling()
      if (modeling && this.rootElement) {
        modeling.updateProperties(this.rootElement, { id: this.processId })
      }
    },
    updateProcessName() {
      const modeling = this.getModeling()
      if (modeling && this.rootElement) {
        modeling.updateProperties(this.rootElement, { name: this.processName })
      }
    },
    updateIsExecutable() {
      const modeling = this.getModeling()
      if (modeling && this.rootElement) {
        modeling.updateProperties(this.rootElement, { isExecutable: this.isExecutable })
      }
    },
    updateProcessProperty(property, value) {
      const modeling = this.getModeling()
      if (modeling && this.rootElement) {
        modeling.updateModdleProperties(this.rootElement, this.rootElement.businessObject, {
          [`activiti:${property}`]: value
        })
      }
    },
    updateProcessCategory() {
      this.updateProcessProperty('category', this.processCategory)
    },
    updateProcessCandidateStarterUsers() {
      this.updateProcessProperty('candidateStarterUsers', this.processCandidateStarterUsers)
    },
    updateProcessCandidateStarterGroups() {
      this.updateProcessProperty('candidateStarterGroups', this.processCandidateStarterGroups)
    },
    updateActivitiProperty(property, value) {
      const modeling = this.getModeling()
      if (modeling && this.selectedElement) {
        modeling.updateModdleProperties(this.selectedElement, this.selectedElement.businessObject, {
          [`activiti:${property}`]: value
        })
      }
    },
    updateAssignee() {
      this.updateActivitiProperty('assignee', this.assignee)
    },
    updateCandidateUsers() {
      this.updateActivitiProperty('candidateUsers', this.candidateUsers)
    },
    updateCandidateGroups() {
      this.updateActivitiProperty('candidateGroups', this.candidateGroups)
    },
    updateFormKey() {
      this.updateActivitiProperty('formKey', this.formKey)
    },
    updatePriority() {
      this.updateActivitiProperty('priority', this.priority)
    },
    updateDueDate() {
      this.updateActivitiProperty('dueDate', this.dueDate)
    },
    updateCategory() {
      this.updateActivitiProperty('category', this.category)
    },
    updateServiceClass() {
      this.updateActivitiProperty('class', this.serviceClass)
    },
    updateServiceExpression() {
      this.updateActivitiProperty('expression', this.serviceExpression)
    },
    updateServiceDelegateExpression() {
      this.updateActivitiProperty('delegateExpression', this.serviceDelegateExpression)
    },
    updateResultVariable() {
      this.updateActivitiProperty('resultVariable', this.resultVariable)
    },
    updateConditionExpression() {
      const modeling = this.getModeling()
      const moddle = this.getModdle()
      if (modeling && moddle && this.selectedElement) {
        const formalExpression = moddle.create('bpmn:FormalExpression', {
          body: this.conditionExpression
        })
        modeling.updateProperties(this.selectedElement, {
          conditionExpression: formalExpression
        })
      }
    },
    updateAsyncBefore() {
      this.updateActivitiProperty('asyncBefore', this.asyncBefore)
    },
    updateAsyncAfter() {
      this.updateActivitiProperty('asyncAfter', this.asyncAfter)
    },
    updateExclusive() {
      this.updateActivitiProperty('exclusive', this.exclusive)
    },
    updateLoopCollection() {
      this.updateLoopProperty('collection', this.loopCollection)
    },
    updateLoopVariable() {
      this.updateLoopProperty('elementVariable', this.loopVariable)
    },
    updateCompletionCondition() {
      const modeling = this.getModeling()
      const moddle = this.getModdle()
      if (modeling && moddle && this.selectedElement?.businessObject?.loopCharacteristics) {
        const formalExpression = moddle.create('bpmn:FormalExpression', {
          body: this.completionCondition
        })
        modeling.updateModdleProperties(
          this.selectedElement,
          this.selectedElement.businessObject.loopCharacteristics,
          { completionCondition: formalExpression }
        )
      }
    },
    updateLoopProperty(property, value) {
      const modeling = this.getModeling()
      if (modeling && this.selectedElement?.businessObject?.loopCharacteristics) {
        modeling.updateModdleProperties(
          this.selectedElement,
          this.selectedElement.businessObject.loopCharacteristics,
          { [`activiti:${property}`]: value }
        )
      }
    }
  }
}
</script>

<style scoped>
.property-panel {
  width: 300px;
  height: 100%;
  background-color: #fff;
  border-left: 1px solid #dcdfe6;
  overflow-y: auto;
}

.property-header {
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

.element-type {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.element-id {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.property-content {
  padding: 0 12px 12px;
}

.no-selection {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
}

.no-selection i {
  font-size: 32px;
  margin-bottom: 12px;
}

.el-divider--horizontal {
  margin: 16px 0 12px;
}

.el-form-item {
  margin-bottom: 12px;
}

.el-form-item__label {
  font-size: 12px;
}
</style>
