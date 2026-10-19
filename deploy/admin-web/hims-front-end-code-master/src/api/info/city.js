import request from '@/utils/request'

// 查询省份下拉列表
export function listProvince() {
  return request({
    url: '/info/city',
    method: 'get'
  })
}

// 查询医院管理详细
export function listCity(pcode) {
  return request({
    url: '/info/city/city/' + pcode,
    method: 'get'
  })
}

export function listTown(ccode) {
  return request({
    url: '/info/city/town/' + ccode,
    method: 'get'
  })
}
