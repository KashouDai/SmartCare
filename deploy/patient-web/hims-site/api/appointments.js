import request from "@/utils/request";

const api_name = "/info/appointments";

export default {
  getBookingScheduleList(query) {
    return request({
      url: `${api_name}/getBookingScheduleList`,
      method: "get",
      params: query
    });
  },
  addAppointments(query) {
    return request({
      url: `${api_name}`,
      method: 'post',
      data: query
    })
  },
  getAppointments(appointmentsId) {
    return request({
      url: `${api_name}/` + appointmentsId,
      method: 'get'
    })
  }
};
