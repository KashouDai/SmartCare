import request from '@/utils/request'

// 查询医院管理列表
export function listHospital(query) {
  return request({
    url: '/info/hospital/list',
    method: 'get',
    params: query
  })
}

// 查询医院下拉框列表
export function getHospitalList() {
  return request({
    url: '/info/hospital/getList',
    method: 'get'
  })
}

// 查询医院管理详细
export function getHospital(id) {
  return request({
    url: '/info/hospital/' + id,
    method: 'get'
  })
}

// 新增医院管理
export function addHospital(data) {
  return request({
    url: '/info/hospital',
    method: 'post',
    data: data
  })
}

// 修改医院管理
export function updateHospital(data) {
  return request({
    url: '/info/hospital',
    method: 'put',
    data: data
  })
}

// 删除医院管理
export function delHospital(id) {
  return request({
    url: '/info/hospital/' + id,
    method: 'delete'
  })
}
