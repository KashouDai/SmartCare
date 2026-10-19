import request from '@/utils/request'

// 查询就诊历史列表
export function listHistory(query) {
  return request({
    url: '/info/history/list',
    method: 'get',
    params: query
  })
}

// 查询就诊历史详细
export function getHistory(historyId) {
  return request({
    url: '/info/history/' + historyId,
    method: 'get'
  })
}

// 新增就诊历史
export function addHistory(data) {
  return request({
    url: '/info/history',
    method: 'post',
    data: data
  })
}

// 修改就诊历史
export function updateHistory(data) {
  return request({
    url: '/info/history',
    method: 'put',
    data: data
  })
}

// 删除就诊历史
export function delHistory(historyId) {
  return request({
    url: '/info/history/' + historyId,
    method: 'delete'
  })
}
