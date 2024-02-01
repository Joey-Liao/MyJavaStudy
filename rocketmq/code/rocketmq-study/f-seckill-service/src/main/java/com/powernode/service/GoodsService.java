package com.powernode.service;

import com.powernode.domain.Goods;

/**
 * @Author: DLJD
 * @Date: 2023/4/24
 */
public interface GoodsService {


    int deleteByPrimaryKey(Integer goodsId);

    int insert(Goods record);

    int insertSelective(Goods record);

    Goods selectByPrimaryKey(Integer goodsId);

    int updateByPrimaryKeySelective(Goods record);

    int updateByPrimaryKey(Goods record);

    /**
     * 真正处理秒杀的业务
     *
     * @param userId
     * @param goodsId
     */
    void realSeckill(Integer userId, Integer goodsId);
}
