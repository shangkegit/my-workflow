import request from '@/utils/request'

// 获取插件列表
export function listPlugins() {
  return request({
    url: '/plugin/list',
    method: 'get'
  })
}

// 获取插件详情
export function getPlugin(pluginId) {
  return request({
    url: '/plugin/' + pluginId,
    method: 'get'
  })
}

// 导入插件
export function importPlugin(data) {
  return request({
    url: '/plugin/import',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: data
  })
}

// 卸载插件
export function uninstallPlugin(pluginId) {
  return request({
    url: '/plugin/' + pluginId,
    method: 'delete'
  })
}
