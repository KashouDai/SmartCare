<template>
  <!-- header -->
  <div class="nav-container page-component">
    <!--左侧导航 #start -->
    <div class="nav left-nav">
      <div class="nav-item selected">
        <span class="v-link selected dark" :onclick="'javascript:window.location=\'/hospital/'+hoscode+'\''">预约挂号 </span>
      </div>
      <div class="nav-item ">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/detail/'+hoscode+'\''"> 医院详情 </span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/notice/'+hoscode+'\''"> 预约须知 </span>
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
      <div class="hospital-source-list">
        <div class="header-wrapper" style="justify-content:normal">
          <span class="v-link clickable" @click="show()">{{ hospital.hospitalName }}</span>
          <div class="split"></div>
          <div>{{ getLevel(hospital.level) }}</div>
        </div>
        <div class="title mt20"> {{ deptInfo.deptName }}</div>
        <!-- 号源列表 #start -->
        <div class="mt60">
          <div class="calendar-list-wrapper">
            <!-- item.depNumber == -1 ? 'gray space' : item.depNumber == 0 ? 'gray' : 'small small-space'-->
            <!-- selected , index == activeIndex ? 'selected' : ''-->
            <div :class="'calendar-item '+item.curClass" style="width: 120px;"
                 v-for="(item, index) in bookingScheduleList" :key="item.id"
                 @click="selectDate(item, index)">

              <div
                class="date-wrapper">
                <div>{{ item.date }}</div>
                <div class="week">{{ item.weekday }}</div>
              </div>
              <div class="status-wrapper" v-if="item.state == '0'">{{ item.surplusState == 0 ? '无号' : '有号' }}</div>
              <div class="status-wrapper" v-if="item.state == '1'">即将放号</div>
              <div class="status-wrapper" v-if="item.state == '2'">停止挂号</div>
            </div>
          </div>
          <!-- 分页 -->
          <el-pagination
            class="pagination"
            layout="prev, pager, next"
            :current-page="queryParams.pageNum"
            :page-size="queryParams.pageSize"
            :total="hospital.appointmentDay + 1"
            @current-change="getPage">
          </el-pagination>
        </div>

        <!-- 即将放号 #start-->
        <div class="countdown-wrapper mt60" v-if="!tabShow">
          <div class="countdonw-title"> {{ time }}<span class="v-link selected">{{ hospital.releaseTime }} </span>放号
          </div>
          <div class="countdown-text"> 倒 计 时
            <div>
              <span class="number">{{ timeString }}</span>
            </div>
          </div>
        </div>
        <!-- 即将放号 #end-->

        <!-- 号源列表 #end -->
        <!-- 上午号源 #start -->
        <div class="mt60" v-if="tabShow">
          <div class="">
            <div class="list-title">
              <div class="block"></div>
              上午号源
            </div>
            <div v-for="item in dutyDoctroList" v-if="item.dutyPeriod == 1" :key="item.id">
              <div class="list-item">
                <div class="item-wrapper">
                  <div class="title-wrapper">
                    <div class="name"> {{ item.doctorName }}</div>
                  </div>
                  <div class="special-wrapper">{{ item.doctorIntroduction }}</div>
                </div>
                <div class="right-wrapper">
                  <div class="fee"> ￥{{ item.registrationFee }}
                  </div>
                  <div class="button-wrapper">
                    <div class="v-button" @click="booking(item.scheduleId, item.availableNumber)"
                         :style="item.availableNumber == 0 || pageFirstStatus == 2 ? 'background-color: #7f828b;' : ''">
                      <span>剩余
                        <span class="number">{{ (item.appointmentsNum - item.appointmentsNumMade) }}</span></span></div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
        <!-- 上午号源 #end -->
        <!-- 下午号源 #start -->
        <div class="mt60" v-if="tabShow">
          <div class="">
            <div class="list-title">
              <div class="block"></div>
              下午号源
            </div>
            <div v-for="item in dutyDoctroList" v-if="item.dutyPeriod == 2" :key="item.id">
              <div class="list-item">
                <div class="item-wrapper">
                  <div class="title-wrapper">
                    <div class="name"> {{ item.doctorName }}</div>
                  </div>
                  <div class="special-wrapper">{{ item.doctorIntroduction }}</div>
                </div>
                <div class="right-wrapper">
                  <div class="fee"> ￥{{ item.registrationFee }}
                  </div>
                  <div class="button-wrapper">
                    <div class="v-button" @click="booking(item.scheduleId, item.availableNumber)"
                         :style="item.availableNumber == 0 || pageFirstStatus == 2 ? 'background-color: #7f828b;' : ''">
                      <span>剩余
                        <span class="number">{{ (item.appointmentsNum - item.appointmentsNumMade) }}</span></span></div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
        <!-- 下午号源 #end -->
      </div>
    </div>
    <!-- 右侧内容 #end -->
  </div>
  <!-- footer -->
</template>

<script>
import '~/assets/css/hospital_personal.css'
import '~/assets/css/hospital.css'

import hospitalApi from '@/api/hosp'
import deptApi from '@/api/dept'
import appointmentsApi from '@/api/appointments'
import dictApi from "@/api/dict";
import dutyApi from "@/api/dutyInfo";

export default {

  data() {
    return {
      hoscode: null,
      deptId: null,
      workDate: null,

      queryParams: {
        pageNum: 1,
        pageSize: 7,
        hospitalCode: null,
        deptId: null,
      },

      dutyParams: {
        hospitalId: null,
        deptId: null,
        dutyDate: null
      },
      bookingScheduleList: [],
      dutyDoctroList: [],
      hostypeList: [],
      hospital: {},
      deptInfo: {},
      nextWorkDate: null, // 下一页第一个日期
      preWorkDate: null, // 上一页第一个日期

      tabShow: true, //挂号列表与即将挂号切换
      activeIndex: 0,

      timeString: null,
      time: '今天',
      timer: null,

      pageFirstStatus: 0 // 第一页第一条数据状态
    }
  },

  created() {
    this.hoscode = this.$route.query.hoscode
    this.deptId = this.$route.query.depcode
    this.queryParams.hospitalCode = this.$route.query.hoscode
    this.queryParams.deptId = this.$route.query.depcode
    this.workDate = this.getCurDate()

    this.init();

  },

  methods: {
    init(){
      this.getHospitalInfo();
      this.getDeptByDeptId();
      this.getBookingScheduleList();
      //查询医院等级列表
      dictApi.getDictByType("hospital_level").then(response => {
        this.hostypeList = []
        this.hostypeList.push({"dictLabel": "全部", "dictValue": ""})
        for (let i in response.data) {
          this.hostypeList.push(response.data[i]);
        }
      })
    },

    getPage(page = 1) {
      this.queryParams.pageNum = page
      this.workDate = null
      this.activeIndex = 0
      this.getBookingScheduleList()

    },

    getHospitalInfo() {
      hospitalApi.getHospital(this.hoscode).then(response => {
        this.hospital = response.data;
      })


    },

    getDeptByDeptId() {
      deptApi.getDeptByDeptId(this.deptId).then(response => {
        this.deptInfo = response.data;
      })
    },

    getBookingScheduleList() {
      appointmentsApi.getBookingScheduleList(this.queryParams).then(response => {
        this.bookingScheduleList = response.rows;
        this.dealClass()

        // 分页后workDate=null，默认选中第一个
        if (this.workDate == null) {
          this.workDate = this.bookingScheduleList[0].date
        }
        this.findScheduleList()
      })
    },

    // 获取等级
    getLevel(item) {
      for (let i in this.hostypeList) {
        if (item == this.hostypeList[i].dictValue) {
          return this.hostypeList[i].dictLabel;
        }
      }
    },

    findScheduleList() {
      this.dutyParams.deptId = this.deptId;
      this.dutyParams.dutyDate = this.workDate;
      this.dutyParams.hospitalId = this.hospital.id;
      dutyApi.getDutyDoctorInfo(this.dutyParams).then(response => {
        this.dutyDoctroList = response.data
      })
    },

    selectDate(item, index) {
      this.workDate = item.date
      this.activeIndex = index
      //清理定时
      if(this.timer != null) clearInterval(this.timer)
      this.dealClass();
      // 是否即将放号
      if(item.state == 1) {
        this.tabShow = false
        // 放号时间
        let releaseTime = new Date(this.getCurDate() + ' ' + this.hospital.releaseTime).getTime()
        let nowTime = new Date().getTime();
        this.countDown(releaseTime, nowTime)

      } else {
        this.tabShow = true
        this.getHospitalInfo()
        this.findScheduleList()
      }
    },

    dealClass() {
      this.pageFirstStatus = this.bookingScheduleList[this.activeIndex].state
      //处理样式
      for (let i = 0; i < this.bookingScheduleList.length; i++) {
        let curClass = this.bookingScheduleList[i].surplusState == 0 ? 'gray space'  : 'small small-space'
        curClass += i == this.activeIndex ? ' selected' : ''
        this.bookingScheduleList[i].curClass = curClass
      }
    },

    getCurDate() {
      let datetime = new Date()
      let year = datetime.getFullYear()
      let month = datetime.getMonth() + 1 < 10 ? '0' + (datetime.getMonth() + 1) : datetime.getMonth() + 1
      let date = datetime.getDate() < 10 ? '0' + datetime.getDate() : datetime.getDate()
      return year + '-' + month + '-' + date
    },

    countDown(releaseTime, nowTime) {
      //计算倒计时时长
      let secondes = 0;
      if(releaseTime > nowTime) {
        this.time = '今天'
        //当前时间到放号时间的时长
        secondes = Math.floor((releaseTime - nowTime) / 1000);
      } else {
        this.time = '明天'
        //计算明天放号时间
        let releaseDate = new Date(releaseTime)
        releaseTime = new Date(releaseDate.setDate(releaseDate.getDate() + 1)).getTime()
        //当前时间到明天放号时间的时长
        secondes = Math.floor((releaseTime - nowTime) / 1000);
      }

      //定时任务
      this.timer = setInterval(() => {
        secondes = secondes - 1
        if(secondes <= 0) {
          clearInterval(timer);
          this.init()
        }
        this.timeString = this.convertTimeString(secondes)
      }, 1000);
      // 通过$once来监听定时器，在beforeDestroy钩子可以被清除。
      this.$once('hook:beforeDestroy', () => {
        clearInterval(timer);
      })
    },

    convertTimeString(allseconds) {
      if(allseconds <= 0) return '00:00:00'
      // 计算天数
      let days = Math.floor(allseconds / (60 * 60 * 24));
      // 小时
      let hours = Math.floor((allseconds - (days * 60 * 60 * 24)) / (60 * 60));
      // 分钟
      let minutes = Math.floor((allseconds - (days * 60 * 60 * 24) - (hours * 60 * 60)) / 60);
      // 秒
      let seconds = allseconds - (days * 60 * 60 * 24) - (hours * 60 * 60) - (minutes * 60);

      //拼接时间
      let timString = "";
      if (days > 0) {
        timString = days + "天:";
      }
      return timString += hours + " 时 " + minutes + " 分 " + seconds + " 秒 ";
    },

    show() {
      window.location.href = '/hospital/' + this.hoscode
    },

    booking(scheduleId, availableNumber) {
      debugger
      if(availableNumber == 0 || this.pageFirstStatus == 2) {
        this.$message.error('不能预约')
      } else {
        window.location.href = '/hospital/booking?scheduleId=' + scheduleId
      }
    }
  }
}
</script>
