package com.powernode.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: DLJD
 * @Date:   2023/4/24
 */
/**
    * 商品
    */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Goods implements Serializable {
    /**
    * 商品ID
    */
    private Integer goodsId;

    /**
    * 商品名称
    */
    private String goodsName;

    /**
    * 现价
    */
    private BigDecimal price;

    /**
    * 详细描述
    */
    private String content;

    /**
    * 默认是1，表示正常状态, -1表示删除, 0下架
    */
    private Integer status;

    /**
    * 总库存
    */
    private Integer totalStocks;

    /**
    * 录入时间
    */
    private Date createTime;

    /**
    * 修改时间
    */
    private Date updateTime;

    /**
    * 是否参与秒杀1是0否
    */
    private Integer spike;

    private static final long serialVersionUID = 1L;
}