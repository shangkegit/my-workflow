/**
 * 表单组件注册中心
 * 新增流程只需在此注册组件，无需修改 processTask.vue
 * 支持内置组件和动态插件组件
 */

import { loadPluginComponent, isPluginLoaded } from '@/utils/plugin-loader'

// 内置组件注册表
const formRegistry = {
  leaveapply: () => import('../forms/leaveApplyForm.vue'),
  meeting: () => import('../forms/meetingForm.vue'),
  purchase: () => import('../forms/purchaseForm.vue')
}

// 插件 URL 映射表（流程类型 -> 插件组件 URL）
const pluginUrls = new Map()

/**
 * 获取表单组件加载器
 * @param {string} taskType - 流程类型（如 leaveapply, meeting, purchase）
 * @returns {Function|null} 组件加载函数
 */
export function getFormComponent(taskType) {
  // 1. 优先检查内置组件
  const loader = formRegistry[taskType]
  if (loader) {
    return loader
  }

  // 2. 检查是否为插件流程
  const pluginUrl = pluginUrls.get(taskType)
  if (pluginUrl) {
    // 返回异步加载器
    return async () => {
      // 如果插件已加载，直接返回
      if (isPluginLoaded(taskType)) {
        return { default: await loadPluginComponent(taskType, pluginUrl) }
      }
      // 动态加载插件
      const component = await loadPluginComponent(taskType, pluginUrl)
      return { default: component }
    }
  }

  console.warn(`未注册的流程类型: ${taskType}`)
  return null
}

/**
 * 注册新的表单组件
 * @param {string} taskType - 流程类型
 * @param {Function} componentLoader - 组件加载函数 () => import('./xxxForm.vue')
 */
export function registerForm(taskType, componentLoader) {
  formRegistry[taskType] = componentLoader
}

/**
 * 注册插件组件 URL
 * @param {string} processType - 流程类型
 * @param {string} pluginUrl - 插件组件 JS 文件 URL
 */
export function registerPluginUrl(processType, pluginUrl) {
  pluginUrls.set(processType, pluginUrl)
}

/**
 * 检查流程类型是否已注册
 * @param {string} taskType - 流程类型
 * @returns {boolean}
 */
export function hasForm(taskType) {
  return !!formRegistry[taskType] || pluginUrls.has(taskType)
}

/**
 * 获取所有已注册的流程类型
 * @returns {string[]}
 */
export function getRegisteredTypes() {
  const builtIn = Object.keys(formRegistry)
  const plugins = Array.from(pluginUrls.keys())
  return [...builtIn, ...plugins]
}
