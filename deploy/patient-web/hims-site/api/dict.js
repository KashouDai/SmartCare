import request from "@/utils/request";

const api_name_cmn = "/system/dict/data";

export default {
  //根据dictCode查询id，再根据id查询子数据
  findByDictCode(dictCode) {
    return request({
      url: `${api_name_cmn}/findByDictCode/${dictCode}`,
      method: "get",
    });
  },

  //根据id查看数据词典信息，再根据parent_id查看是否有子节点
  findByParentId(id) {
    return request({
      url: `${api_name_cmn}/findChildData/${id}`,
      method: "get",
    });
  },

  //根据value查询数据词典名称
  findNameByValue(value) {
    return request({
      url: `${api_name_cmn}/getName/${value}`,
      method: "get",
    });
  },

  getDictByType(dictType){
    return request({
      url: `${api_name_cmn}/type/${dictType}`,
      method: "get",
    });
  }
};
