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

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @Author: DLJD
 * @Date: 2023/4/22
 */
public class IRetryTest {


    @Test
    public void retryProducer() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("retry-producer-group");
        producer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        producer.start();
        // 生产者发送消息 重试次数
        producer.setRetryTimesWhenSendFailed(2);
        producer.setRetryTimesWhenSendAsyncFailed(2);
        String key = UUID.randomUUID().toString();
        System.out.println(key);
        Message message = new Message("retryTopic", "vip1", key, "我是vip666的文章".getBytes());
        producer.send(message);
        System.out.println("发送成功");
        producer.shutdown();
    }

    /**
     * 重试的时间间隔
     * 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     * 默认重试16次
     * 1.能否自定义重试次数
     * 2.如果重试了16次(并发模式) 顺序模式下(int最大值次)都是失败的?  是一个死信消息 则会放在一个死信主题中去 主题的名称:%DLQ%retry-consumer-group
     * 3.当消息处理失败的时候 该如何正确的处理?
     * --------------
     * 重试的次数一般 5次
     * @throws Exception
     */
    @Test
    public void retryConsumer() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("retry-consumer-group");
        consumer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        consumer.subscribe("retryTopic", "*");
        // 设定重试次数
        consumer.setMaxReconsumeTimes(2);
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt messageExt = msgs.get(0);
                System.out.println(new Date());
                System.out.println(messageExt.getReconsumeTimes());
                System.out.println(new String(messageExt.getBody()));
                // 业务报错了 返回null 返回 RECONSUME_LATER 都会重试
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });
        consumer.start();
        System.in.read();
    }


    /////////// 直接监听死信主题的消息,记录下拉 通知人工接入处理
    @Test
    public void retryDeadConsumer() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("retry-dead-consumer-group");
        consumer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        consumer.subscribe("%DLQ%retry-consumer-group", "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt messageExt = msgs.get(0);
                System.out.println(new Date());
                System.out.println(new String(messageExt.getBody()));
                System.out.println("记录到特别的位置 文件 mysql 通知人工处理");
                // 业务报错了 返回null 返回 RECONSUME_LATER 都会重试
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.in.read();
    }

    //////////////////////// 第二种方案 用法比较多

    @Test
    public void retryConsumer2() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("retry-consumer-group");
        consumer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        consumer.subscribe("retryTopic", "*");
        // 设定重试次数
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt messageExt = msgs.get(0);
                System.out.println(new Date());
                // 业务处理
                try {
                    handleDb();
                } catch (Exception e) {
                    // 重试
                    int reconsumeTimes = messageExt.getReconsumeTimes();
                    if (reconsumeTimes >= 3) {
                        // 不要重试了
                        System.out.println("记录到特别的位置 文件 mysql 通知人工处理");
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    }
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                // 业务报错了 返回null 返回 RECONSUME_LATER 都会重试
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.in.read();
    }

    private void handleDb() {
        int i = 10 / 0;
    }


}
