package com.XduDailyCatalog.service.impl;

import com.XduDailyCatalog.dto.Result;
import com.XduDailyCatalog.entity.SeckillVoucher;
import com.XduDailyCatalog.entity.User;
import com.XduDailyCatalog.entity.VoucherOrder;
import com.XduDailyCatalog.mapper.VoucherOrderMapper;
import com.XduDailyCatalog.service.ISeckillVoucherService;
import com.XduDailyCatalog.service.IVoucherOrderService;
import com.XduDailyCatalog.utils.RedisidWorker;
import com.XduDailyCatalog.utils.UserHolder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisidWorker redisidWorker;

    @Override

    public Result seckillVoucher(Long voucherId) {
        // 1. 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2. 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀未开始！");
        }
        // 3. 判断是否已经秒杀过
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束！");
        }
        // 4. 判断库存是否足够
        if (voucher.getStock() <= 0) {
            return Result.fail("库存不足！");
        }

        Long userId = UserHolder.getUser().getId();
        synchronized(userId.toString().intern()) {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.creatVoucherOrder(voucherId);
        }
    }

    @Transactional
    public Result creatVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();

        if (count > 0) {
            return Result.fail("不得重复购买！");
        }
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足！");
        }

        // 6. 生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1 订单id
        long orderId = redisidWorker.nextId("order");
        voucherOrder.setId(orderId);

        // 6.2 用户id

        voucherOrder.setUserId(userId);

        // 6.3 优惠券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return Result.ok(orderId);


    }
}
