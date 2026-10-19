import request from "@/utils/request";

const api_name_hosp = `/info/hospital`;

export default {
  //用户patient-web，分页查询所有医院
  getPageList(query) {
    return request({
      url: `${api_name_hosp}/list`,
      method: 'get',
      params: query
    });
  },

  // 根据hoscode获取医院详情
  getHospital(hoscode) {
    return request({
      url: `${api_name_hosp}/getDetail/${hoscode}`,
      method: "get",
    });
  },

  //根据hoscode获取科室列表
  findDepartment(hoscode) {
    return request({
      url: `${api_name_hosp}/department/${hoscode}`,
      method: "get",
    });
  },

  //获取排班可预约日期数据
  getBookingScheduleRule(page, limit, hoscode, depcode) {
    return request({
      url: `${api_name_hosp}/auth/getBookingScheduleRule/${page}/${limit}/${hoscode}/${depcode}`,
      method: "get",
    });
  },

  //获取排班数据
  findScheduleList(hoscode, depcode, workDate) {
    return request({
      url: `${api_name_hosp}/auth/findScheduleList/${hoscode}/${depcode}/${workDate}`,
      method: "get",
    });
  },

  getSchedule(scheduleId) {
    return request({
      url: `${api_name_hosp}/getSchedule/${scheduleId}`,
      method: "get",
    });
  },
};
