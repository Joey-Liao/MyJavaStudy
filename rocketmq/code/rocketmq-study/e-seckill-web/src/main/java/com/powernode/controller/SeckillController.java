package com.powernode.controller;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: DLJD
 * @Date: 2023/4/24
 */
@RestController
public class SeckillController {


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    //CAS java无锁的   原子性 安全的
    AtomicInteger userIdAt = new AtomicInteger(0);

    /**
     * 1.用户去重
     * 2.库存的预扣减
     * 3.消息放入mq
     * 秒杀不是一个单独的系统
     * 都是大项目的某一个小的功能模块
     *
     * @param goodsId
     * @param userId  真实的项目中 要做登录的 不要穿这个参数
     * @return
     */
    @GetMapping("seckill")
    public String doSecKill(Integer goodsId /*, Integer userId*/) {
        // log 2023-4-24 16:58:11
        // log 2023-4-24 16:58:11
        int userId = userIdAt.incrementAndGet();
        // uk uniqueKey = [yyyyMMdd] +  userId + goodsId
        String uk = userId + "-" + goodsId;
        // setIfAbsent = setnx
        Boolean flag = redisTemplate.opsForValue().setIfAbsent("uk:" + uk, "");
        if (!flag) {
            return "您已经参与过该商品的抢购，请参与其他商品O(∩_∩)O~";
        }
        // 记住 先查再改 再更新  不安全的操作
        Long count = redisTemplate.opsForValue().decrement("goodsId:" + goodsId);
        if (count < 0) {
            // 保证我的redis的库存 最小值是0
            redisTemplate.opsForValue().increment("goodsId:" + goodsId);
            return "该商品已经被抢完,下次早点来(●ˇ∀ˇ●)";
        }
        // 方mq 异步处理
        rocketMQTemplate.asyncSend("seckillTopic3", uk, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("发送成功");
            }

            @Override
            public void onException(Throwable throwable) {
                System.out.println("发送失败:" + throwable.getMessage());
                System.out.println("用户的id:" + userId + "商品id" + goodsId);
            }
        });
        return "正在拼命抢购中,请稍后去订单中心查看";
    }


    /**
     * 抢一个付费的商品
     * 1.先扣减库存  再付费  | 如果不付费 库存需要回滚
     * 2.先付费  再扣减库存  | 如果库存不足  则退费
     */

}
