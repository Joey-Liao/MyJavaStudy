package com.powernode.listener;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: DLJD
 * @Date: 2023/4/22
 */
@Component
@RocketMQMessageListener(topic = "jumpTopic",
        consumerGroup = "jump-consumer-group",
        consumeThreadNumber = 40,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class FJumpListener implements RocketMQListener<String> {


    @Override
    public void onMessage(String message) {
        System.out.println("我是消费者:" + message);
    }
}
