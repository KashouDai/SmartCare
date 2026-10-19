import request from "@/utils/request";

const api_name = `/order/orderInfo`;

export default {
  //统一下单入口：金额与用户身份由服务端推导，客户端只传排班、就诊人与幂等请求号
  createRegisteredOrder(data) {
    return request({
      url: `${api_name}/registeredOrder`,
      method: 'post',
      data: data
    })
  },
  addOrderInfo(data) {
    return request({
      url: `${api_name}`,
      method: 'post',
      data: data
    })
  },
  getOrderInfo(orderId) {
    return request({
      url: `${api_name}/` + orderId,
      method: 'get'
    })
  },
  getList(params){
    return request({
      url: `${api_name}/getList`,
      params : params
    })
  }

};
