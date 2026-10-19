<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :inline="true" :model="queryParams" label-width="68px" size="small">
      <el-form-item label="医生姓名" prop="doctorName">
        <el-input
          v-model="queryParams.doctorName"
          clearable
          placeholder="请输入医生姓名"
          @keyup.enter.native="handleQuery">
        </el-input>
      </el-form-item>
      <el-form-item label="所属医院" prop="hospitalId">
        <el-select v-model="queryParams.hospitalId" clearable placeholder="请选择所属医院">
          <el-option
            v-for="hospital in hospitalList"
            :key="hospital.id"
            :label="hospital.hospitalName"
            :value="hospital.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属科室" prop="deptId">
        <el-select v-model="queryParams.deptId" clearable placeholder="请选择所属科室">
          <el-option
            v-for="dept in deptList"
            :key="dept.deptId"
            :label="dept.deptName"
            :value="dept.deptId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="值班时间" prop="dutyDate">
        <el-date-picker v-model="queryParams.dutyDate"
                        clearable
                        placeholder="请选择值班时间"
                        type="date"
                        value-format="yyyy-MM-dd">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="值班时段" prop="dutyPeriod">
        <el-select v-model="queryParams.dutyPeriod" clearable placeholder="请选择值班时段">
          <el-option
            v-for="dict in dict.type.duty_period"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="el-icon-search" size="mini" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['info:duty:add']"
          icon="el-icon-plus"
          plain
          size="mini"
          type="primary"
          @click="handleAdd"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['info:duty:edit']"
          :disabled="single"
          icon="el-icon-edit"
          plain
          size="mini"
          type="success"
          @click="handleUpdate"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['info:duty:remove']"
          :disabled="multiple"
          icon="el-icon-delete"
          plain
          size="mini"
          type="danger"
          @click="handleDelete"
        >删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['info:duty:export']"
          icon="el-icon-download"
          plain
          size="mini"
          type="warning"
          @click="handleExport"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dutyList" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55"/>
      <el-table-column align="center" label="医生姓名" prop="doctorName"/>
      <el-table-column align="center" label="所属医院" prop="hospitalName"/>
      <el-table-column align="center" label="所属科室" prop="deptName"/>
      <el-table-column align="center" label="值班时间" prop="dutyDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.dutyDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="值班时段" prop="dutyPeriod">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.duty_period" :value="scope.row.dutyPeriod"/>
        </template>
      </el-table-column>
      <el-table-column align="center" label="可预约数" prop="appointmentsNum"/>
      <el-table-column align="center" label="已预约数" prop="appointmentsNumMade"/>
      <el-table-column align="center" label="挂号费" prop="registrationFee"/>
      <el-table-column align="center" label="备注" prop="remark"/>
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
        <template slot-scope="scope">
          <el-button
            v-hasPermi="['info:duty:edit']"
            icon="el-icon-edit"
            size="mini"
            type="text"
            @click="handleUpdate(scope.row)"
          >修改
          </el-button>
          <el-button
            v-hasPermi="['info:duty:remove']"
            icon="el-icon-delete"
            size="mini"
            type="text"
            @click="handleDelete(scope.row)"
          >删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :limit.sync="queryParams.pageSize"
      :page.sync="queryParams.pageNum"
      :total="total"
      @pagination="getList"
    />

    <!-- 添加或修改排班信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" append-to-body width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="医生名字" prop="doctorName">
          <el-input placeholder="请选择医生" v-model="form.doctorName" disabled>
            <el-button slot="append" @click="getDoctorId" icon="el-icon-search"></el-button>
          </el-input>
        </el-form-item>
        <el-form-item label="所属医院" prop="hospitalName">
          <el-input v-model="form.hospitalName" disabled/>
        </el-form-item>
        <el-form-item label="所属科室" prop="deptName">
          <el-input v-model="form.deptName" disabled/>
        </el-form-item>
        <el-form-item label="值班时间" prop="dutyDate">
          <el-date-picker v-model="form.dutyDate"
                          clearable
                          placeholder="请选择值班时间"
                          type="date"
                          value-format="yyyy-MM-dd">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="值班时段" prop="dutyPeriod">
          <el-select v-model="form.dutyPeriod" placeholder="请选择值班时段">
            <el-option
              v-for="dict in dict.type.duty_period"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="可预约数" prop="appointmentsNum">
          <el-input-number v-model="form.appointmentsNum" placeholder="请输入可预约数" :min="1"/>
        </el-form-item>
        <el-form-item label="挂号费" prop="registrationFee">
          <el-input-number v-model="form.registrationFee" placeholder="请输入挂号费" :precision="2" :step="0.1" :min="0"/>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <doctorSelect :isShow="doctorSelectShow" @doctorSelectSumbit="doctorSelectSumbit"
                  @hideModal="hideDoctorSelectModal"></doctorSelect>
  </div>
</template>

<script>
import {addDuty, delDuty, getDuty, listDuty, updateDuty} from "@/api/info/duty";
import doctorSelect from '@/views/components/doctorSelect'
import {getHospitalList} from "@/api/info/hospital";
import {getDeptListNotParent} from "@/api/info/dept";

export default {
  name: "Duty",
  dicts: ['duty_period'],
  components: {doctorSelect},
  data() {
    return {
      doctorSelectShow: false, //关联办班id组件显示
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
      // 排班信息表格数据
      dutyList: [],
      hospitalList: [],
      deptList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        dutyDate: null,
        dutyPeriod: null,
        doctorName: null,
        hospitalId: null,
        deptId: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        doctorName: [
          { required: true, message: "医生姓名不能为空", trigger: "blur" }
        ],
        dutyDate: [
          { required: true, message: "值班时间不能为空", trigger: "blur" }
        ],
        dutyPeriod: [
          { required: true, message: "值班时段不能为空", trigger: "blur" }
        ],
        appointmentsNum: [
          { required: true, message: "可预约数不能为空", trigger: "blur" }
        ],
        registrationFee: [
          { required: true, message: "挂号费", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
    this.getHospitalList();
    this.getDeptList();
  },
  methods: {
    /** 查询排班信息列表 */
    getList() {
      this.loading = true;
      listDuty(this.queryParams).then(response => {
        this.dutyList = response.rows;
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
        scheduleId: null,
        doctorId: null,
        doctorName: null,
        deptId: null,
        dutyDate: null,
        dutyPeriod: null,
        appointmentsNum: null,
        appointmentsNumMade: null,
        registrationFee: null,
        delFlag: null,
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
      this.ids = selection.map(item => item.scheduleId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加排班信息";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const scheduleId = row.scheduleId || this.ids
      getDuty(scheduleId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改排班信息";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.scheduleId != null) {
            updateDuty(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addDuty(this.form).then(response => {
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
      const scheduleIds = row.scheduleId || this.ids;
      this.$modal.confirm('是否确认删除排班信息编号为"' + scheduleIds + '"的数据项？').then(function () {
        return delDuty(scheduleIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('info/duty/export', {
        ...this.queryParams
      }, `duty_${new Date().getTime()}.xlsx`)
    },
    doctorSelectSumbit(obj) {
      if (obj) {
        this.form.doctorId = obj.userId
        this.form.doctorName = obj.nickName
        this.form.hospitalName = obj.hospitalName
        this.form.deptName = obj.dept
      }
      this.doctorSelectShow = false;
    },
    hideDoctorSelectModal() {
      this.doctorSelectShow = false;
    },
    getDoctorId() {
      this.doctorSelectShow = true;
    },
    // 查询医院下拉列表
    getHospitalList() {
      getHospitalList().then(response => {
        this.hospitalList = response.data;
      })
    },

    // 查询医院下拉列表
    getDeptList() {
      getDeptListNotParent().then(response => {
        this.deptList = response.data;
      })
    },
  }
};
</script>
