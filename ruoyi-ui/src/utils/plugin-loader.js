/**
 * 插件组件动态加载器
 */

// 已加载的插件组件缓存
const loadedPlugins = new Map()

// 加载状态追踪
const loadingPlugins = new Map()

/**
 * 动态加载插件前端组件
 * @param {string} processType - 流程类型
 * @param {string} componentUrl - 组件 JS 文件 URL
 * @returns {Promise<Object>} Vue 组件对象
 */
export async function loadPluginComponent(processType, componentUrl) {
  // 检查缓存
  if (loadedPlugins.has(processType)) {
    return loadedPlugins.get(processType)
  }

  // 检查是否正在加载
  if (loadingPlugins.has(processType)) {
    return loadingPlugins.get(processType)
  }

  // 创建加载 Promise
  const loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = componentUrl
    script.async = true

    script.onload = () => {
      // 约定：插件组件挂载到 window.__PLUGINS__[processType]
      const component = window.__PLUGINS__?.[processType]
      if (component) {
        loadedPlugins.set(processType, component)
        loadingPlugins.delete(processType)
        resolve(component)
      } else {
        loadingPlugins.delete(processType)
        reject(new Error(`插件组件未正确注册: ${processType}`))
      }
    }

    script.onerror = () => {
      loadingPlugins.delete(processType)
      reject(new Error(`加载插件失败: ${componentUrl}`))
    }

    document.head.appendChild(script)
  })

  loadingPlugins.set(processType, loadPromise)
  return loadPromise
}

/**
 * 检查插件是否已加载
 */
export function isPluginLoaded(processType) {
  return loadedPlugins.has(processType)
}

/**
 * 清除插件缓存
 */
export function clearPluginCache(processType) {
  loadedPlugins.delete(processType)
  if (window.__PLUGINS__) {
    delete window.__PLUGINS__[processType]
  }
}
