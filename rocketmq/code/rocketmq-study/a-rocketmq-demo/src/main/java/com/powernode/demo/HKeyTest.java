package com.powernode.demo;

import com.powernode.constant.MqConstant;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

/**
 * @Author: DLJD
 * @Date: 2023/4/22
 */
public class HKeyTest {

    /**
     * 业务参数 我们自身要确保唯一
     * 为了查阅和去重
     *
     * @throws Exception
     */
    @Test
    public void keyProducer() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("key-producer-group");
        producer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        producer.start();
        String key = UUID.randomUUID().toString();
        System.out.println(key);
        Message message = new Message("keyTopic", "vip1", key, "我是vip1的文章".getBytes());
        producer.send(message);
        System.out.println("发送成功");
        producer.shutdown();
    }


    @Test
    public void keyConsumer() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("key-consumer-group");
        consumer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        consumer.subscribe("keyTopic", "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt messageExt = msgs.get(0);
                System.out.println("我是vip1的消费者，我正在消费消息" + new String(messageExt.getBody()));
                System.out.println("我们业务的标识：" + messageExt.getKeys());
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.in.read();
    }


}
