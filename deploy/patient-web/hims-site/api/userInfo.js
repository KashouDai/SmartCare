import request from "@/utils/request";

const api_name = `/api/user`;

export default {
  login(userInfo) {
    return request({
      url: `/login1`,
      method: `post`,
      data: userInfo,
    });
  },

  register(userInfo) {
    return request({
      url: '/register',
      method: 'post',
      data: userInfo,
    })
  },

  logout() {
    return request({
      url: '/logout',
      method: 'post'
    })
  },

  getUserInfoById(userId){
    return request({
      url: `/system/user/`+userId,
      method: `get`
    });
  },
  updateUserInfo(userInfo){
    return request({
      url: `/system/user/updateUserInfo`,
      method: 'post',
      data: userInfo
    })
  },
  resetPassword(userInfo){
    return request({
      url: '/system/user/resetPassword',
      method: 'post',
      data: userInfo,
    })
  },
}
