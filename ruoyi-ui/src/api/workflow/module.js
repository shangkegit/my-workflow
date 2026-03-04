import request from '@/utils/request'

// 获取模块列表
export function listModules() {
  return request({
    url: '/workflow/module/manage/list',
    method: 'get'
  })
}

// 获取模块详情
export function getModuleDetail(processKey) {
  return request({
    url: '/workflow/module/manage/detail/' + processKey,
    method: 'get'
  })
}

// 导入模块
export function importModule(jsonConfig) {
  return request({
    url: '/workflow/module/manage/import',
    method: 'post',
    data: jsonConfig,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

// 导出模块
export function exportModule(processKey) {
  return request({
    url: '/workflow/module/manage/export/' + processKey,
    method: 'get'
  })
}

// 暂停模块
export function suspendModule(processKey) {
  return request({
    url: '/workflow/module/manage/suspend/' + processKey,
    method: 'post'
  })
}

// 激活模块
export function activateModule(processKey) {
  return request({
    url: '/workflow/module/manage/activate/' + processKey,
    method: 'post'
  })
}

// 删除模块
export function removeModule(processKey) {
  return request({
    url: '/workflow/module/manage/remove/' + processKey,
    method: 'post'
  })
}

// 重新加载模块
export function reloadModule(processKey) {
  return request({
    url: '/workflow/module/manage/reload/' + processKey,
    method: 'post'
  })
}

// 获取任务表单
export function getTaskForm(taskId) {
  return request({
    url: '/workflow/module/task/form/' + taskId,
    method: 'get'
  })
}

// 完成任务
export function completeTask(taskId, variables) {
  return request({
    url: '/workflow/module/task/complete/' + taskId,
    method: 'post',
    data: variables
  })
}
