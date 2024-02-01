package com.powernode.config;

import com.powernode.domain.Goods;
import com.powernode.mapper.GoodsMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @Author: DLJD
 * @Date: 2023/4/24
 * 1.每天10点 晚上8点 通过定时任务 将mysql的库存 同步到redis中去
 * 2.为了测试方便 希望项目启动的时候 就同步数据
 */
@Component
public class DataSync {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

//    @Scheduled(cron = "0 0 10 0 0 ?")
//    public void initData(){
//    }

    /**
     * 我希望这个方法再项目启动以后
     * 并且再这个类的属性注入完毕以后执行
     * bean生命周期了
     * 实例化 new
     * 属性赋值
     * 初始化  (前PostConstruct/中InitializingBean/后BeanPostProcessor)
     * 使用
     * 销毁
     * ----------
     * 定位不一样
     */
    @PostConstruct
    public void initData() {
        List<Goods> goodsList = goodsMapper.selectSeckillGoods();
        if (CollectionUtils.isEmpty(goodsList)) {
            return;
        }
        goodsList.forEach(goods -> {
            redisTemplate.opsForValue().set("goodsId:" + goods.getGoodsId(), goods.getTotalStocks().toString());
        });
    }


}
