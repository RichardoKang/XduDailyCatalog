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

import static com.XduDailyCatalog.utils.RedisConstants.CACHE_SHOP_KEY;

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

        Shop shop = getById(id);
        log.debug("从数据库中获取了商铺列表");

        if(shop == null){
            return Result.fail("商铺不存在");
        }

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));

        return Result.ok(shop);
    }
}
