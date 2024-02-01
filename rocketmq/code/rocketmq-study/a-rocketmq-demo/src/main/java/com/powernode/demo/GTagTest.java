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

/**
 * @Author: DLJD
 * @Date: 2023/4/22
 */
public class GTagTest {


    @Test
    public void tagProducer() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("tag-producer-group");
        producer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        producer.start();
        Message message = new Message("tagTopic", "vip1", "我是vip1的文章".getBytes());
        Message message2 = new Message("tagTopic", "vip2", "我是vip2的文章".getBytes());
        producer.send(message);
        producer.send(message2);
        System.out.println("发送成功");
        producer.shutdown();
    }

    // 订阅关系一致性

    /**
     * vip1
     *
     * @throws Exception
     */
    @Test
    public void tagConsumer1() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("tag-consumer-group-a");
        consumer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        consumer.subscribe("tagTopic", "vip1");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                System.out.println("我是vip1的消费者，我正在消费消息" + new String(msgs.get(0).getBody()));
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.in.read();
    }


    /**
     * vip1 || vip2
     *
     * @throws Exception
     */
    @Test
    public void tagConsumer2() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("tag-consumer-group-b");
        consumer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        consumer.subscribe("tagTopic", "vip1 || vip2");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                System.out.println("我是vip2的消费者，我正在消费消息" + new String(msgs.get(0).getBody()));
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.in.read();
    }

}
