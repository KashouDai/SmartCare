<template>
  <div class="home page-component">
    <el-carousel>
      <el-carousel-item v-for="item in 2" :key="item">
        <img alt="" src="~assets/images/web-banner1.png">
      </el-carousel-item>
    </el-carousel>
    <!-- bottom -->
    <div class="bottom">
      <div class="left">
        <div class="home-filter-wrapper">
          <div class="title"> 医院</div>
          <div>
            <div class="filter-wrapper">
              <span class="label">等级：</span>
              <div class="condition-wrapper">
                <span v-for="(item,index) in hostypeList" :key="item.dictValue"
                      :class="hostypeActiveIndex == index ? 'selected' : ''" class="item v-link clickable"
                      @click="hospitalLevel(item.dictValue, index)">{{ item.dictLabel }}</span>
              </div>
            </div>
            <div class="filter-wrapper">
              <span class="label">地区：</span>
              <div class="condition-wrapper">
                <span v-for="(item,index) in districtList" :key="item.tcode"
                      :class="provinceActiveIndex == index ? 'selected' : ''" class="item v-link clickable"
                      @click="districtSelect(item.tcode, index)">{{
                    item.tname
                  }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="v-scroll-list hospital-list">
          <div v-for="item in hospitalList" :key="item.id" class="v-card clickable list-item">
            <div class="">
              <div class="hospital-list-item hos-item" index="0" @click="show(item.hospitalCode)">
                <div class="wrapper">
                  <div class="hospital-title"> {{ item.hospitalName }}</div>
                  <div class="bottom-container">
                    <div class="icon-wrapper">
                      <span class="iconfont"></span>
                      {{ getLevel(item.level) }}
                    </div>
                    <div class="icon-wrapper">
                      <span class="iconfont"></span>每天{{ item.releaseTime }}放号
                    </div>
                  </div>
                </div>
                <!--                <img :src="'data:image/jpeg;base64,'+item.logoData" :alt="item.hosname" class="hospital-img">-->
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="right">
        <div class="common-dept">
          <div class="header-wrapper">
            <div class="title"> 常见科室</div>
            <div class="all-wrapper"><span>全部</span>
              <span class="iconfont icon"></span>
            </div>
          </div>
          <div class="content-wrapper">
            <span class="item v-link clickable dark">神经内科 </span>
            <span class="item v-link clickable dark">消化内科 </span>
            <span class="item v-link clickable dark">呼吸内科 </span>
            <span class="item v-link clickable dark">内科 </span>
            <span class="item v-link clickable dark">神经外科 </span>
            <span class="item v-link clickable dark">妇科 </span>
            <span class="item v-link clickable dark"> 产科 </span>
            <span class="item v-link clickable dark">儿科 </span>
          </div>
        </div>
        <div class="space">
          <div class="header-wrapper">
            <div class="title-wrapper">
              <div class="icon-wrapper"><span
                class="iconfont title-icon"></span>
              </div>
              <span class="title">平台公告</span>
            </div>
            <div class="all-wrapper">
              <span>全部</span>
              <span class="iconfont icon"></span>
            </div>
          </div>
          <div class="content-wrapper">
            <div class="notice-wrapper">
              <div class="point"></div>
              <span class="notice v-link clickable dark">关于延长北京大学国际医院放假的通知 </span>
            </div>
            <div class="notice-wrapper">
              <div class="point"></div>
              <span class="notice v-link clickable dark">北京中医药大学东方医院部分科室医生门诊医 </span>
            </div>
            <div class="notice-wrapper">eee
              <div class="point"></div>
              <span class="notice v-link clickable dark"> 武警总医院号源暂停更新通知 </span>
            </div>
          </div>
        </div>
        <div class="suspend-notice-list space">
          <div class="header-wrapper">
            <div class="title-wrapper">
              <div class="icon-wrapper">
                <span class="iconfont title-icon"></span>ee
              </div>
              <span class="title">停诊公告</span>
            </div>
            <div class="all-wrapper">
              <span>全部</span>
              <span class="iconfont icon"></span>
            </div>
          </div>
          <div class="content-wrapper">
            <div class="notice-wrapper">
              <div class="point"></div>
              <span
                class="notice v-link clickable dark"> 中国人民解放军总医院第六医学中心(原海军总医院)呼吸内科门诊停诊公告 </span>
            </div>
            <div class="notice-wrapper">
              <div class="point"></div>
              <span class="notice v-link clickable dark"> 首都医科大学附属北京潞河医院老年医学科门诊停诊公告 </span>
            </div>
            <div class="notice-wrapper">
              <div class="point"></div>
              <span class="notice v-link clickable dark">中日友好医院中西医结合心内科门诊停诊公告 </span>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>
<script>
import hospitalApi from "@/api/hosp";
import dictApi from "@/api/dict";
import cityApi from "@/api/city";

export default {
  // //服务端渲染异步，显示医院列表
  // asyncData({ params, error }) {
  //   //调用
  //   return hospitalApi.getPageList(this.queryParams).then((response) => {
  //     return {
  //       hospitalList: response.rows,
  //       total: response.total,
  //     };
  //   });
  // },

  data() {
    return {
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        level: null,
        tcode: null,
      },
      hosname: "", //医院名称
      hospitalList: [],// 医院列表
      hostypeList: [], //医院等级集合
      districtList: [], //地区集合
      dictList: [], // 字典集合
      hostypeActiveIndex: 0,
      provinceActiveIndex: 0,
    };
  },

  created() {
    this.init();
    this.getList();
  },

  methods: {
    //查询医院等级列表 和 所有地区列表
    init() {
      //查询医院等级列表
      dictApi.getDictByType("hospital_level").then(response => {
        this.hostypeList = []
        this.hostypeList.push({"dictLabel": "全部", "dictValue": ""})
        for (let i in response.data) {
          this.hostypeList.push(response.data[i]);
        }
      })

      //查询地区数据
      cityApi.getTownListByCCode("440100").then(response => {
        this.districtList = []
        this.districtList.push({"tname": "全部", "tcode": ""})
        for (let i in response.data) {
          this.districtList.push(response.data[i]);
        }
      })
    },

    // 查询字典
    getDict(type) {
      dictApi.getDictByType(type).then(response => {
        this.dictList = response.data
      })
    },

    //查询医院列表
    getList() {
      hospitalApi.getPageList(this.queryParams).then(response => {
        for (let i in response.rows) {
          this.hospitalList.push(response.rows[i]);
        }
      })
    },

    //根据医院等级查询
    hospitalLevel(level, index) {
      //准备数据
      this.hospitalList = [];
      this.hostypeActiveIndex = index;
      this.queryParams.level = level; //调用查询医院列表方法
      this.getList();
    },

    //根据地区查询医院
    districtSelect(tcode, index) {
      this.hospitalList = [];
      this.provinceActiveIndex = index;
      this.queryParams.tcode = tcode;
      this.getList();
    },

    // 获取等级
    getLevel(item) {
      for (let i in this.hostypeList) {
        if(item == this.hostypeList[i].dictValue){
          return this.hostypeList[i].dictLabel;
        }
      }
    },
    //点击某个医院名称，跳转到详情页面中
    show(hoscode) {
      window.location.href = "/hospital/" + hoscode;
    },
  },
};
</script>

