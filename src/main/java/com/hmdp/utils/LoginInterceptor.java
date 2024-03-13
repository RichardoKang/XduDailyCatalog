package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取session
        // HttpSession session = request.getSession();
        // 1. 获取token

        // 2， 获取session用户
        //Object user = session.getAttribute("user");
        // 2. 基于token获取用户

        // 3. 将hash用户转换为dto用户



        // 3. 判断是否存在
        if(user == null){
            // 3.1 不存在，报错
            response.setStatus(401);
            return false;
        }

        // 3.2 存在，保存信息到threadLocal
        UserHolder.saveUser((UserDTO) user);


        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
