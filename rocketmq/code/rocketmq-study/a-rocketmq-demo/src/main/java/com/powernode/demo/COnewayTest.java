package com.powernode.demo;

import com.powernode.constant.MqConstant;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.junit.Test;

/**
 * @Author: DLJD
 * @Date: 2023/4/21
 */
public class COnewayTest {


    @Test
    public void onewayProducer() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("oneway-producer-group");
        producer.setNamesrvAddr(MqConstant.NAME_SRV_ADDR);
        producer.start();
        Message message = new Message("onewayTopic", "日志xxx".getBytes());
        producer.sendOneway(message);
        System.out.println("成功");
        producer.shutdown();
    }


}
