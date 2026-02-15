<template>
  <div class="bpmn-canvas" ref="canvasContainer"></div>
</template>

<script>
import { createModeler, destroyModeler, importXML, getModeler } from '../hooks/useBpmnModeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

export default {
  name: 'BpmnCanvas',
  props: {
    xml: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      modeler: null
    }
  },
  watch: {
    xml(newVal) {
      if (newVal && this.modeler) {
        this.loadXML(newVal)
      }
    }
  },
  mounted() {
    this.initModeler()
  },
  beforeDestroy() {
    destroyModeler()
  },
  methods: {
    initModeler() {
      this.modeler = createModeler(this.$refs.canvasContainer)

      // 监听选择变化
      this.modeler.on('selection.changed', (e) => {
        this.$emit('selection-changed', e.newSelection)
      })

      // 监听元素变化
      this.modeler.on('element.changed', (e) => {
        this.$emit('element-changed', e.element)
      })

      // 如果有初始 XML，则加载
      if (this.xml) {
        this.loadXML(this.xml)
      }
    },
    async loadXML(xml) {
      try {
        await importXML(xml)
        this.$emit('import-success')
      } catch (error) {
        console.error('Failed to import XML:', error)
        this.$emit('import-error', error)
      }
    },
    getModeler() {
      return getModeler()
    }
  }
}
</script>

<style scoped>
.bpmn-canvas {
  width: 100%;
  height: 100%;
  background-color: #f7f7f7;
}
</style>
