package com.powernode.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: DLJD
 * @Date: 2023/4/24
 */
@RestController
public class QpsTestController {

    /**
     *
     * @return
     */
    @GetMapping("test")
    public String qpsTest() {
        return "ok";
    }


    @GetMapping("test2")
    public String qpsTest2() {
        // dosth. 处理处理 操作数据库 操作redis ....  50ms
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "ok";
    }

}
