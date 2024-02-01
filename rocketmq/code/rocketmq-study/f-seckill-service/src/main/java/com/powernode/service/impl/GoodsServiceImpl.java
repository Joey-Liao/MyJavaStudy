package com.powernode.service.impl;

import com.powernode.domain.Order;
import com.powernode.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import com.powernode.domain.Goods;
import com.powernode.mapper.GoodsMapper;
import com.powernode.service.GoodsService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @Author: DLJD
 * @Date: 2023/4/24
 */
@Service
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public int deleteByPrimaryKey(Integer goodsId) {
        return goodsMapper.deleteByPrimaryKey(goodsId);
    }

    @Override
    public int insert(Goods record) {
        return goodsMapper.insert(record);
    }

    @Override
    public int insertSelective(Goods record) {
        return goodsMapper.insertSelective(record);
    }

    @Override
    public Goods selectByPrimaryKey(Integer goodsId) {
        return goodsMapper.selectByPrimaryKey(goodsId);
    }

    @Override
    public int updateByPrimaryKeySelective(Goods record) {
        return goodsMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(Goods record) {
        return goodsMapper.updateByPrimaryKey(record);
    }

    /////////////////////////////////

    /**
     *
     *  常规的 方案
     *  锁加载调用方法的地方 要加载事务外面
     * @param userId
     * @param goodsId
     */
//    @Override
//    @Transactional(rollbackFor = Exception.class) // rr
//    public void realSeckill(Integer userId, Integer goodsId) {
//        // 扣减库存  插入订单表
//        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
//        int finalStock = goods.getTotalStocks() - 1;
//        if (finalStock < 0) {
//            // 只是记录日志 让代码停下来   这里的异常用户无法感知
//            throw new RuntimeException("库存不足：" + goodsId);
//        }
//        goods.setTotalStocks(finalStock);
//        goods.setUpdateTime(new Date());
//        // update goods set stocks =  1 where id = 1  没有行锁
//        int i = goodsMapper.updateByPrimaryKey(goods);
//        if (i > 0) {
//            Order order = new Order();
//            order.setGoodsid(goodsId);
//            order.setUserid(userId);
//            order.setCreatetime(new Date());
//            orderMapper.insert(order);
//        }
//    }//


    /**
     * 行锁(innodb)方案 mysql  不适合用于并发量特别大的场景
     * 因为压力最终都在数据库承担
     *
     * @param userId
     * @param goodsId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void realSeckill(Integer userId, Integer goodsId) {
        // update goods set total_stocks = total_stocks - 1 where goods_id = goodsId and total_stocks - 1 >= 0;
        // 通过mysql来控制锁
        int i = goodsMapper.updateStock(goodsId);
        if (i > 0) {
            Order order = new Order();
            order.setGoodsid(goodsId);
            order.setUserid(userId);
            order.setCreatetime(new Date());
            orderMapper.insert(order);
        }
    }

}
