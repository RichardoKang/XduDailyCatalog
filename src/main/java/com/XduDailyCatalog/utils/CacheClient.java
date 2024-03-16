package com.XduDailyCatalog.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.XduDailyCatalog.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.XduDailyCatalog.utils.RedisConstants.*;
import static com.XduDailyCatalog.utils.RedisConstants.CACHE_SHOP_TTL;

@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value,Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue()
                .set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue()
                .set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type
                                            ,Function<ID, R> dbFallback, long expireTime, TimeUnit unit) {
        String key = keyPrefix + id;

        // 1 从缓存中获取信息
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2 如果缓存中无数据，直接返回空
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }

        // 如果命中的是空数据，直接返回
        if(json != null){
            return null;
        }

        // 函数式接口，从数据库中获取数据
        R r = dbFallback.apply(id);

        if(r == null){
            // 如果数据库中没有数据，将空数据存入缓存，设置过期时间
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }


        // 将数据存入缓存，设置过期时间
        this.set(key, r, expireTime, unit);
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type
            , Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;

        // 1 从缓存中获取商铺信息
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2 如果缓存中无数据，直接返回空
        if(StrUtil.isBlank(json)){
            return null;
        }

        // 3 命中，先把JSON转为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject)redisData.getData(), type);
        LocalDateTime expireTime= redisData.getExpireTime();

        // 4 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 4.1 未过期，直接返回信息
            return r;
        }
        // 4.2 已过期，缓存重建
        // 4.2.1 尝试获取锁
        String lockKey = keyPrefix + id;
        boolean isLock = tryLock(lockKey);
        // 4.2.2 获取到锁，从数据库中获取数据
        if(isLock){
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    // 重建缓存
                    R result = dbFallback.apply(id);
                    // 存入缓存
                    this.setWithLogicalExpire(key, result, time, unit);
                    log.debug("已重建缓存");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }

            });
        }

        return r;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

}
