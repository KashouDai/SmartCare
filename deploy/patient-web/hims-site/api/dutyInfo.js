import request from "@/utils/request";

const api_name = "/info/duty";

export default {
  getDutyDoctorInfo(query){
    return request({
      url: `${api_name}/getDutyDoctorInfo`,
      method: "get",
      params: query
    });
  },

  getInfoById(scheduleId){
    return request({
      url: `${api_name}/${scheduleId}`,
      method: "get"
    });
  }
};
