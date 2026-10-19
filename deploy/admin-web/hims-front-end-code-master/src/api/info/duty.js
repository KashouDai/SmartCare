import request from '@/utils/request'

// 查询排班信息列表
export function listDuty(query) {
  return request({
    url: '/info/duty/list',
    method: 'get',
    params: query
  })
}

// 查询排班信息详细
export function getDuty(scheduleId) {
  return request({
    url: '/info/duty/' + scheduleId,
    method: 'get'
  })
}

// 新增排班信息
export function addDuty(data) {
  return request({
    url: '/info/duty',
    method: 'post',
    data: data
  })
}

// 修改排班信息
export function updateDuty(data) {
  return request({
    url: '/info/duty',
    method: 'put',
    data: data
  })
}

// 删除排班信息
export function delDuty(scheduleId) {
  return request({
    url: '/info/duty/' + scheduleId,
    method: 'delete'
  })
}
