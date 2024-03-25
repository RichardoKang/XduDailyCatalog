package com.XduDailyCatalog.service;

import com.XduDailyCatalog.dto.Result;
import com.XduDailyCatalog.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);
}
