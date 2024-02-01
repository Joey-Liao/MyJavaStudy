package com.powernode.listener;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: DLJD
 * @Date: 2023/4/22
 */
@Component
@RocketMQMessageListener(topic = "bootTestTopic", consumerGroup = "boot-test-consumer-group")
public class ABootSimpleMsgListener implements RocketMQListener<MessageExt> {

    /**
     * 这个方法就是消费者的方法
     * 如果泛型制定了固定的类型 那么消息体就是我们的参数
     * MessageExt 类型是消息的所有内容
     * ------------------------
     * 没有报错 就签收了
     * 如果报错了 就是拒收 就会重试
     *
     * @param message
     */
    @Override
    public void onMessage(MessageExt message) {
        System.out.println(new String(message.getBody()));
    }
}
