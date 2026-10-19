import request from '@/utils/request'

// 查询药品基础信息列表
export function listDrugInfo(query) {
  return request({
    url: '/info/drugInfo/list',
    method: 'get',
    params: query
  })
}

// 查询药品基础信息详细
export function getDrugInfo(id) {
  return request({
    url: '/info/drugInfo/' + id,
    method: 'get'
  })
}

// 新增药品基础信息
export function addDrugInfo(data) {
  return request({
    url: '/info/drugInfo',
    method: 'post',
    data: data
  })
}

// 修改药品基础信息
export function updateDrugInfo(data) {
  return request({
    url: '/info/drugInfo',
    method: 'put',
    data: data
  })
}

// 删除药品基础信息
export function delDrugInfo(id) {
  return request({
    url: '/info/drugInfo/' + id,
    method: 'delete'
  })
}
