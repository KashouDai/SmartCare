<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="患者姓名" prop="nickName">
        <el-input
          v-model="queryParams.nickName"
          placeholder="请输入患者姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="患者性别" prop="sex">
        <el-select v-model="queryParams.sex" placeholder="请选择患者性别" clearable>
          <el-option
            v-for="dict in dict.type.sys_user_sex"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['info:info:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['info:info:edit']"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['info:info:remove']"
        >删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['info:info:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="infoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <!--      <el-table-column label="患者id" align="center" prop="patientId" />-->
      <el-table-column label="就诊卡号" align="center" prop="cardNo"/>
      <el-table-column label="患者姓名" align="center" prop="nickName"/>
      <el-table-column label="患者性别" align="center" prop="sex">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_user_sex" :value="scope.row.sex"/>
        </template>
      </el-table-column>
      <el-table-column label="手机号码" align="center" prop="phone"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['info:patientsInfo:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['info:patientsInfo:remove']"
          >删除
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-tickets"
            @click="handleHistory(scope.row.cardNo)"
            v-hasPermi="['info:patientsInfo:remove']"
          >就诊记录
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改患者信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="就诊卡号" prop="cardNo">
                  <el-input v-model="form.cardNo" placeholder="请输入就诊卡号" disabled/>
                </el-form-item>
        <el-form-item label="患者姓名" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入患者姓名"/>
        </el-form-item>
        <el-form-item label="患者性别" prop="sex">
          <el-select v-model="form.sex" placeholder="请选择患者性别">
            <el-option
              v-for="dict in dict.type.sys_user_sex"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号码"/>
        </el-form-item>
        <!--        <el-form-item label="删除标志" prop="delFlag">-->
        <!--          <el-input v-model="form.delFlag" placeholder="请输入删除标志" />-->
        <!--        </el-form-item>-->
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 添加或修改患者信息对话框 -->
    <el-dialog :title="historyDataTitle" :visible.sync="historyData" width="1000px" append-to-body>
      <el-table v-loading="loading" :data="historyList">
        <el-table-column label="医生姓名" align="center" prop="doctorName"/>
        <el-table-column label="诊断结果" align="center" prop="diagnosis"/>
        <el-table-column label="就诊日期" align="center" prop="visitDate"/>
        <el-table-column label="备注" align="center" prop="remark"/>
      </el-table>
      <pagination
        v-show="historyTotal>0"
        :total="historyTotal"
        :page.sync="queryParams2.pageNum"
        :limit.sync="queryParams2.pageSize"
        @pagination="handleHistory2"
      />
    </el-dialog>
  </div>
</template>

<script>
import {addInfo, delInfo, getHistoryInfo, getInfo, listInfo, updateInfo} from "@/api/info/patientsInfo";

export default {
  name: "Info",
  dicts: ['sys_user_sex'],
  data() {
    return {
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
      // 总条数
      total: 0,
      historyTotal: 0,
      // 患者信息表格数据
      infoList: [],
      // 患者就诊历史表格数据
      historyList: [],
      // 弹出层标题
      title: "",
      historyDataTitle: "",
      // 是否显示弹出层
      open: false,
      historyData: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        nickName: null,
        sex: null,
        cardNo: null
      },
      queryParams2: {
        pageNum: 1,
        pageSize: 10,
        cardNo: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        nickName: [
          {required: true, message: "患者名称不能为空", trigger: "blur"}
        ],
        sex: [
          {required: true, message: "患者性别不能为空", trigger: "blur"}
        ],
        phone: [
          {required: true, message: "手机号码不能为空", trigger: "blur"},
          {
            pattern: /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/
            , message: "手机号码格式不正确", trigger: "blur"
          }
        ],
      },
      rules2: {},
      myCardNo: null
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询患者信息列表 */
    getList() {
      this.loading = true;
      listInfo(this.queryParams).then(response => {
        this.infoList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        patientId: null,
        cardNo: null,
        name: null,
        gender: null,
        number: null,
        delFlag: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
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
      this.ids = selection.map(item => item.patientId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加患者信息";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const patientId = row.patientId || this.ids
      getInfo(patientId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改患者信息";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.patientId != null) {
            updateInfo(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addInfo(this.form).then(response => {
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
      const patientIds = row.patientId || this.ids;
      this.$modal.confirm('是否确认删除患者信息编号为"' + patientIds + '"的数据项？').then(function () {
        return delInfo(patientIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {
      });
    },
    /** 打开就诊历史 */
    handleHistory2(){
      this.handleHistory(this.myCardNo)
    },
    handleHistory(cardNo) {
      this.loading = true;
      this.queryParams2.cardNo = cardNo;
      this.myCardNo = cardNo;
      getHistoryInfo(this.queryParams2).then(response => {
        this.historyList = response.rows;
        this.historyTotal =response.total;
        this.historyData = true;
        this.historyDataTitle = "患者就诊历史数据";
        this.loading = false;
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('info/info/export', {
        ...this.queryParams
      }, `info_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
