package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
/**
 * 登录拦截器
 */

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取session
        // HttpSession session = request.getSession();
        // 1. 获取token
        String token = request.getHeader("authorization");
        // 1.1 判断是否存在
        if (StrUtil.isBlank(token)) {
            return true;
        }

        // 2， 获取session用户
        //Object user = session.getAttribute("user");
        // 2. 基于token获取用户
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().
                entries(key);

        // 3. 判断是否存在
        if(userMap.isEmpty()){
            // 3.1 不存在，报错
            response.setStatus(401);
            return false;
        }

        // 4. 将hash用户转换为dto用户
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap,new UserDTO(), false);
        // 3.2 存在，保存信息到threadLocal
        UserHolder.saveUser(userDTO);

        // 5. 刷新token
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
