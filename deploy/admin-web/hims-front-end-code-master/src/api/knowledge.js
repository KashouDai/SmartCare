import request from '@/utils/request'

// 提交知识库问答请求。
export function askKnowledge(data) {
  return request({
    url: '/knowledge/ask',
    method: 'post',
    data: data,
    timeout: 90000
  })
}

// 上传知识库文档。
export function uploadKnowledgeDocument(formData) {
  return request({
    url: '/knowledge/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
