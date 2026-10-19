<template>
      

    <div class="header-container">
          <!-- 登录弹出层 -->
          
      <el-dialog
        :append-to-body="true"
        :visible.sync="dialogUserFromLogin"
        style="text-align: left"
        title="用户登录"
        top="50px"
        width="500px">
        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" label-width="100px">
          <el-form-item label="用户名" prop="username" >
            <el-input v-model="loginForm.username" placeholder="请输入用户名" id="username"></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password" >
            <el-input v-model="loginForm.password" placeholder="请输入密码" show-password id="password"></el-input>
          </el-form-item>

          <el-form-item>
            <el-button size="medium" type="primary" @click="login">登录</el-button>
            <el-button size="medium" type="info">取消</el-button>
            <el-button size="medium" style="float:right;" type="text" @click="showRegister()">注册</el-button>
            <el-button size="medium" style="float:right;" type="text" @click="showResetPassword()">忘记密码</el-button>
          </el-form-item>
        </el-form>
            
      </el-dialog>

      <el-dialog
        :append-to-body="true"
        :visible.sync="dialogUserFromRegister"
        style="text-align: left"
        title="用户注册"
        top="50px"
        width="500px">
        <el-form ref="registerForm" :model="registerForm" :rules="registerRules" label-width="100px">
          <el-form-item label="用户名" prop="userName">
            <el-input v-model="registerForm.userName" placeholder="请输入用户名" id="userName"></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password" >
            <el-input v-model="registerForm.password" placeholder="请输入密码" show-password id="rePassword"></el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" placeholder="请再次输入密码" show-password  id="confirmPassword"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button size="medium" type="primary" @click="register">注册</el-button>
            <el-button size="medium" type="info">取消</el-button>
          </el-form-item>
        </el-form>
            
      </el-dialog>

      <el-dialog
        :append-to-body="true"
        :visible.sync="dialogUserFromResetPassword"
        style="text-align: left"
        title="找回密码"
        top="50px"
        width="500px">
        <el-form ref="registerForm" :model="resetPasswordForm" :rules="resetPasswordRules" label-width="100px">
          <el-form-item label="用户名" prop="userName">
            <el-input v-model="resetPasswordForm.userName" placeholder="请输入用户名" id="userName"></el-input>
          </el-form-item>
          <el-form-item label="手机号" prop="phonenumber">
            <el-input v-model="resetPasswordForm.phonenumber" placeholder="请输入手机号码" id="phonenumber"></el-input>
          </el-form-item>
          <el-form-item label="新密码" prop="password" >
            <el-input v-model="resetPasswordForm.password" placeholder="请输入新密码" show-password id="rePassword"></el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="resetPasswordForm.confirmPassword" placeholder="请再次输入密码" show-password  id="confirmPassword"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button size="medium" type="primary" @click="resetPassword">重置密码</el-button>
            <el-button size="medium" type="info">取消</el-button>
          </el-form-item>
        </el-form>
            
      </el-dialog>

      <div class="wrapper">
                <!-- logo -->           
        <div class="left-wrapper v-link selected">
          <img height="50" src="~assets/images/logo.png" style="width: 50px" width="50"/>
          <span class="text" onclick="javascript:window.location='/'">预约挂号平台</span>
        </div>
                <!-- 右侧 -->
        <div class="right-wrapper">
          <span class="v-link clickable">帮助中心</span>

          <span v-if="name == ''" class="v-link clickable" @click="showLogin()" id="loginDialog">登录/注册</span>

          <el-dropdown v-if="name != ''" @command="loginMenu">
              <span class="el-dropdown-link">
                {{ name }}<i class="el-icon-arrow-down el-icon--right"></i>
              </span>
            <el-dropdown-menu class="user-name-wrapper" slot="dropdown">
              <el-dropdown-item command="/user">实名认证</el-dropdown-item>
              <el-dropdown-item command="/order">挂号订单</el-dropdown-item>
<!--              <el-dropdown-item command="/patient">就诊人管理</el-dropdown-item>-->
              <el-dropdown-item command="/logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </div>
</template>

<script>
import { getCodeImg } from "@/api/userInfo";
import userInfoApi from "@/api/userInfo";
import {getParam, getToken, removeToken, setOther, setToken, } from '@/utils/auth'
import Vue from 'vue'

export default {
  data() {
    // 校验注册两次密码是否一致
    let checkPasswdRe = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入密码'))
      } else if (value !== this.registerForm.password) {
        callback(new Error('两次输入密码不一致!'))
      } else {
        callback()
      }
    };
    let checkPasswdRe2 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入密码'))
      } else if (value !== this.resetPasswordForm.password) {
        callback(new Error('两次输入密码不一致!'))
      } else {
        callback()
      }
    };
    return {
      name: "", // 用户登录显示的名称
      loginForm: {
        username: '',
        password: '',
      },
      registerForm: {
        userName: '',
        password: '',
        confirmPassword: '',
        phone: '',
        sex: '',
        idCard: '',
        nickName: '',
      },
      resetPasswordForm:{
        userName:'',
        password: '',
        confirmPassword: '',
        phonenumber:''
      },

      dialogUserFromLogin: false, // 弹出层相关属性
      dialogUserFromRegister: false, // 弹出层相关属性
      dialogUserFromResetPassword: false, // 弹出层相关属性

      loginRules: {
        username: [
          {required: true, message: '用户名不能为空', trigger: 'blur'}
        ],
        password: [
          {required: true, message: '密码不能为空', trigger: 'blur'}
        ],
      },
      registerRules: {
        userName: [
          {required: true, message: '用户名不能为空', trigger: 'blur'},
          {pattern: /^(?=.*[a-zA-Z])(?=.*\d).{2,20}$/, message: '用户名长度至少2位，并包含字母、数字', trigger: 'blur'},
        ],
        password: [
          {required: true, message: '密码不能为空', trigger: 'blur'},
          {
            pattern: /^(?![a-zA-Z]+$)(?!\d+$)(?![^\da-zA-Z\s]+$).{5,20}$/,
            message: '密码长度至少5位，并由字母、数字、特殊字符组成',
            trigger:
              'blur'
          },
        ],
        confirmPassword: [
          {required: true, message: '密码不能为空', trigger: 'blur'},
          {validator: checkPasswdRe, trigger: 'blur'}
        ],
      },
      resetPasswordRules: {
        userName: [
          {required: true, message: '用户名不能为空', trigger: 'blur'},
        ],
        phonenumber: [
          {required: true, message: '手机号不能为空',trigger: 'blur'},
          {
            pattern: /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/,
            message: '手机号码格式不正确',
            trigger:
              'blur'
          }
        ],
        password: [
          {required: true, message: '密码不能为空', trigger: 'blur'},
          {
            pattern: /^(?![a-zA-Z]+$)(?!\d+$)(?![^\da-zA-Z\s]+$).{5,20}$/,
            message: '密码长度至少5位，并由字母、数字、特殊字符组成',
            trigger:
              'blur'
          },
        ],
        confirmPassword: [
          {required: true, message: '密码不能为空', trigger: 'blur'},
          {validator: checkPasswdRe2, trigger: 'blur'}
        ]
      },
    };
  },
  created() {
    this.showInfo();
  },
  mounted() {
    // 注册全局登录事件对象
    window.loginEvent = new Vue();
    // 监听登录事件
    loginEvent.$on('loginDialogEvent', function () {
      document.getElementById("loginDialog").click();
    })
    // 触发事件，显示登录层：loginEvent.$emit('loginDialogEvent')
  },
  methods: {
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userInfoApi.logout().then((response) => {
          removeToken();
          location.href = '/';
        })
      }).catch(() => {
      });
    },
    showInfo() {
      let token = getToken();
      if(token){
        this.name = getParam("name")
      }
    },

    //点击显示登录层
    showLogin() {
      this.dialogUserFromLogin = true; // 初始化登录层相关参数
    },
    // 点击显示注册层
    showRegister() {
      this.dialogUserFromLogin = false; // 关闭登录框
      this.dialogUserFromRegister = true; // 打开注册框
    },

    // 点击显示注册层
    showResetPassword() {
      this.dialogUserFromLogin = false; // 关闭登录框
      this.dialogUserFromResetPassword = true; // 打开忘记密码框
    },

    // 登录
    login() {
      userInfoApi.login(this.loginForm).then((response) => {
        if (response.code === 200) {
          setToken(response.token)
          setOther(response)
          this.$message.success("登录成功");
          this.dialogUserFromLogin = false; // 关闭登录框
          this.name = response.sub;
        } else
        {
          this.$message.error(response.msg);
        }
      })
    },

    // 注册
    register() {
      userInfoApi.register(this.registerForm).then((response) => {
        // 成功
        if (response.code === 200) {
          this.$message.success("注册成功");
          this.dialogUserFromRegister = false; // 关闭注册框
        } else {
          // 失败
          this.$message.error(response.msg)
        }
      })
    },
    loginMenu(command) {
      if('/logout' == command) {

        this.logout();
      } else {
        window.location.href = command
      }
    },
    resetPassword(){
     userInfoApi.resetPassword(this.resetPasswordForm).then((response)=>{
       // 成功
       if (response.code === 200) {
         this.$message.success("重置成功");
         this.dialogUserFromResetPassword = false; // 关闭找回密码框
       } else {
         // 失败
         this.$message.error(response.msg)
       }
     })
    }
  },
};
</script>
