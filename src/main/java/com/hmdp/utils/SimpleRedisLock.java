package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;



public class SimpleRedisLock implements ILock {
    /**
     * RedisTemplate
     */
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 锁的名称
     */
    private String name;

    public static final String KEY_PREFIX = "lock:";
    /**
     * ID前缀
     */
    public static final String ID_PREFIX = UUID.randomUUID().toString() + "-";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }


    /**
     * 获取锁
     *
     * @param timeoutSec 超时时间
     * @return
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        String threadId = ID_PREFIX + Thread.currentThread().getId() + "";
        // SET lock:name id EX timeoutSec NX
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        // 判断 锁的线程标识 是否与 当前线程一致
        String currentThreadFlag = ID_PREFIX + Thread.currentThread().getId();
        String redisThreadFlag = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if (currentThreadFlag != null || currentThreadFlag.equals(redisThreadFlag)) {
            // 一致，说明当前的锁就是当前线程的锁，可以直接释放
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
        // 不一致，不能释放
    }



}
