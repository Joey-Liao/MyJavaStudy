package com.powernode.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: DLJD
 * @Date: 2023/4/21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MsgModel {

    private String orderSn;
    private Integer userId;
    private String desc; // 下单 短信 物流

}
