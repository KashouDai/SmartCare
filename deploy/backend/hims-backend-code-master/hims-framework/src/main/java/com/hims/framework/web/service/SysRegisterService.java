package com.hims.framework.web.service;

import cn.hutool.core.util.RandomUtil;
import com.hims.common.constant.CacheConstants;
import com.hims.common.constant.Constants;
import com.hims.common.core.domain.entity.SysUser;
import com.hims.common.core.domain.model.RegisterBody;
import com.hims.common.core.redis.RedisCache;
import com.hims.common.exception.ServiceException;
import com.hims.common.exception.user.CaptchaException;
import com.hims.common.exception.user.CaptchaExpireException;
import com.hims.common.utils.MessageUtils;
import com.hims.common.utils.SecurityUtils;
import com.hims.common.utils.StringUtils;
import com.hims.framework.manager.AsyncManager;
import com.hims.framework.manager.factory.AsyncFactory;
import com.hims.info.service.IPatientsInfoService;
import com.hims.system.service.ISysConfigService;
import com.hims.system.service.ISysUserService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 注册校验方法
 *
 *
 */
@Component
public class SysRegisterService {
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private IPatientsInfoService patientsInfoService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 注册
     */
    public String register(RegisterBody registerBody) {
        String msg = "";
        String userName = registerBody.getUserName();
        // 校验用户名是否重复
        SysUser user = userService.selectUserByUserName(userName);
        if (!ObjectUtils.isEmpty(user)) {
            throw new ServiceException("注册失败,用户名重复");
        } else {
            SysUser sysUser = new SysUser();
            sysUser.setUserName(userName);
            sysUser.setNickName("用户"+ RandomUtil.randomStringUpper(5));
            sysUser.setPassword(SecurityUtils.encryptPassword(registerBody.getPassword()));
            sysUser.setUserType("01");
            boolean regFlag = userService.registerUser(sysUser);
            if (!regFlag) {
                throw new ServiceException("注册失败,请联系系统管理人员");
            } else {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.REGISTER,
                        MessageUtils.message(
                                "user.register.success")));
            }
        }
        return msg;
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
