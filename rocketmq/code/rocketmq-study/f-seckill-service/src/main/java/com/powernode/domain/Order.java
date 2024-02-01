package com.powernode.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: DLJD
 * @Date:   2023/4/24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order implements Serializable {
    private Integer id;

    private Integer userid;

    private Integer goodsid;

    private Date createtime;

    private static final long serialVersionUID = 1L;
}