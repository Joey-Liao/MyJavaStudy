package com.powernode.listener;

import com.alibaba.fastjson2.JSON;
import com.powernode.domain.MsgModel;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: DLJD
 * @Date: 2023/4/22
 */
@Component
@RocketMQMessageListener(topic = "bootTagTopic",
        consumerGroup = "boot-tag-consumer-group",
        selectorType = SelectorType.TAG,// tag过滤模式
        selectorExpression = "tagA || tagB"
//        selectorType = SelectorType.SQL92,// sql92过滤模式
//        selectorExpression = "a in (3,5,7)" // broker.conf中开启enbalePropertyFilter=true
)
public class CTagMsgListener implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        System.out.println(new String(message.getBody()));
    }
}
