import request from '@/utils/request'

// 查询预约信息列表
export function listAppointments(query) {
  return request({
    url: '/info/appointments/list',
    method: 'get',
    params: query
  })
}

// 查询预约信息详细
export function getAppointments(appointmentsId) {
  return request({
    url: '/info/appointments/' + appointmentsId,
    method: 'get'
  })
}

// 新增预约信息
export function addAppointments(data) {
  return request({
    url: '/info/appointments',
    method: 'post',
    data: data
  })
}

// 修改预约信息
export function updateAppointments(data) {
  return request({
    url: '/info/appointments',
    method: 'put',
    data: data
  })
}

// 删除预约信息
export function delAppointments(appointmentsId) {
  return request({
    url: '/info/appointments/' + appointmentsId,
    method: 'delete'
  })
}
