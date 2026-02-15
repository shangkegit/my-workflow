import BpmnModeler from 'bpmn-js/lib/Modeler'
import activitiModdle from '../config/activitiModdle.json'

let modelerInstance = null

export function createModeler(container) {
  if (modelerInstance) {
    modelerInstance.destroy()
  }

  modelerInstance = new BpmnModeler({
    container: container,
    additionalModules: [],
    moddleExtensions: {
      activiti: activitiModdle
    },
    keyboard: {
      bindTo: window
    }
  })

  return modelerInstance
}

export function getModeler() {
  return modelerInstance
}

export function destroyModeler() {
  if (modelerInstance) {
    modelerInstance.destroy()
    modelerInstance = null
  }
}

export async function importXML(xml) {
  const modeler = getModeler()
  if (!modeler) {
    throw new Error('Modeler not initialized')
  }

  try {
    const result = await modeler.importXML(xml)
    const canvas = modeler.get('canvas')
    canvas.zoom('fit-viewport')
    return result
  } catch (error) {
    console.error('Import XML error:', error)
    throw error
  }
}

export async function saveXML() {
  const modeler = getModeler()
  if (!modeler) {
    throw new Error('Modeler not initialized')
  }

  try {
    const result = await modeler.saveXML({ format: true })
    return result.xml
  } catch (error) {
    console.error('Save XML error:', error)
    throw error
  }
}

export async function saveSVG() {
  const modeler = getModeler()
  if (!modeler) {
    throw new Error('Modeler not initialized')
  }

  try {
    const result = await modeler.saveSVG()
    return result.svg
  } catch (error) {
    console.error('Save SVG error:', error)
    throw error
  }
}

export function getSelection() {
  const modeler = getModeler()
  if (!modeler) return []

  const selection = modeler.get('selection')
  return selection.get()
}

export function getElementRegistry() {
  const modeler = getModeler()
  if (!modeler) return null

  return modeler.get('elementRegistry')
}

export function getModdle() {
  const modeler = getModeler()
  if (!modeler) return null

  return modeler.get('moddle')
}

export function getModeling() {
  const modeler = getModeler()
  if (!modeler) return null

  return modeler.get('modeling')
}

export default {
  createModeler,
  getModeler,
  destroyModeler,
  importXML,
  saveXML,
  saveSVG,
  getSelection,
  getElementRegistry,
  getModdle,
  getModeling
}
