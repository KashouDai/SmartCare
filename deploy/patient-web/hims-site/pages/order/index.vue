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
    <!-- 左侧导航 #end -->
        <!-- 右侧内容 #start -->

    <div class="page-container">
      <div class="personal-order">
        <div class="title"> 挂号订单</div>
        <el-form :inline="true">
          <el-form-item label="订单状态：">
            <el-select v-model="queryParams.payState" placeholder="全部" class="v-select patient-select" style="width: 200px;">
              <el-option
                v-for="item in payStatusList"
                :key="item.dictValue"
                :label="item.dictLabel"
                :value="item.dictValue">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="text" class="search-button v-link highlight clickable selected" @click="fetchData()">
              查询
            </el-button>
          </el-form-item>
        </el-form>
        <div class="table-wrapper table">
          <el-table
            :data="list"
            :header-cell-style="{'text-align':'center'}"
            :cell-style="{'text-align':'center'}"
            stripe
            style="width: 100%">
            <el-table-column
              label="就诊时间"
              width="140">
              <template slot-scope="scope">
                {{ scope.row.dutyDate }} {{ scope.row.dutyPeriod === 1 ? '上午' : '下午' }}
              </template>
            </el-table-column>
            <el-table-column
              prop="hospitalName"
              label="医院"
              width="150">
            </el-table-column>
            <el-table-column
              prop="deptName"
              label="科室">
            </el-table-column>
            <el-table-column
              prop="doctorName"
              label="医生">
            </el-table-column>
            <el-table-column
              prop="orderAmount"
              label="订单金额">
            </el-table-column>
            <el-table-column
              prop="createTime"
              label="创建时间"
              width="150">
            </el-table-column>
            <el-table-column
              label="订单状态">
              <template slot-scope="scope">
                {{ getPayState(scope.row.payState) }}
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template slot-scope="scope">
                <el-button type="text" class="v-link highlight clickable selected" style="font-size: 12px"
                           @click="show(scope.row.orderId)">详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <!-- 分页 -->
        <el-pagination
          class="pagination"
          layout="prev, pager, next"
          :current-page="queryParams.pageNum"
          :total="total"
          :page-size="queryParams.pageSize"
          @current-change="fetchData">
        </el-pagination>
      </div>
    </div>
        <!-- 右侧内容 #end -->
      
  </div>
    <!-- footer -->
</template>
<script>
import "~/assets/css/hospital_personal.css";
import "~/assets/css/hospital.css";
import orderInfoApi from "@/api/orderInfo";
import patientApi from "@/api/patient";
import dictApi from "@/api/dict";
import {getParam, getToken} from "@/utils/auth";
export default {
  data() {
    return {
      list: [], // banner列表
      total: 0, // database中的总记录数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        payState: null,
      },
      patientList: [],
      payStatusList: [],
    };
  },
  created() {
    this.orderId = this.$route.query.orderId;
    this.queryParams.userId = getParam("userId");
    //查询医院等级列表
    dictApi.getDictByType("pay_state").then(response => {
      this.payStatusList = []
      this.payStatusList.push({"dictLabel": "全部", "dictValue": ""})
      for (let i in response.data) {
        this.payStatusList.push(response.data[i]);
      }
    })
    this.fetchData();
  },
  methods: {
    fetchData(page = 1) {
      this.queryParams.pageNum = page;
      orderInfoApi.getList(this.queryParams)
        .then((response) => {
          console.log(response.data);
          this.list = response.rows;
          this.total = response.total;
        });
    },
    getPayState(item){
      for (let i in this.payStatusList) {
        if(item == this.payStatusList[i].dictValue){
          return this.payStatusList[i].dictLabel;
        }
      }
    },

    show(id) {
      window.location.href = "/order/show?orderId=" + id;
    },
  },
};
</script>
