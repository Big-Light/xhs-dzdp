package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.util.concurrent.RateLimiter;
import com.hmdp.config.RedissonConfig;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.rabbitmq.MQSender;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private MQSender mqSender;
    @Resource
    private RedisIdWorker redisIdWorker;

    //private RateLimiter rateLimiter = RateLimiter.create(10);
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //@Resource
    //private RedissonClient redissonClient;

    //lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVocher(Long voucherId) {
        //1.执行lua脚本
        Long userId = UserHolder.getUser().getId();

        Long r = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        //2.判断结果为0
        int result = r.intValue();
        if (result != 0) {
            //2.1不为0代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "该用户重复下单");
        }
        //2.2为0代表有购买资格,将下单信息保存到阻塞队列

        //2.3创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //2.4订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //2.5用户id
        voucherOrder.setUserId(userId);
        //2.6代金卷id
        voucherOrder.setVoucherId(voucherId);

        //2.7将信息放入MQ中
        mqSender.sendSeckillMessage(JSON.toJSONString(voucherOrder));


        //2.7 返回订单id
        return Result.ok(orderId);

//        // 1、查询秒杀券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//
//        // 2、判断秒杀券是否合法
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            // 秒杀券的开始时间在当前时间之后
//            return Result.fail("秒杀尚未开始");
//        }
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            // 秒杀券的结束时间在当前时间之前
//            return Result.fail("秒杀已结束");
//        }
//        if (voucher.getStock() < 1) {
//            return Result.fail("秒杀券已抢空");
//        }

        //return createVoucherOrder(voucherId);
    }

    /**
     * 创建订单
     *
     * @param voucherId
     * @return
     */
//    @Transactional
//    public Result createVoucherOrder(Long voucherId) {
//        //5、一人一单
//        Long userId = UserHolder.getUser().getId();
//
//        //创建锁对象
//        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
//
//        //尝试获取锁
//        boolean isLock = redisLock.tryLock();
//
//        if (!isLock) {
//            // 索取锁失败，重试或者直接抛异常（这个业务是一人一单，所以直接返回失败信息）
//            return Result.fail("一人只能下一单");
//        }
//
//        try{
//            //5.1、查询订单
//            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//            //5.2、判断是否存在
//            if(count > 0)
//                return Result.fail("该用户已经下了一单");
//
//            //6、扣减库存
//            boolean success = seckillVoucherService.update()
//                    .setSql("stock = stock - 1")
//                    .eq("voucher_id", voucherId).gt("stock", 0)
//                    .update();
//            if(!success){
//                //扣减失败
//                return Result.fail("库存不足！");
//            }
//            log.info("创建了一条订单");
//            VoucherOrder voucherOrder = new VoucherOrder();
//            long orderId = redisIdWorker.nextId("order");
//            voucherOrder.setId(orderId);
//            voucherOrder.setUserId(userId);
//            voucherOrder.setVoucherId(voucherId);
//            save(voucherOrder);
//
//            return Result.ok(orderId);
//        } finally {
//            redisLock.unlock();
//        }
//    }
}

