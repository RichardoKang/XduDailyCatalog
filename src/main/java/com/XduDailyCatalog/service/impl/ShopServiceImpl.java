package com.XduDailyCatalog.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.XduDailyCatalog.dto.Result;
import com.XduDailyCatalog.entity.Shop;
import com.XduDailyCatalog.mapper.ShopMapper;
import com.XduDailyCatalog.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.XduDailyCatalog.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 */

@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private StringRedisTemplate stringRedisTemplate;

    public ShopServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result queryShopById(Long id) {
        String key = CACHE_SHOP_KEY + id;

        // 从缓存中获取商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 如果缓存中有数据，直接返回
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            log.debug("从缓存中获取了商铺列表");
            return Result.ok(shop);
        }

        // 如果命中的是空数据，直接返回
        if(shopJson != null){
            return Result.fail("商铺不存在");
        }

        Shop shop = getById(id);
        log.debug("从数据库中获取了商铺列表");

        if(shop == null){
            // 如果数据库中没有数据，将空数据存入缓存，设置过期时间
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return Result.fail("商铺不存在");
        }


        // 将数据存入缓存，设置过期时间
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(shop);
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
