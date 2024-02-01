package com.powernode.mapper;

import com.powernode.domain.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: DLJD
 * @Date:   2023/4/24
 */
public interface OrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);
}