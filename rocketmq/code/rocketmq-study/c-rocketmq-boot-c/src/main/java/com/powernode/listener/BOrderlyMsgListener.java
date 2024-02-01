package com.powernode.listener;

import com.alibaba.fastjson2.JSON;
import com.powernode.domain.MsgModel;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: DLJD
 * @Date: 2023/4/22
 */
@Component
@RocketMQMessageListener(topic = "bootOrderlyTopic",
        consumerGroup = "boot-orderly-consumer-group",
        consumeMode = ConsumeMode.ORDERLY, // 顺序消费模式 单线程
        maxReconsumeTimes = 5 // 消费重试的次数
)
public class BOrderlyMsgListener implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        MsgModel msgModel = JSON.parseObject(new String(message.getBody()), MsgModel.class);
        System.out.println(msgModel);
    }
}
