import request from '@/utils/request'

/**
 * 获取模型 XML
 * @param {string} modelId 模型ID
 * @returns Promise
 */
export function getModelXml(modelId) {
  return request({
    url: `/bpmn/designer/model/${modelId}/xml`,
    method: 'get'
  })
}

/**
 * 保存模型
 * @param {string} modelId 模型ID
 * @param {object} data 包含 xml 和 svg 的数据对象
 * @returns Promise
 */
export function saveModel(modelId, data) {
  return request({
    url: `/bpmn/designer/model/${modelId}/save`,
    method: 'put',
    data: data
  })
}

/**
 * 验证模型
 * @param {string} modelId 模型ID
 * @param {string} xml BPMN XML
 * @returns Promise
 */
export function validateModel(modelId, xml) {
  return request({
    url: `/bpmn/designer/model/${modelId}/validate`,
    method: 'post',
    data: { xml }
  })
}
