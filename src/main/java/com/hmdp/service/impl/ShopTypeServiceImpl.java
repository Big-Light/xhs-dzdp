package com.hmdp.service.impl;

import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public List queryTypeList() {
        //1、从redis中查询，查到了直接返回
        List list = redisTemplate.boundListOps("type:list:").range(0, 10);

        if (list != null && !list.isEmpty()) return list;


        //2、没有查到就从数据库中查，这个肯定能查到
        return list;
    }
}
