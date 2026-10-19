<template>
    <!-- header -->
    
  <div class="nav-container page-component">
    <!--左侧导航 #start -->
    <div class="nav left-nav">
      <div class="nav-item ">
        <span class="v-link clickable dark" onclick="javascript:window.location='/user'">实名认证 </span>
      </div>
      <div class="nav-item selected">
        <span class="v-link selected dark" onclick="javascript:window.location='/order'"> 挂号订单 </span>
      </div>
<!--      <div class="nav-item ">-->
<!--        <span class="v-link clickable dark" onclick="javascript:window.location='/patient'"> 就诊人管理 </span>-->
<!--      </div>-->
<!--      <div class="nav-item ">-->
<!--        <span class="v-link clickable dark"> 修改账号信息 </span>-->
<!--      </div>-->
<!--      <div class="nav-item ">-->
<!--        <span class="v-link clickable dark"> 意见反馈 </span>-->
<!--      </div>-->
    </div>
        <!-- 左侧导航 #end -->
        <!-- 右侧内容 #start -->
        
    <div class="page-container">
      <div v-if="loading" class="order-loading">正在加载订单详情...</div>
      <div v-else class="order-detail">
        <div class="title"> 挂号详情</div>
        <div class="status-bar">
          <div class="left-wrapper">
            <div class="status-wrapper BOOKING_SUCCESS">
              <span class="iconfont"></span> {{ getPayState(orderInfo.payState) }}
            </div>
          </div>
        </div>

        <div class="info-wrapper">
          <div class="title-wrapper">
            <div class="block"></div>
            <div>挂号信息</div>
          </div>
          <div class="info-form">
            <el-form ref="form">

              <el-form-item label="就诊人信息：">
                <div class="content"><span>{{ appointmentsInfo.patientName }}</span></div>
              </el-form-item>
              <el-form-item label="就诊日期：">
                <div class="content"><span>{{ appointmentsInfo.dutyDate }} {{ appointmentsInfo.dutyPeriod == 1 ? '上午'
                  : '下午' }}</span></div>
              </el-form-item>
              <el-form-item label="就诊医院：">
                <div class="content"><span>{{ dutyInfo.hospitalName }} </span></div>
              </el-form-item>
              <el-form-item label="就诊科室：">
                <div class="content"><span>{{ dutyInfo.deptName }} </span></div>
              </el-form-item>
              <el-form-item label="医事服务费：">
                <div class="content">
                  <div class="fee">{{ dutyInfo.registrationFee }}元
                  </div>
                </div>
              </el-form-item>
              <el-form-item label="挂号单号：">
                <div class="content"><span>{{ orderInfo.orderId }} </span></div>
              </el-form-item>
              <el-form-item label="挂号时间：">
                <div class="content"><span>{{ appointmentsInfo.createTime }}</span></div>
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="bottom-wrapper mt60" v-if="orderInfo.payState == 0">
          <div class="button-wrapper">
            <div class="v-button" @click="pay()">支付</div>
          </div>
          <div class="button-wrapper ml20">
            <div class="v-button white" @click="cancelOrder()">取消预约</div>
          </div>
        </div>
        <div class="bottom-wrapper mt60" v-if="orderInfo.payState == 1">
          <div class="button-wrapper">
            <div class="v-button white" @click="cancelOrder()">取消预约</div>
          </div>
        </div>
        <div class="rule-wrapper mt40">
          <div class="rule-title"> 注意事项</div>
          <div>1、请确认就诊人信息是否准确，若填写错误将无法取号就诊，损失由本人承担；<br>
            <span style="color:red">2、持身份证、医保卡、电子医保凭证、身份证号码、登记号中任一凭证取号；</span><br>
            3、若不能按时就诊，请至少在就诊时间段前30分钟取消；<br>
          </div>
        </div>
      </div>
    </div>
    <!-- 右侧内容 #end -->
  </div>
  <!-- footer -->
</template>
<script>
import "~/assets/css/hospital_personal.css";
import "~/assets/css/hospital.css";
import orderInfoApi from "@/api/orderInfo";
import aliPayApi from "@/api/aliPay";
import appointmentsInfoApi from "@/api/appointments";
import dutyInfoApi from "@/api/dutyInfo";
import dictApi from "@/api/dict";

export default {
  data() {
    return {
      orderId: null,
      orderInfo: {},
      appointmentsInfo: {},
      dutyInfo: {},
      loading: true,
      timer: null, // 定时器名称
      payStateList: [], // 支付状态字典
    };
  },
  created() {
    this.orderId = this.$route.query.orderId;
    this.init();

    // 获取证件类型字典
    dictApi.getDictByType("pay_state").then(response => {
      this.payStateList = []
      for (let i in response.data) {
        this.payStateList.push(response.data[i]);
      }
    });
  },
  methods: {
    init() {
      this.loading = true;
      // 获取订单信息
      orderInfoApi.getOrderInfo(this.orderId).then((response) => {
        this.orderInfo = response.data;
        // 获取预约信息
        return appointmentsInfoApi.getAppointments(this.orderInfo.appointmentsId).then((response) => {
          this.appointmentsInfo = response.data;
          // 获取排班信息
          return dutyInfoApi.getInfoById(this.appointmentsInfo.scheduleId).then((response) => {
            this.dutyInfo = response.data;
            this.loading = false;
          })
        })
      }).catch(() => {
        this.loading = false;
        this.$message.error('订单详情加载失败，请稍后重试');
      });
    },
    pay() {
      const subject = this.appointmentsInfo.patientName +
        this.appointmentsInfo.dutyDate +
        (this.appointmentsInfo.dutyPeriod == 1 ? '上午' : '下午') + '挂号费'
      const query = new URLSearchParams({
        subject,
        traceNo: String(this.orderId),
        totalAmount: String(this.dutyInfo.registrationFee)
      })
      window.open('/api/order/alipay/pay?' + query.toString())
    },
    // 渲染支付状态
    getPayState(item) {
      for (let i in this.payStateList) {
        if (item == this.payStateList[i].dictValue) {
          return this.payStateList[i].dictLabel;
        }
      }
    },
    cancelOrder() {
      this.$confirm("确定取消预约吗?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          // promise
          // 点击确定，远程调用
          return aliPayApi.cancelOrder(this.orderId);
        })
        .then((response) => {
          this.$message.success("取消成功");
          this.init();
        })
    },
  },
};
</script>
<style>
  .info-wrapper  {
      padding-left: 0;
      padding-top: 0;
}
  .content-wrapper  {
      color: #333;
      font-size: 14px;
      padding-bottom: 0;
}
  .bottom-wrapper  {
      width: 100%;
}
  .button-wrapper  {
      margin: 0;
}
  .el-form-item  {
      margin-bottom: 5px;
}
  .bottom-wrapper .button-wrapper  {
      margin-top: 0;
}
</style>
