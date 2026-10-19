<template>
  <el-dialog
    :before-close="dialogVisibleCloseBefore"
    :visible.sync="isShow"
    title="选择医生"
    width="960px"
  >
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="医生名称" prop="nickName">
        <el-input
          v-model="queryParams.nickName"
          placeholder="请输入医生名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属医院" prop="hospitalId">
        <el-select v-model="queryParams.hospitalId" placeholder="请选择所属医院" clearable>
          <el-option
            v-for="hospital in hospitalList"
            :key="hospital.id"
            :label="hospital.hospitalName"
            :value="hospital.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>


    <el-table v-loading="loading" :data="doctorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="医生名称" align="center" prop="nickName" />
      <el-table-column label="所属医院" align="center" prop="hospitalName" />
      <el-table-column label="科室名称" align="center" prop="dept" />
      <el-table-column label="手机号码" align="center" prop="phonenumber" />
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

  </div>
    <span slot="footer" class="dialog-footer">
    <el-button @click="dialogVisibleCloseBefore">取 消</el-button>
    <el-button type="primary" @click="submitOK">确 定</el-button>
  </span>
  </el-dialog>
</template>

<script>
import { listDoctor, getDoctor, delDoctor, addDoctor, updateDoctor ,deptTreeSelect} from "@/api/info/doctor";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import {getHospitalList} from "@/api/info/hospital";
export default {
  props: {
    customParams:{
      type: Object,
      default(){
        return {}
      }
    },
    isShow: {
      type: Boolean,
      default: false
    },

  },
  watch:{
    isShow(){
      if(this.isShow){
        this.resetQuery();
      }

    }
  },
  name: "Doctor",
  dicts: ['account_status', 'sys_user_sex'],
  components: { Treeselect },
  data() {
    return {
      //多选选中的数据
      selectMulRow: [],
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 科室树选项
      deptOptions: undefined,
      // 医院下拉列表
      hospitalList:[],
      // 总条数
      total: 0,
      // 医生信息管理表格数据
      doctorList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        nickName: null,
        hospitalId:null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {}
    };
  },
  created() {
    this.getList();
    this.getDeptTree();
    this.getHospitalList();
  },
  methods: {
    /** 查询医生信息管理列表 */
    getList() {
      this.loading = true;
      listDoctor(this.queryParams).then(response => {
        this.doctorList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 查询医院下拉列表
    getHospitalList() {
      getHospitalList().then(response => {
        this.hospitalList = response.data;
      })
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        userId: null,
        deptId: null,
        userName: null,
        nickName: null,
        userType: null,
        email: null,
        phonenumber: null,
        sex: null,
        avatar: null,
        password: null,
        status: null,
        delFlag: null,
        loginIp: null,
        loginDate: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.selectMulRow = selection;
      this.ids = selection.map(item => item.userId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加医生信息管理";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const userId = row.userId || this.ids
      getDoctor(userId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改医生信息管理";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.userId != null) {
            updateDoctor(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addDoctor(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const userIds = row.userId || this.ids;
      this.$modal.confirm('是否确认删除医生信息管理编号为"' + userIds + '"的数据项？').then(function() {
        return delDoctor(userIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('info/doctor/export', {
        ...this.queryParams
      }, `doctor_${new Date().getTime()}.xlsx`)
    },
    /** 查询科室下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data;
      });
    },
    dialogVisibleCloseBefore(done) {
      this.$emit("hideModal")
    },
    submitOK() {
      this.$emit("doctorSelectSumbit", this.selectMulRow[0])

    },
  }
};
</script>
