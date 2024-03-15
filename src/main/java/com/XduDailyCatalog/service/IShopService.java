package com.XduDailyCatalog.service;

import com.XduDailyCatalog.dto.Result;
import com.XduDailyCatalog.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author KW
 * @since 2024-3-14
 */
public interface IShopService extends IService<Shop> {

    Result queryShopById(Long id) throws InterruptedException;

    Result updateShop(Shop shop);

}
