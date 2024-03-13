package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session){
        // 1. 校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            // 1.1 不符合，报错
            return Result.fail("手机号格式不正确");
        }

        // 1.2 符合要求
        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 3. 保存验证码到session
        //session.setAttribute("code", code);
        // 3 保存验证码到redis
        stringRedisTemplate.opsForValue().set("LOGIN_CODE_KEY" + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 4. 发送验证码
        log.debug("已发送短信验证码，验证码为： {}",code);
        // 5. 返回成功
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session){
        // 1. 校验手机号
        String phone = loginForm.getPhone();

        if(RegexUtils.isPhoneInvalid(phone)){
            // 1.1 不符合，报错
            return Result.fail("手机号格式不正确");
        }
        // 2. 校验验证码
        //Object cacheCode = session.getAttribute("code");
        String cacheCode = stringRedisTemplate.opsForValue().get("LOGIN_CODE_KEY" + phone);
        String code = loginForm.getCode();

        if(cacheCode == null || !cacheCode.equals(code)){
            // 2.1 不一致，报错
            return Result.fail("验证码错误");
        }
        // 3. 一致，根据手机号查询用户 select * from tb_ser where phone = ?
        User user = query().eq("phone", phone).one();
        // 4. 查询用户是否存在
        if(user == null){
            // 4.1 不存在，注册用户
            user = createUserWithPhone(phone);
        }
        // 5. 保存用户信息到session
        // session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        // 5 保存用户信息到redis
        // 5.1 生成token
        String token = UUID.randomUUID().toString();

        // 5.2将User对象转换为Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        stringRedisTemplate.opsForHash().putAll("LOGIN_USER_KEY" + token, userMap);

        // 5.3 设置过期时间
        stringRedisTemplate.expire("LOGIN_USER_KEY" + token, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 6. 返回成功
        return Result.ok();
    }

    private User createUserWithPhone(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
