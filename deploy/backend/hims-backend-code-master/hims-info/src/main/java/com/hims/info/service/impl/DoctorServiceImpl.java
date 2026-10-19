package com.hims.info.service.impl;

import com.hims.info.domain.DoctorVo;
import com.hims.info.mapper.DoctorMapper;
import com.hims.info.mapper.DutyInfoMapper;
import com.hims.info.service.DoctorService;
import com.hims.common.core.domain.entity.SysUser;
import com.hims.common.exception.ServiceException;
import com.hims.common.core.redis.BasicInfoCache;
import com.hims.system.service.impl.SysUserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户 业务层处理
 */
@Service
public class DoctorServiceImpl implements DoctorService {
    private static final String CACHE_ID_PREFIX = "hospital:doctor:info:";

    private static final Logger log = LoggerFactory.getLogger(DoctorServiceImpl.class);

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DutyInfoMapper dutyInfoMapper;

    @Autowired
    private SysUserServiceImpl sysUserService;

    @Autowired
    private BasicInfoCache basicInfoCache;
    @Override
    public List<DoctorVo> selectUserList(DoctorVo user) {

        return doctorMapper.selectUserList(user);
    }

    @Override
    public void check(SysUser user) {
        // 根据用户名称查询用户信息
        SysUser sysUser = doctorMapper.selectUserByUserName(user.getUserName());
        if(sysUser != null){
            throw new ServiceException("用户名已存在");
        }
    }

    @Override
    @Transactional
    public int insertUser(SysUser user) {
        // 新增用户信息
        int i = sysUserService.insertUser(user);
        // 插入医生医院关系表
        doctorMapper.insertDocHosp(user);
        basicInfoCache.evictAfterCommit(cacheKey(user.getUserId()));
        return i;
    }

    @Override
    public int deleteUserByIds(Long[] userIds) {
        dutyInfoMapper.deleteDutyInfoByScheduleIds(userIds);
        int result = sysUserService.deleteUserByIds(userIds);
        if (result > 0) {
            for (Long userId : userIds) {
                basicInfoCache.evictAfterCommit(cacheKey(userId));
            }
        }
        return result;
    }

    @Override
    public DoctorVo selectUserById(Long userId) {
        if (userId == null) {
            return doctorMapper.selectUserById(null);
        }
        return basicInfoCache.getOrLoad(cacheKey(userId), DoctorVo.class,
                () -> doctorMapper.selectUserById(userId));
    }

    @Override
    public int updateUser(DoctorVo doctorVo) {
        doctorMapper.updateDocHosp(doctorVo);
        int result = doctorMapper.update(doctorVo);
        if (result > 0) {
            basicInfoCache.evictAfterCommit(cacheKey(doctorVo.getUserId()));
        }
        return result;
    }

    private String cacheKey(Long userId) {
        return CACHE_ID_PREFIX + userId;
    }
}
