package com.powernode.listener;

import com.powernode.service.GoodsService;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @Author: DLJD
 * @Date: 2023/4/24
 */
@Component
@RocketMQMessageListener(topic = "seckillTopic3",
        consumerGroup = "seckill-consumer-group3",
        consumeMode = ConsumeMode.CONCURRENTLY,
        consumeThreadNumber = 40
)
public class SeckillListener implements RocketMQListener<MessageExt> {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    int ZX_TIME = 20000;

    /**
     * 扣减库存
     * 写订单表
     *
     * @param message
     */
//    @Override
//    public void onMessage(MessageExt message) {
//        String msg = new String(message.getBody());
//        // userId + "-" + goodsId
//        Integer userId = Integer.parseInt(msg.split("-")[0]);
//        Integer goodsId = Integer.parseInt(msg.split("-")[1]);
//        // 方案一: 再事务外面加锁 可以实现安全 没法集群
    // jvm  EntrySet WaitSet
//        synchronized (this) {
//            goodsService.realSeckill(userId, goodsId);
//        }
//    }


    // 方案二  分布式锁  mysql(行锁)   不适合并发较大场景
//    @Override
//    public void onMessage(MessageExt message) {
//        String msg = new String(message.getBody());
//        // userId + "-" + goodsId
//        Integer userId = Integer.parseInt(msg.split("-")[0]);
//        Integer goodsId = Integer.parseInt(msg.split("-")[1]);
//        goodsService.realSeckill(userId, goodsId);
//    }

    // 方案三: redis setnx 分布式锁  压力会分摊到redis和程序中执行  缓解db的压力
    @Override
    public void onMessage(MessageExt message) {
        String msg = new String(message.getBody());
        Integer userId = Integer.parseInt(msg.split("-")[0]);
        Integer goodsId = Integer.parseInt(msg.split("-")[1]);
        int currentThreadTime = 0;
        while (true) {
            // 这里给一个key的过期时间,可以避免死锁的发生
            Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock:" + goodsId, "", Duration.ofSeconds(30));
            if (flag) {
                // 拿到锁成功
                try {
                    goodsService.realSeckill(userId, goodsId);
                    return;
                } finally {
                    // 删除
                    redisTemplate.delete("lock:" + goodsId);
                }
            } else {
                currentThreadTime += 200;
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
