package com.hmdp.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.utils.RegexUtils;

import javax.servlet.http.HttpSession;
import java.util.Random;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    /**
     * 发送手机验证码
     * @param phone 手机号
     * @param session session
     * @return 发送结果
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @param session session
     * @return 登录结果
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

}
