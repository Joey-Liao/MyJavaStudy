package com.powernode;

import com.powernode.constant.MqConstant;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class ARocketmqDemoApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Test
    void repeatProducer() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("repeat-producer-group");
        producer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        producer.start();
        String key = UUID.randomUUID().toString();
        System.out.println(key);
        // 测试 发两个key一样的消息
        Message m1 = new Message("repeatTopic", null, key, "扣减库存-1".getBytes());
        Message m1Repeat = new Message("repeatTopic", null, key, "扣减库存-1".getBytes());
        producer.send(m1);
        producer.send(m1Repeat);
        System.out.println("发送成功");
        producer.shutdown();
    }

    /**
     * 幂等性(mysql的唯一索引, redis(setnx) )
     * 多次操作产生的影响均和第一次操作产生的影响相同
     * 新增:普通的新增操作  是非幂等的,唯一索引的新增,是幂等的
     * 修改:看情况
     * 查询: 是幂等操作
     * 删除:是幂等操作
     * ---------------------
     * 我们设计一个去重表 对消息的唯一key添加唯一索引
     * 每次消费消息的时候 先插入数据库 如果成功则执行业务逻辑 [如果业务逻辑执行报错 则删除这个去重表记录]
     * 如果插入失败 则说明消息来过了,直接签收了
     *
     * @throws Exception
     */
    @Test
    void repeatConsumer() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("repeat-consumer-group");
        consumer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        consumer.subscribe("repeatTopic", "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                // 先拿key
                MessageExt messageExt = msgs.get(0);
                String keys = messageExt.getKeys();
                // 原生方式操作
                Connection connection = null;
                try {
                    connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test?serverTimezone=GMT%2B8&useSSL=false", "root", "123456");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                PreparedStatement statement = null;

                try {
                    // 插入数据库 因为我们 key做了唯一索引
                    statement = connection.prepareStatement("insert into order_oper_log(`type`, `order_sn`, `user`) values (1,'" + keys + "','123')");
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try {
                    // 新增 要么成功 要么报错   修改 要么成功,要么返回0 要么报错
                    statement.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("executeUpdate");
                    if (e instanceof SQLIntegrityConstraintViolationException) {
                        // 唯一索引冲突异常
                        // 说明消息来过了
                        System.out.println("该消息来过了");
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    }
                    e.printStackTrace();
                }

                // 处理业务逻辑
                // 如果业务报错 则删除掉这个去重表记录 delete order_oper_log where order_sn = keys;
                System.out.println(new String(messageExt.getBody()));
                System.out.println(keys);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.in.read();
    }


}
