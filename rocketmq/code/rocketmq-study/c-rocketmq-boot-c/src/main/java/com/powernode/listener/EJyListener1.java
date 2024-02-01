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
@RocketMQMessageListener(topic = "jyTopic",
        consumerGroup = "jy-consumer-group",
        consumeThreadNumber = 40,
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class EJyListener1 implements RocketMQListener<String> {


    @Override
    public void onMessage(String message) {
        System.out.println("我是第一个消费者:" + message);
    }
}
