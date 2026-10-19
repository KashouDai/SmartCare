<template>
  <!-- header -->
  <div class="nav-container page-component">
    <!--左侧导航 #start -->
    <div class="nav left-nav">
      <div class="nav-item selected">
        <span
          class="v-link selected dark"
          onclick="javascript:window.location='/user'"
          >实名认证
        </span>
      </div>
      <div class="nav-item">
        <span
          class="v-link clickable dark"
          onclick="javascript:window.location='/order'"
        >
          挂号订单
        </span>
      </div>
<!--      <div class="nav-item">-->
<!--        <span-->
<!--          class="v-link clickable dark"-->
<!--          onclick="javascript:window.location='/patient'"-->
<!--        >-->
<!--          就诊人管理-->
<!--        </span>-->
<!--      </div>-->
      <!-- <div class="nav-item">
        <span class="v-link clickable dark"> 修改账号信息 </span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark"> 意见反馈 </span>
      </div> -->
    </div>
    <!-- 左侧导航 #end -->

    <!-- 右侧内容 #start -->
    <div class="page-container">
      <div>
        <div class="title">实名认证</div>
        <div class="status-bar">
          <div class="status-wrapper">
            <!-- <span class="iconfont"></span>{{ userInfo.param.authStatusString }} -->
            <span class="iconfont"></span>
            完成实名认证后才能正常进行挂号，为了不影响后续步骤，建议提前实名认证。
          </div>
        </div>
        <!-- <div class="tips">
          <span class="iconfont"></span>
          完成实名认证后才能添加就诊人，正常进行挂号，为了不影响后续步骤，建议提前实名认证。
        </div> -->
        <div v-if="userInfo.idCard == null" class="form-wrapper">
          <div>
            <el-form
              :model="userAuth"
              :rules="userAuthRule"
              label-width="110px"
              label-position="left"
            >
              <el-form-item class="form-normal" label="姓名：" prop="nickName">
                <div class="name-input">
                  <el-input
                    v-model="userAuth.nickName"
                    placeholder="请输入姓名"
                    class="input v-input"
                  />
                </div>
              </el-form-item>
              <el-form-item label="证件类型：" prop="documentType">
                <el-select
                  v-model="userAuth.documentType"
                  placeholder="请选择证件类型"
                  class="v-select patient-select"
                >
                  <el-option
                    v-for="item in documentTypeList"
                    :key="item.dictValue"
                    :label="item.dictLabel"
                    :value="item.dictValue"
                  >
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="证件号码：" prop="idCard">
                <el-input
                  v-model="userAuth.idCard"
                  placeholder="请输入证件号码"
                  class="input v-input"
                />
              </el-form-item>
              <el-form-item label="性别：" prop="sex">
                <el-select
                  v-model="userAuth.sex"
                  class="v-select patient-select"
                  placeholder="请选择性别类型"
                >
                  <el-option
                    v-for="item in sexList"
                    :key="item.dictValue"
                    :label="item.dictLabel"
                    :value="item.dictValue"
                  >
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="手机号码：" prop="phonenumber">
                <el-input
                  v-model="userAuth.phonenumber"
                  class="input v-input"
                  placeholder="请输入手机号码"
                />
              </el-form-item>
            </el-form>

            <div class="bottom-wrapper">
              <div class="button-wrapper">
                <div class="v-button" @click="saveUserAuth()">
                  {{ submitBnt }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="userInfo.idCard != null" class="context-container">
          <div>
            <el-form label-width="110px" label-position="right">
              <el-form-item prop="name" label="姓名：" class="form-normal">
                <div class="name-input">
                  {{ userInfo.nickName }}
                </div>
              </el-form-item>
              <el-form-item prop="name" label="证件类型：">
                {{ getDocumentType(userInfo.documentType) }}
              </el-form-item>
              <el-form-item prop="name" label="证件号码：">
                {{ userInfo.idCard }}
              </el-form-item>
              <el-form-item prop="name" label="性别：">
                {{ getSex(userInfo.sex) }}
              </el-form-item>
              <el-form-item prop="name" label="手机号码：">
                {{ userInfo.phonenumber }}
              </el-form-item>
            </el-form>
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
import "~/assets/css/personal.css";

import dictApi from "@/api/dict";
import userInfoApi from "@/api/userInfo";
import patientsInfoApi from "@/api/patientsInfo";
import {getParam} from "@/utils/auth";


export default {
  data() {
    return {
      userAuth: {
        userId: "",
        nickName: "",
        documentType: "",
        idCard: "",
        sex: "",
        phonenumber: "",
      },
      patientsInfo: {
        patientId: "",
        nickName: "",
        sex: "",
        phone: "",
      },
      documentTypeList: [],
      sexList: [],

      userInfo: {
        param: {},
      },

      submitBnt: "提交",
      userAuthRule: {
        phonenumber: [
          {required: true, message: '手机号码不能为空', trigger: 'blur'},
          {pattern: /^[1][3,4,5,6,7,8,9][0-9]{9}$/, message: '手机号码格式不正确', trigger: 'blur'}
        ],
        sex: [
          {required: true, message: '性别不能为空', trigger: 'blur'},
        ],
        documentType: [
          {required: true, message: '证件类型不能为空', trigger: 'blur'},
        ],
        idCard: [
          {required: true, message: '身份证号码不能为空', trigger: 'blur'},
          {pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证号码格式不正确', trigger: 'blur'}
        ],
        nickName: [
          {required: true, message: '姓名不能为空', trigger: 'blur'},
        ],
      },
    };
  },

  created() {
    this.init();
  },

  methods: {
    init() {
      let userId = getParam("userId");
      userInfoApi.getUserInfoById(userId).then((response) => {
        this.userInfo = response.data;
      });

      dictApi.getDictByType("document_type").then(response => {
        this.documentTypeList = []
        for (let i in response.data) {
          this.documentTypeList.push(response.data[i]);
        }
      });

      dictApi.getDictByType("sys_user_sex").then(response => {
        this.sexList = []
        for (let i in response.data) {
          this.sexList.push(response.data[i]);
        }
      })
    },

    saveUserAuth() {
      this.userAuth.userId = this.userInfo.userId;
      if (this.submitBnt == "正在提交...") {
        this.$message.info("重复提交");
        return;
      }
      this.submitBnt = "正在提交...";
      userInfoApi.updateUserInfo(this.userAuth)
        .then((response) => {
          // 生成患者信息
          this.insertPatientsInfo();
        })
        .catch((e) => {
          this.submitBnt = "提交";
        });
    },

    insertPatientsInfo() {
      this.patientsInfo.patientId = getParam("userId");
      this.patientsInfo.nickName = this.userAuth.nickName;
      this.patientsInfo.sex = this.userAuth.sex;
      this.patientsInfo.phone = this.userAuth.phonenumber;
      patientsInfoApi.insertPatientsInfo(this.patientsInfo)
        .then((response) => {
          this.$message.success("提交成功");
          window.location.reload();
        })
    },

    getSex(item) {
      for (let i in this.sexList) {
        if (item == this.sexList[i].dictValue) {
          return this.sexList[i].dictLabel;
        }
      }
    },
    getDocumentType(item) {
      for (let i in this.documentTypeList) {
        if(item == this.documentTypeList[i].dictValue){
          return this.documentTypeList[i].dictLabel;
        }
      }
    },
  },
};
</script>
<style>
.header-wrapper .title {
  font-size: 16px;
  margin-top: 0;
}

.content-wrapper {
  margin-left: 0;
}

.patient-card .el-card__header .detail {
  font-size: 14px;
}

.page-container .title {
  letter-spacing: 1px;
  font-weight: 700;
  color: #333;
  font-size: 16px;
  margin-top: 0;
  margin-bottom: 20px;
}

.page-container .tips {
  width: 100%;
  padding-left: 0;
}

.page-container .form-wrapper {
  padding-left: 92px;
  width: 580px;
}

.form-normal {
  height: 40px;
}
.bottom-wrapper {
  width: 100%;
  padding: 0;
  margin-top: 0;
}
</style>
