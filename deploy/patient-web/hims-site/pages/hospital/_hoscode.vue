<template>

  <!-- header -->
  <div class="nav-container page-component">
    <!--左侧导航 #start -->
    <div class="nav left-nav">
      <div class="nav-item selected">
        <span :onclick="'javascript:window.location=\'/hospital/'+hospital.hospitalCode+'\''"
              class="v-link selected dark">预约挂号 </span>
      </div>
      <div class="nav-item ">
        <span :onclick="'javascript:window.location=\'/hospital/detail/'+hospital.hospitalCode+'\''"
              class="v-link clickable dark"> 医院详情 </span>
      </div>
      <div class="nav-item">
        <span :onclick="'javascript:window.location=\'/hospital/notice/'+hospital.hospitalCode+'\''"
              class="v-link clickable dark"> 预约须知 </span>
      </div>
<!--      <div class="nav-item "><span-->
<!--        class="v-link clickable dark"> 停诊信息 </span>-->
<!--      </div>-->
<!--      <div class="nav-item "><span-->
<!--        class="v-link clickable dark"> 查询/取消 </span>-->
<!--      </div>-->
    </div>
    <!-- 左侧导航 #end -->

    <!-- 右侧内容 #start -->
    <div class="page-container">
      <div class="hospital-home">
        <div class="common-header">
          <div class="title-wrapper"><span
            class="hospital-title">{{ hospital.hospitalName }}</span>
            <div class="icon-wrapper">
              <div class="iconfont">{{ getLevel(hospital.level) }}</div>
            </div>
          </div>
        </div>
        <div class="info-wrapper">
          <!--          <img class="hospital-img" :src="'data:image/jpeg;base64,'+hospital.logoData" :alt="hospital.hosname">-->
          <div class="content-wrapper">
            <div> 挂号规则</div>
            <div class="line">
              <div>
                <span class="label">预约周期：</span>
                <span> {{hospital.appointmentDay}} 天</span>
              </div>
              <div class="space">
                <span class="label">放号时间：</span>
                <span>{{ hospital.releaseTime }}</span>
              </div>
              <div class="space">
                <span class="label">停挂时间：</span>
                <span>{{ hospital.suspendTime }}</span>
              </div>
            </div>
            <div class="line">
              <span class="label">退号时间：</span>
              <span>就诊前一工作日 15:30 前取消</span>
            </div>
            <div style="margin-top:20px"> 医院预约规则</div>
            <div class="rule-wrapper">
              <span>门诊楼一层大厅挂号窗口取号</span>
            </div>
          </div>
        </div>
        <div class="hospital-home">
          <div class="info-wrapper">
            <div class="content-wrapper">
            <div> 选择科室</div>
            </div>
          </div>
        </div>
        <div class="select-dept-wrapper">
          <div class="department-wrapper">
            <div class="hospital-department">
              <div class="dept-list-wrapper el-scrollbar" style="height: 100%;">
                <div class="dept-list el-scrollbar__wrap" style="margin-bottom: -17px; margin-right: -17px;">
                  <div class="el-scrollbar__view">
                    <div v-for="(item,index) in departmentVoList" :key="item.id"
                         :class="index == activeIndex ? 'selected' : ''"
                         class="sub-item" @click="move(index,item.id)">
                      {{ item.label }}
                    </div>
                  </div>
                </div>
                <div class="el-scrollbar__bar is-horizontal">
                  <div class="el-scrollbar__thumb" style="transform: translateX(0%);"></div>
                </div>
                <div class="el-scrollbar__bar is-vertical">
                  <div class="el-scrollbar__thumb" style="transform: translateY(0%); height: 91.4761%;"></div>
                </div>
              </div>
            </div>
          </div>
          <div class="sub-dept-container">
            <div v-for="(item,index) in departmentVoList" :id="item.id" :key="item.id"
                 :class="index == 0 ? 'selected' : ''" class="sub-dept-wrapper">
              <div class="sub-title">
                <div class="block selected"></div>
                {{ item.label }}
              </div>
              <div class="sub-item-wrapper">
                <div v-for="it in item.children" :key="it.id" class="sub-item" @click="schedule(it.id)">
                  <span class="v-link clickable">{{ it.label }} </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 右侧内容 #end -->
  </div>
  <!-- footer -->
</template>


<script>
import "~/assets/css/hospital_personal.css";
import "~/assets/css/hospital.css";
import "~/assets/css/_hoscode.css";
import hospitalApi from "@/api/hosp";
import userInfoApi from "@/api/userInfo";
import dictApi from "@/api/dict";
import deptApi from "@/api/dept";
import {getParam, getToken} from "@/utils/auth";

export default {
  data() {
    return {
      hoscode: null,
      activeIndex: 0,
      hostypeList: [],// 医院等级列表
      hospital: {
        param: {},
      },
      bookingRule: {},
      departmentVoList: [],
    };
  },
  created() {
    this.hoscode = this.$route.params.hoscode;
    this.init();
  },

  methods: {
    init() {
      hospitalApi.getHospital(this.hoscode).then((response) => {
        this.hospital = response.data;
        // this.bookingRule = response.data.bookingRule;
      });

      deptApi.getDeptTree().then((response) => {
        this.departmentVoList = response.data[0].children;
      });

      //查询医院等级列表
      dictApi.getDictByType("hospital_level").then(response => {
        this.hostypeList = []
        for (let i in response.data) {
          this.hostypeList.push(response.data[i]);
        }
      })
    },

    move(index, id) {
      this.activeIndex = index;
      document.getElementById(id).scrollIntoView();
    },
    // 获取等级
    getLevel(item) {
      for (let i in this.hostypeList) {
        if (item == this.hostypeList[i].dictValue) {
          return this.hostypeList[i].dictLabel;
        }
      }
    },

    schedule(depcode) {
      // 登录判断
      let token = getToken();
      if (!token) {
        loginEvent.$emit('loginDialogEvent');
        return;
      }


      //判断认证
      let userId = getParam("userId");
      userInfoApi.getUserInfoById(userId).then((response) => {
        let idCard = response.data.idCard
        if (!idCard) {
          window.location.href = "/user";
          return;
        }
      });

      window.location.href =
        "/hospital/schedule?hoscode=" +
        this.hospital.hospitalCode +
        "&depcode=" +
        depcode;
    },
  },
};
</script>
