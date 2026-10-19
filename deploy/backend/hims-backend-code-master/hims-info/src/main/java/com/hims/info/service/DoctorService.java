package com.hims.info.service;

import com.hims.info.domain.DoctorVo;
import com.hims.common.core.domain.entity.SysUser;

import java.util.List;

/**
 * 用户 业务层
 */
public interface DoctorService {

    List<DoctorVo> selectUserList(DoctorVo user);

    /**
     *校验是否存在
     */
    void check(SysUser user);

    int insertUser(SysUser user);

    int deleteUserByIds(Long[] userIds);

    DoctorVo selectUserById(Long userId);

    int updateUser(DoctorVo doctorVo);
}
