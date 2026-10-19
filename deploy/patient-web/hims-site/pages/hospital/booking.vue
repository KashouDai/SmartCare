<template>
  <!-- header -->
  <div class="nav-container page-component">
    <!--左侧导航 #start -->
    <div class="nav left-nav">
      <div class="nav-item selected">
        <span class="v-link selected dark" :onclick="'javascript:window.location=\'/hospital/'+schedule.hoscode+'\''">预约挂号 </span>
      </div>
      <div class="nav-item ">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/detail/'+schedule.hoscode+'\''"> 医院详情 </span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/notice/'+schedule.hoscode+'\''"> 预约须知 </span>
      </div>
      <!-- <div class="nav-item "><span
        class="v-link clickable dark"> 停诊信息 </span>
      </div>
      <div class="nav-item "><span
        class="v-link clickable dark"> 查询/取消 </span>
      </div> -->
    </div>
    <!-- 左侧导航 #end -->

    <!-- 右侧内容 #start -->
    <div class="page-container">
      <div class="hospital-order">
        <div class="header-wrapper">
          <div class="title mt20"> 确认挂号信息</div>
          <div>
            <div class="sub-title">
              <div class="block"></div>
              确认就诊人：
            </div>
            <div class="content-wrapper">
              <div>
                <el-form label-width="110px" label-position="right">
                  <el-form-item label="就诊人：">
                    <el-select
                      v-model="selectedPatientId"
                      placeholder="请选择就诊人"
                      class="v-select patient-select"
                      :disabled="patientList.length === 0"
                      @change="onPatientChange">
                      <el-option
                        v-for="item in patientList"
                        :key="item.patientId"
                        :label="item.phone ? item.nickName + '（' + item.phone + '）' : item.nickName"
                        :value="item.patientId">
                      </el-option>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="姓名：">
                    {{ selectedPatient.nickName }}
                  </el-form-item>
                  <el-form-item label="性别：">
                    {{ getSex(selectedPatient.sex) }}
                  </el-form-item>
                  <el-form-item label="手机号码：">
                    {{ selectedPatient.phone }}
                  </el-form-item>
                </el-form>
              </div>
            </div>


            <div class="sub-title">
              <div class="block"></div>
              挂号信息
            </div>
            <div class="content-wrapper">
              <div>
                <el-form label-width="110px" label-position="right">
                  <el-form-item label="就诊日期：">
                    {{ schedule.dutyDate }} {{schedule.dutyPeriod == 1 ? '上午' : '下午' }}
                  </el-form-item>
                  <el-form-item label="就诊医院：">
                    {{ schedule.hospitalName }}
                  </el-form-item>
                  <el-form-item label="就诊科室：">
                    {{ schedule.deptName }}
                  </el-form-item>
                  <el-form-item label="医生姓名：">
                    {{ schedule.doctorName }}
                  </el-form-item>
                  <el-form-item label="医生专长：">
                    {{ schedule.doctorIntroduction }}
                  </el-form-item>
                  <el-form-item label="医事服务费：">
                    <div class="fee">{{ schedule.registrationFee }}元</div>
                  </el-form-item>
                </el-form>
              </div>
            </div>

            <div class="bottom-wrapper">
              <div class="button-wrapper">
                <div class="v-button" @click="submitOrder()">{{ submitBnt }}</div>
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
import '~/assets/css/hospital_personal.css'
import '~/assets/css/hospital.css'
import orderInfoApi from '@/api/orderInfo'
import dictApi from "@/api/dict";
import dutyApi from "@/api/dutyInfo";
import patientsInfoApi from "@/api/patientsInfo";
import {resolveInitialPatientId, ensureRequestId} from "@/utils/booking";

export default {

  data() {
    return {
      // 排班id
      scheduleId: null,
      // 排班信息
      schedule: {},
      // 当前登录用户绑定的就诊人
      patientList: [],
      // 选中的就诊人id。0 个就诊人或多于 1 个就诊人时保持 null，必须由用户明确选择
      selectedPatientId: null,
      // 幂等请求号：首次提交时生成，同一患者失败重试复用；切换就诊人后清空
      requestId: '',
      sexList: [],
      activeIndex: 0,
      submitBnt: '确认挂号',
      pay: '', // 支付信息
    }
  },

  computed: {
    // 选中的就诊人
    selectedPatient() {
      return this.patientList.find(item => String(item.patientId) === String(this.selectedPatientId)) || {}
    }
  },

  created() {
    this.scheduleId = this.$route.query.scheduleId

    this.init()
  },

  methods: {
    init() {
      this.getScheduleById()

      // 获取当前登录用户绑定的就诊人
      this.loadPatients()

      // 获取性别字典
      dictApi.getDictByType("sys_user_sex").then(response => {
        this.sexList = []
        for (let i in response.data) {
          this.sexList.push(response.data[i]);
        }
      })
    },

    // 加载就诊人列表：0 个提示并保持未选中，1 个自动选中，多个必须由用户明确选择
    loadPatients() {
      patientsInfoApi.getMine().then(response => {
        this.patientList = response.data || []
        if (this.patientList.length === 0) {
          this.$message.warning('请先完成实名认证或添加就诊人')
          return
        }
        this.selectedPatientId = resolveInitialPatientId(this.patientList)
      })
    },

    // 用户切换就诊人视为新的预约意图：清空 requestId，下次提交重新生成。
    // 挂在 el-select 的 change 上而非 watch，故初始化自动选中唯一就诊人时不会触发重置。
    onPatientChange() {
      this.requestId = ''
    },

    // 根据id获取排班信息
    getScheduleById() {
      dutyApi.getInfoById(this.scheduleId).then(response => {
        this.schedule = response.data
      })
    },

    // 渲染性别
    getSex(item) {
      for (let i in this.sexList) {
        if (item == this.sexList[i].dictValue) {
          return this.sexList[i].dictLabel;
        }
      }
    },

    submitOrder() {
      // 防止重复提交
      if (this.submitBnt == '正在提交...') {
        this.$message.error('重复提交')
        return
      }

      if (!this.selectedPatientId) {
        this.$message.error('请先选择就诊人')
        return
      }

      // 幂等请求号：首次提交生成，同一患者失败重试复用；切换就诊人已在 onPatientChange 中清空
      this.requestId = ensureRequestId(this.requestId)

      this.submitBnt = '正在提交...'

      // 统一下单：金额与用户身份由服务端推导，重试复用同一 requestId
      orderInfoApi.createRegisteredOrder({
        scheduleId: this.scheduleId,
        patientId: this.selectedPatientId,
        requestId: this.requestId
      }).then(response => {
        window.location.href = '/order/show?orderId=' + response.data.orderId
      }).catch(e => {
        this.submitBnt = '确认挂号'
      })
    },
  }
}
</script>
<style>
  .hospital-order .header-wrapper {
    display: -webkit-box;
    display: -ms-flexbox;
    display: block !important;
    -webkit-box-align: center;
    -ms-flex-align: center;
    align-items: center;
  }
  .hospital-order .sub-title {
    letter-spacing: 1px;
    color: #999;
    margin-top: 60px;
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-align: center;
    -ms-flex-align: center;
    align-items: center;
  }
  .hospital-order .content-wrapper .content {
    color: #333;
  }
  .el-form-item {
    margin-bottom: 5px;
  }
  .hospital-order .content-wrapper {
    margin-left: 140px;
    margin-top: 20px;
  }
</style>
