/**
 * 挂号下单页的可验证纯逻辑。
 * 与 Vue 组件解耦，使「就诊人初始选中」与「幂等请求号生命周期」两条规则可直接断言。
 */

// 就诊人列表的初始选中规则：0 个无可选，1 个自动选中，多个必须由用户明确选择（不默认取第一项）
export function resolveInitialPatientId(patientList) {
  if (!patientList || patientList.length !== 1) {
    return null
  }
  return patientList[0].patientId
}

// 生成幂等请求号，格式需匹配后端 ^[A-Za-z0-9_-]{8,64}$
export function generateRequestId() {
  return 'reg-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 12)
}

// 幂等请求号：已有则复用（同一患者同一次预约意图的重试），没有才生成
export function ensureRequestId(currentRequestId) {
  return currentRequestId || generateRequestId()
}
