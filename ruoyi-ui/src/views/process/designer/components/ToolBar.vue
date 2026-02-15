<template>
  <div class="toolbar">
    <div class="toolbar-left">
      <el-button-group>
        <el-tooltip content="撤销 (Ctrl+Z)" placement="bottom">
          <el-button size="small" icon="el-icon-refresh-left" @click="handleUndo" :disabled="!canUndo"></el-button>
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)" placement="bottom">
          <el-button size="small" icon="el-icon-refresh-right" @click="handleRedo" :disabled="!canRedo"></el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical"></el-divider>

      <el-button-group>
        <el-tooltip content="放大" placement="bottom">
          <el-button size="small" icon="el-icon-zoom-in" @click="handleZoomIn"></el-button>
        </el-tooltip>
        <el-tooltip content="缩小" placement="bottom">
          <el-button size="small" icon="el-icon-zoom-out" @click="handleZoomOut"></el-button>
        </el-tooltip>
        <el-tooltip content="适应画布" placement="bottom">
          <el-button size="small" icon="el-icon-full-screen" @click="handleZoomFit"></el-button>
        </el-tooltip>
        <el-tooltip content="重置缩放" placement="bottom">
          <el-button size="small" @click="handleZoomReset">100%</el-button>
        </el-tooltip>
      </el-button-group>
    </div>

    <div class="toolbar-right">
      <el-button-group>
        <el-tooltip content="导入 BPMN/XML 文件" placement="bottom">
          <el-button size="small" icon="el-icon-upload2" @click="handleImport">导入</el-button>
        </el-tooltip>
        <el-tooltip content="下载 BPMN" placement="bottom">
          <el-button size="small" icon="el-icon-download" @click="handleDownloadBpmn">BPMN</el-button>
        </el-tooltip>
        <el-tooltip content="下载 SVG" placement="bottom">
          <el-button size="small" icon="el-icon-picture" @click="handleDownloadSvg">SVG</el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical"></el-divider>

      <el-button type="primary" size="small" icon="el-icon-check" @click="handleSave" :loading="saving">
        保存
      </el-button>
    </div>

    <!-- 隐藏的文件输入框 -->
    <input
      ref="fileInput"
      type="file"
      accept=".bpmn,.xml"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script>
import { getModeler, saveXML, saveSVG } from '../hooks/useBpmnModeler'

export default {
  name: 'ToolBar',
  props: {
    saving: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      canUndo: false,
      canRedo: false,
      commandStackReady: false
    }
  },
  mounted() {
    this.waitForModeler()
  },
  methods: {
    waitForModeler() {
      // 等待 modeler 初始化完成
      const checkModeler = () => {
        const modeler = getModeler()
        if (modeler) {
          this.setupCommandStack()
        } else {
          setTimeout(checkModeler, 100)
        }
      }
      checkModeler()
    },
    setupCommandStack() {
      const modeler = getModeler()
      if (!modeler) return

      const commandStack = modeler.get('commandStack')
      this.canUndo = commandStack.canUndo()
      this.canRedo = commandStack.canRedo()
      this.commandStackReady = true

      modeler.on('commandStack.changed', () => {
        this.canUndo = commandStack.canUndo()
        this.canRedo = commandStack.canRedo()
      })
    },
    handleUndo() {
      const modeler = getModeler()
      if (modeler) {
        modeler.get('commandStack').undo()
      }
    },
    handleRedo() {
      const modeler = getModeler()
      if (modeler) {
        modeler.get('commandStack').redo()
      }
    },
    handleZoomIn() {
      const modeler = getModeler()
      if (modeler) {
        const canvas = modeler.get('canvas')
        canvas.zoom(canvas.zoom() * 1.1)
      }
    },
    handleZoomOut() {
      const modeler = getModeler()
      if (modeler) {
        const canvas = modeler.get('canvas')
        canvas.zoom(canvas.zoom() * 0.9)
      }
    },
    handleZoomFit() {
      const modeler = getModeler()
      if (modeler) {
        const canvas = modeler.get('canvas')
        canvas.zoom('fit-viewport')
      }
    },
    handleZoomReset() {
      const modeler = getModeler()
      if (modeler) {
        const canvas = modeler.get('canvas')
        canvas.zoom(1)
      }
    },
    handleImport() {
      this.$refs.fileInput.click()
    },
    handleFileChange(event) {
      const file = event.target.files[0]
      if (!file) return

      const reader = new FileReader()
      reader.onload = (e) => {
        const xml = e.target.result
        this.$emit('import', xml, file.name)
      }
      reader.onerror = () => {
        this.$message.error('读取文件失败')
      }
      reader.readAsText(file)

      // 清空 input，允许重复导入同一文件
      event.target.value = ''
    },
    async handleDownloadBpmn() {
      try {
        const xml = await saveXML()
        const blob = new Blob([xml], { type: 'application/xml' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = 'diagram.bpmn'
        a.click()
        URL.revokeObjectURL(url)
      } catch (error) {
        this.$message.error('导出 BPMN 失败: ' + error.message)
      }
    },
    async handleDownloadSvg() {
      try {
        const svg = await saveSVG()
        const blob = new Blob([svg], { type: 'image/svg+xml' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = 'diagram.svg'
        a.click()
        URL.revokeObjectURL(url)
      } catch (error) {
        this.$message.error('导出 SVG 失败: ' + error.message)
      }
    },
    handleSave() {
      this.$emit('save')
    }
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-divider--vertical {
  height: 20px;
  margin: 0 8px;
}
</style>
