import request from "@/utils/request";

const api_name = "/info/city";

export default {
  getTownListByCCode(cityCode){
    return request({
      url: `${api_name}/town/${cityCode}`,
      method: "get",
    });
  }
};
