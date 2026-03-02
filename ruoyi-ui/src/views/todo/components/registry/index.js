/**
 * 表单组件注册中心
 * 新增流程只需在此注册组件，无需修改 processTask.vue
 */

// 组件注册表
const formRegistry = {
  leaveapply: () => import('../forms/leaveApplyForm.vue'),
  meeting: () => import('../forms/meetingForm.vue'),
  purchase: () => import('../forms/purchaseForm.vue')
}

/**
 * 获取表单组件加载器
 * @param {string} taskType - 流程类型（如 leaveapply, meeting, purchase）
 * @returns {Function|null} 组件加载函数
 */
export function getFormComponent(taskType) {
  const loader = formRegistry[taskType]
  if (!loader) {
    console.warn(`未注册的流程类型: ${taskType}`)
    return null
  }
  return loader
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
 * 检查流程类型是否已注册
 * @param {string} taskType - 流程类型
 * @returns {boolean}
 */
export function hasForm(taskType) {
  return !!formRegistry[taskType]
}

/**
 * 获取所有已注册的流程类型
 * @returns {string[]}
 */
export function getRegisteredTypes() {
  return Object.keys(formRegistry)
}
