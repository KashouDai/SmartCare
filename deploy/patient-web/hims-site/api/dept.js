import request from "@/utils/request";

const api_name = "/system/dept";

export default {
  getDeptTree(){
    return request({
      url: `${api_name}/getDeptTree`,
      method: "get",
    });
  },
  getDeptByDeptId(deptId){
    return request({
      url: `${api_name}/${deptId}`,
      method: "get",
    });
  },
};
