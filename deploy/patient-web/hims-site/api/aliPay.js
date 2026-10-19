import request from "@/utils/request";

const api_name = "/order/alipay";

export default {
  cancelOrder(orderId) {
    return request({
      url: `${api_name}/return/` + orderId,
      method: 'get'
    })
  }
};
