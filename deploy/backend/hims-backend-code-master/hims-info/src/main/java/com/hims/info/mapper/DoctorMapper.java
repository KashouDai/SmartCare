package com.hims.info.mapper;

import com.hims.common.core.domain.entity.SysUser;
import com.hims.info.domain.DoctorVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface DoctorMapper {

    /**
     * 查询用户列表
     */
    List<DoctorVo> selectUserList(DoctorVo user);

    /**
     *按照名字查询用户
     */
    SysUser selectUserByUserName(String userName);

    /**
     *按照ID查询用户
     */
    DoctorVo selectUserById(Long userId);

    /**
     *新增医院医生
     */
    Integer insertDocHosp(@Param("data") SysUser user);

    /**
     *修改医院医生
     */
    Integer updateDocHosp(DoctorVo doctorVo);

    /**
     *更新医院医生
     */
    int update(DoctorVo doctorVo);
}
