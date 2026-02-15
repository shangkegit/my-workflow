<template>
  <div class="designer-container">
    <!-- 工具栏 -->
    <tool-bar @save="handleSave" @import="handleFileImport" :saving="saving" />

    <!-- 主体内容 -->
    <div class="designer-main">
      <!-- 画布区域 -->
      <div class="canvas-wrapper">
        <bpmn-canvas
          ref="canvas"
          :xml="bpmnXml"
          @selection-changed="handleSelectionChanged"
          @import-success="handleImportSuccess"
          @import-error="handleImportError"
        />
      </div>

      <!-- 属性面板 -->
      <property-panel :selected-element="selectedElement" :root-element="rootElement" />
    </div>
  </div>
</template>

<script>
import BpmnCanvas from './components/BpmnCanvas.vue'
import PropertyPanel from './components/PropertyPanel.vue'
import ToolBar from './components/ToolBar.vue'
import { getModelXml, saveModel } from './api/designer.js'
import { saveXML, saveSVG, importXML, getModeler } from './hooks/useBpmnModeler'

export default {
  name: 'ProcessDesigner',
  components: {
    BpmnCanvas,
    PropertyPanel,
    ToolBar
  },
  data() {
    return {
      modelId: '',
      bpmnXml: '',
      selectedElement: null,
      rootElement: null,
      saving: false,
      loading: false
    }
  },
  created() {
    this.modelId = this.$route.params.modelId
    if (this.modelId) {
      this.loadModel()
    }
  },
  methods: {
    async loadModel() {
      this.loading = true
      try {
        const response = await getModelXml(this.modelId)
        if (response.code === 200) {
          this.bpmnXml = response.data.xml
        } else {
          this.$message.error(response.msg || '加载模型失败')
        }
      } catch (error) {
        console.error('Load model error:', error)
        this.$message.error('加载模型失败: ' + (error.message || '未知错误'))
      } finally {
        this.loading = false
      }
    },
    handleSelectionChanged(selection) {
      if (selection && selection.length === 1) {
        this.selectedElement = selection[0]
      } else {
        this.selectedElement = null
        // 没有选中元素时，获取流程根元素
        this.updateRootElement()
      }
    },
    updateRootElement() {
      const modeler = getModeler()
      if (modeler) {
        const canvas = modeler.get('canvas')
        const rootElement = canvas.getRootElement()
        this.rootElement = rootElement
      }
    },
    handleImportSuccess() {
      this.$message.success('模型加载成功')
      this.updateRootElement()
    },
    handleImportError(error) {
      this.$message.error('模型解析失败: ' + (error.message || '未知错误'))
    },
    async handleFileImport(xml, fileName) {
      try {
        await importXML(xml)
        this.$message.success(`文件 ${fileName} 导入成功`)
        this.updateRootElement()
      } catch (error) {
        console.error('Import file error:', error)
        this.$message.error('导入失败: ' + (error.message || '文件格式不正确'))
      }
    },
    async handleSave() {
      this.saving = true
      try {
        const xml = await saveXML()
        const svg = await saveSVG()

        const response = await saveModel(this.modelId, {
          xml: xml,
          svg: svg
        })

        if (response.code === 200) {
          this.$message.success('保存成功')
        } else {
          this.$message.error(response.msg || '保存失败')
        }
      } catch (error) {
        console.error('Save model error:', error)
        this.$message.error('保存失败: ' + (error.message || '未知错误'))
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.designer-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f7f7f7;
}

.designer-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.canvas-wrapper {
  flex: 1;
  overflow: hidden;
}
</style>
