package com.powernode.demo;

import com.powernode.constant.MqConstant;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.Test;

import java.util.List;

/**
 * @Author: DLJD
 * @Date: 2023/4/21
 */
public class ASimpleTest {

    @Test
    public void simpleProducer() throws Exception {
        // 创建一个生产者  (制定一个组名)
        DefaultMQProducer producer = new DefaultMQProducer("test-producer-group");
        // 连接namesrv
        producer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        // 启动
        producer.start();
        // 创建一个消息
        for (int i = 0; i < 10; i++) {
            Message message = new Message("testTopic", "我是一个简单的消息".getBytes());
            // 发送消息
            SendResult sendResult = producer.send(message);
            System.out.println(sendResult.getSendStatus());
        }
        // 关闭生产者
        producer.shutdown();
    }

    //////////////////// 消费者
    @Test
    public void simpleConsumer() throws Exception {
        // 创建一个消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("test-consumer-group");
        // 连接namesrv
        consumer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        // 订阅一个主题  * 标识订阅这个主题中所有的消息  后期会有消息过滤
        consumer.subscribe("testTopic", "*");
        // 设置一个监听器 (一直监听的， 异步回调方式)
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                // 这个就是消费的方法 （业务处理）
                System.out.println("我是消费者");
                System.out.println(msgs.get(0).toString());
                System.out.println("消息内容:" + new String(msgs.get(0).getBody()));
                System.out.println("消费上下文:" + context);
                // 返回值  CONSUME_SUCCESS成功，消息会从mq出队
                // RECONSUME_LATER（报错/null） 失败 消息会重新回到队列 过一会重新投递出来 给当前消费者或者其他消费者消费的
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 启动
        consumer.start();
        // 挂起当前的jvm
        System.in.read();
    }


}
