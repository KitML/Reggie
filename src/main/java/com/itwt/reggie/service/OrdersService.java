package com.itwt.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itwt.reggie.entity.Orders;

public interface OrdersService extends IService<Orders> {
    public void submit(Orders orders);
}
