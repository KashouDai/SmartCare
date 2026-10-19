<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="150px">
      <el-form-item label="医院名称" prop="hospitalName">
        <el-input
          v-model="queryParams.hospitalName"
          placeholder="请输入医院名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="医院编号" prop="hospitalCode">
        <el-input
          v-model="queryParams.hospitalCode"
          placeholder="请输入医院编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="联系人" prop="contactsName">
        <el-input
          v-model="queryParams.contactsName"
          placeholder="请输入联系人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="联系人手机" prop="contactsPhone">
        <el-input
          v-model="queryParams.contactsPhone"
          placeholder="请输入联系人手机"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="等级" prop="level">
        <el-select v-model="queryParams.level" placeholder="请选择等级" clearable>
          <el-option
            v-for="dict in dict.type.hospital_level"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option
            v-for="dict in dict.type.hospital_status"
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
          v-hasPermi="['info:hospital:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['info:hospital:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['info:hospital:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['info:hospital:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="hospitalList" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55"/>
      <el-table-column align="center" label="医院名称" prop="hospitalName"/>
      <el-table-column align="center" label="医院编号" prop="hospitalCode"/>
      <el-table-column align="center" label="放号时间" prop="releaseTime"/>
      <el-table-column align="center" label="停挂时间" prop="suspendTime"/>
      <el-table-column align="center" label="可预约天数" prop="appointmentDay"/>
      <el-table-column align="center" label="联系人" prop="contactsName"/>
      <el-table-column align="center" label="联系人手机" prop="contactsPhone"/>
      <el-table-column label="等级" align="center" prop="level">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.hospital_level" :value="scope.row.level"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.hospital_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['info:hospital:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['info:hospital:remove']"
          >删除</el-button>
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

    <!-- 添加或修改医院管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="医院名称" prop="hospitalName">
          <el-input v-model="form.hospitalName" placeholder="请输入医院名称"/>
        </el-form-item>
        <el-form-item label="医院编号" prop="hospitalCode">
          <el-input v-model="form.hospitalCode" placeholder="请输入医院编号"/>
        </el-form-item>
        <el-form-item label="放号时间" prop="releaseTime">
          <el-time-picker
            v-model="form.releaseTime"
            :picker-options="{
              selectableRange: '05:00:00 - 09:00:00'
            }"
            format='HH:mm'
            placeholder="请选择放号时间"
            value-format="HH:mm">
          </el-time-picker>
        </el-form-item>
        <el-form-item label="停挂时间" prop="suspendTime">
          <el-time-picker
            v-model="form.suspendTime"
            :picker-options="{
              selectableRange: '08:00:00 - 23:59:00'
            }"
            format='HH:mm'
            placeholder="请选择停挂时间"
            value-format="HH:mm">
          </el-time-picker>
        </el-form-item>
        <el-form-item label="可预约天数" prop="appointmentDay">
          <el-input-number v-model="form.appointmentDay" placeholder="请输入可预约天数" :min="1"/>
        </el-form-item>
        <el-form-item label="联系人" prop="contactsName">
          <el-input v-model="form.contactsName" placeholder="请输入联系人"/>
        </el-form-item>
        <el-form-item label="联系人手机" prop="contactsPhone">
          <el-input v-model="form.contactsPhone" placeholder="请输入联系人手机"/>
        </el-form-item>
        <el-form-item label="省份" prop="pcode">
          <el-select
            v-model="form.pcode"
            placeholder="请选择省份" @change="provinceChanged">
            <el-option
              v-for="province in provinceList"
              :key="province.pcode"
              :label="province.pname"
              :value="province.pcode"/>
          </el-select>
        </el-form-item>
        <el-form-item label="城市" prop="ccode">
          <el-select
            v-model="form.ccode"
            placeholder="请选择城市" @change="cityChanged">
            <el-option
              v-for="city in cityList"
              :key="city.ccode"
              :label="city.cname"
              :value="city.ccode"/>
          </el-select>
        </el-form-item>
        <el-form-item label="区县" prop="tcode">
          <el-select
            v-model="form.tcode"
            placeholder="请选择区县">
            <el-option
              v-for="town in townList"
              :key="town.ttode"
              :label="town.tname"
              :value="town.tcode"/>
          </el-select>
        </el-form-item>
        <el-form-item label="等级" prop="level">
          <el-select
            v-model="form.level"
            placeholder="请选择等级">
            <el-option
              v-for="dict in dict.type.hospital_level"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.hospital_status"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {addHospital, delHospital, getHospital, listHospital, updateHospital} from "@/api/info/hospital";
import {listCity, listProvince, listTown} from "@/api/info/city";

export default {
  name: "Hospital",
  dicts: ['hospital_status', 'hospital_level'],
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
      // 医院管理表格数据
      hospitalList: [],
      // 省份列表
      provinceList:[],
      // 城市列表
      cityList:[],
      // 区县列表
      townList:[],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        hospitalName: null,
        hospitalCode: null,
        releaseTime: null,
        suspendTime: null,
        contactsName: null,
        appointmentDay: null,
        contactsPhone: null,
        status: null,
        level: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        hospitalName: [
          {required: true, message: "医院名称不能为空", trigger: "blur"}
        ],
        hospitalCode: [
          {required: true, message: "医院编号不能为空", trigger: "blur"}
        ],
        releaseTime: [
          {required: true, message: "放号时间不能为空", trigger: "blur"}
        ],
        suspendTime: [
          {required: true, message: "停挂时间不能为空", trigger: "blur"}
        ],
        appointmentDay: [
          {required: true, message: "可预约天数不能为空", trigger: "blur"}
        ],
        contactsName: [
          {required: true, message: "联系人不能为空", trigger: "blur"}
        ],
        contactsPhone: [
          {required: true, message: "联系人手机不能为空", trigger: "blur"}
        ],
        pcode: [
          {required: true, message: "省份不能为空", trigger: "blur"}
        ],
        ccode: [
          { required: true, message: "城市不能为空", trigger: "blur" }
        ],
        tcode: [
          { required: true, message: "区县不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "blur" }
        ],
        level: [
          { required: true, message: "等级不能为空", trigger: "blur" }
        ],

      }
    };
  },
  created() {
    this.getList();
    this.selectProvinceList();

  },
  methods: {
    /** 查询医院管理列表 */
    getList() {
      this.loading = true;
      listHospital(this.queryParams).then(response => {
        this.hospitalList = response.rows;
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
        id: null,
        hospitalName: null,
        hospitalCode: null,
        contactsName: null,
        appointmentDay: null,
        contactsPhone: null,
        status: null,
        delFlag: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      };
      this.resetForm("form");
    },
    // 查询省下拉列表
    selectProvinceList(){
      listProvince().then(response=>{
        this.provinceList = response.data;
      })
    },
    // 选择省份后查询市下拉数据
    provinceChanged(){
      listCity(this.form.pcode).then(response=>{
        this.cityList = response.data;
      })
    },
    // 选择省份后查询市下拉数据
    cityChanged(){
      listTown(this.form.ccode).then(response=>{
        this.townList = response.data;
      })
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
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加医院管理";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getHospital(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改医院管理";
        this.provinceChanged();
        this.cityChanged();
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateHospital(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addHospital(this.form).then(response => {
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
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除医院管理编号为"' + ids + '"的数据项？').then(function() {
        return delHospital(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('info/hospital/export', {
        ...this.queryParams
      }, `hospital_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
