package com.powernode;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: DLJD
 * @Date: 2023/4/24
 */
@Component
public class A {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
}
