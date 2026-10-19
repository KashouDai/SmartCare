import axios from 'axios'
import { Notification, MessageBox, Message, Loading } from 'element-ui'
import errorCode from '@/utils/errorCode'
import cache from '@/plugins/cache'
import { getToken } from '@/utils/auth'
import { tansParams, blobValidate } from "@/utils/common";

// 创建axios实例
const service = axios.create({
    baseURL: process.server
        ? (process.env.NUXT_INTERNAL_API_BASE_URL || process.env.NUXT_API_BASE_URL || '/api')
        : (process.env.NUXT_API_BASE_URL || '/api'),
    timeout: 15000 // 请求超时时间
})
// http request 拦截器
service.interceptors.request.use(config => {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
  // 是否需要防止数据重复提交
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken() // 让每个请求携带自定义token 请根据实际情况自行修改
  }
  // get请求映射params参数
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params);
    url = url.slice(0, -1);
    config.params = {};
    config.url = url;
  }
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length; // 请求数据大小
    const limitSize = 5 * 1024 * 1024; // 限制存放数据5M
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
      return config;
    }
    const sessionObj = cache.session.getJSON('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url;                  // 请求地址
      const s_data = sessionObj.data;                // 请求数据
      const s_time = sessionObj.time;                // 请求时间
      const interval = 1000;                         // 间隔时间(ms)，小于此时间视为重复提交
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
        const message = '数据正在处理，请勿重复提交';
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  return config
}, error => {
  console.log(error)
  Promise.reject(error)
})

// http response 拦截器
service.interceptors.response.use(
    response => {

      // 未设置状态码则默认成功状态
      const code = response.data.code || 200;
      const msg = errorCode[code] || response.data.msg || errorCode['default']
        //状态码是208
        if(code === 208) {
            //弹出登录输入框
            loginEvent.$emit('loginDialogEvent')
            return
        } else if (code === 500) {
          Message({ message: msg, type: 'error' })
          return Promise.reject(new Error(msg))
        } else if (code === 601) {
          Message({ message: msg, type: 'warning' })
          return Promise.reject('error')
        } else if (code !== 200) {
          Notification.error({ title: msg })
          return Promise.reject('error')
        } else {
          return response.data
        }
    },
  error => {
    console.log('err' + error)
    let { message } = error;
    if (message == "Network Error") {
      message = "后端接口连接异常";
    } else if (message.includes("timeout")) {
      message = "系统接口请求超时";
    } else if (message.includes("Request failed with status code")) {
      message = "系统接口" + message.substr(message.length - 3) + "异常";
    }
    Message({ message: message, type: 'error', duration: 5 * 1000 })
    return Promise.reject(error)
  }
)
export default service
