package com.XduDailyCatalog.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.XduDailyCatalog.dto.Result;
import com.XduDailyCatalog.entity.ShopType;
import com.XduDailyCatalog.mapper.ShopTypeMapper;
import com.XduDailyCatalog.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.XduDailyCatalog.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private StringRedisTemplate stringRedisTemplate;

    public ShopTypeServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result queryTypeList() {

        String key = CACHE_SHOP_TYPE_KEY;

        // 从缓存中获取商铺类型列表
        String shopTypeListJson = stringRedisTemplate.opsForValue().get(key);

        // 如果缓存中有数据，直接返回
        if(StrUtil.isNotBlank(shopTypeListJson)){
            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeListJson, ShopType.class);
            log.debug("从缓存中获取了商铺类型列表");
            return Result.ok(shopTypeList);
        }

        // 如果缓存中没有数据，从数据库中获取
        List<ShopType> typeList = list();
        log.debug("从数据库中获取了商铺类型列表");

        // 如果数据库中没有数据，返回错误信息
        if(typeList == null || typeList.isEmpty()){
            return Result.fail("商铺类型不存在");
        }

        // 将数据存入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList));

        return Result.ok(typeList);
    }


}
