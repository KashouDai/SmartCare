import request from '@/utils/request'

// 查询患者信息列表
export function listInfo(query) {
  return request({
    url: '/info/patientsInfo/list',
    method: 'get',
    params: query
  })
}

// 查询患者信息详细
export function getInfo(patientId) {
  return request({
    url: '/info/patientsInfo/' + patientId,
    method: 'get'
  })
}

// 新增患者信息
export function addInfo(data) {
  return request({
    url: '/info/patientsInfo',
    method: 'post',
    data: data
  })
}

// 修改患者信息
export function updateInfo(data) {
  return request({
    url: '/info/patientsInfo',
    method: 'put',
    data: data
  })
}

// 删除患者信息
export function delInfo(patientId) {
  return request({
    url: '/info/patientsInfo/' + patientId,
    method: 'delete'
  })
}

export function getHistoryInfo(query) {
  return request({
    url: '/info/history/list',
    method: 'get',
    params: query
  })
}
