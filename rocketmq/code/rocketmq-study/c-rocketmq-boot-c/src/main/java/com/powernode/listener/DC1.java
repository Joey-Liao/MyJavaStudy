package com.powernode.listener;

import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: DLJD
 * @Date: 2023/4/22
 * [CLUSTERING] 集群模式下 队列会被消费者分摊, 队列数量>=消费者数量 消息的消费位点 mq服务器会记录处理
 * BROADCASTING 广播模式下 消息会被每一个消费者都处理一次, mq服务器不会记录消费点位,也不会重试
 */
@Component
@RocketMQMessageListener(topic = "modeTopic",
        consumerGroup = "mode-consumer-group-a",
        messageModel = MessageModel.CLUSTERING, // 集群模式 负载均衡
        consumeThreadNumber = 40

)
public class DC1 implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("我是mode-consumer-group-a组的第一个消费者:" + message);
    }
}
