package com.XduDailyCatalog.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.XduDailyCatalog.dto.Result;
import com.XduDailyCatalog.entity.Shop;
import com.XduDailyCatalog.mapper.ShopMapper;
import com.XduDailyCatalog.service.IShopService;
import com.XduDailyCatalog.utils.CacheClient;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.XduDailyCatalog.utils.RedisConstants.*;
import com.XduDailyCatalog.utils.RedisData;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 */

@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryShopById(Long id) throws InterruptedException {
        // 通过缓存穿透处理查询商铺信息
        //Shop = queryWithPassThrough(id);

        // 互斥锁，防止缓存击穿
        // Shop = queryWithMutex(id);

        // 逻辑过期时间，防止缓存雪崩
        // Shop = queryWithLogicalExpire(id);

        // redis类封装缓存穿透
        // Shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // redis类封装,逻辑过期时间解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);


        if(shop == null){
            return Result.fail("商铺不存在");
        }


        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public Shop queryWithLogicalExpire(Long id){
        String key = CACHE_SHOP_KEY + id;

        // 1 从缓存中获取商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2 如果缓存中无数据，直接返回空
        if(StrUtil.isBlank(shopJson)){
            return null;
        }

        // 3 命中，先把JSON转为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject)redisData.getData(), Shop.class);
        LocalDateTime expireTime= redisData.getExpireTime();

        // 4 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 4.1 未过期，直接返回信息
            return shop;
        }
        // 4.2 已过期，缓存重建
        // 4.2.1 尝试获取锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 4.2.2 获取到锁，从数据库中获取数据
        if(isLock){
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    // 重建缓存
                    this.saveShop2Redis(id, 20L);
                    log.debug("已重建缓存");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }

            });
        }

        return shop;
    }

    public Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;

        // 从缓存中获取商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 如果缓存中有数据，直接返回
        if(StrUtil.isNotBlank(shopJson)){
            log.debug("从缓存中获取了商铺列表");
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 如果命中的是空数据，直接返回
        if(shopJson != null){
            return null;
        }

        Shop shop = getById(id);
        log.debug("从数据库中获取了商铺列表");

        if(shop == null){
            // 如果数据库中没有数据，将空数据存入缓存，设置过期时间
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }


        // 将数据存入缓存，设置过期时间
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;
    }


    public Shop queryWithMutex(Long id) throws InterruptedException {
        String key = CACHE_SHOP_KEY + id;

        // 1 从缓存中获取商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2 如果缓存中有数据，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            log.debug("从缓存中获取了商铺列表");
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 3 如果命中的是空数据，直接返回
        if (shopJson != null) {
            return null;
        }

        // 4 实现缓存重建
        // 4.1 尝试获取锁
        String lockKey = "lock:shop:" + id;
        Shop shop = null;

        try {
            boolean isLock = tryLock(lockKey);

            // 4.2 判断是否成功获取到锁
            if (!isLock) {
                // 4.2.1 未获取到锁，等待一段时间后重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            // 4.2.2 获取到锁，从数据库中获取数据
            shop = getById(id);
            log.debug("从数据库中获取了商铺列表");
            // 模拟重建延迟
            Thread.sleep(200);
            // 5 如果数据库中没有数据，将空数据存入缓存，设置过期时间
            if (shop == null) {
                // 如果数据库中没有数据，将空数据存入缓存，设置过期时间
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 6 将数据存入缓存，设置过期时间
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7 释放锁
            unlock(lockKey);
        }

        return shop;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }


    public void saveShop2Redis(Long id,Long expireSeconds ) throws InterruptedException {
        // 1 获得店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        // 2 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        // 3 写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));

    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {

        Long id = shop.getId();
        if(id == null){
            return Result.fail("商铺id不能为空");
        }
        // 写入数据库
        updateById(shop);

        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }
}
