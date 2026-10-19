import request from "@/utils/request";

const api_name = "/info/patientsInfo";

export default {
  insertPatientsInfo(data){
    return request({
      url: `${api_name}`,
      method: "post",
      data: data,
    });
  },

  //当前登录用户绑定的就诊人列表
  getMine() {
    return request({
      url: `${api_name}/mine`,
      method: "get",
    });
  }
};
