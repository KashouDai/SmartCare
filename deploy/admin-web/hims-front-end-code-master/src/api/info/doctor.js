import request from '@/utils/request'

// 查询医生信息管理列表
export function listDoctor(query) {
  return request({
    url: '/info/doctor/list',
    method: 'get',
    params: query
  })
}

// 查询医生信息管理详细
export function getDoctor(userId) {
  return request({
    url: '/info/doctor/' + userId,
    method: 'get'
  })
}

// 新增医生信息管理
export function addDoctor(data) {
  return request({
    url: '/info/doctor',
    method: 'post',
    data: data
  })
}

// 修改医生信息管理
export function updateDoctor(data) {
  return request({
    url: '/info/doctor',
    method: 'put',
    data: data
  })
}

// 删除医生信息管理
export function delDoctor(userId) {
  return request({
    url: '/info/doctor/' + userId,
    method: 'delete'
  })
}

// 查询部门下拉树结构
export function deptTreeSelect() {
  return request({
    url: '/system/user/deptTree',
    method: 'get'
  })
}
