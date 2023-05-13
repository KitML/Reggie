package com.itwt.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itwt.reggie.common.BaseContext;
import com.itwt.reggie.common.R;
import com.itwt.reggie.dto.OrdersDto;
import com.itwt.reggie.entity.OrderDetail;
import com.itwt.reggie.entity.Orders;
import com.itwt.reggie.service.OrderDetailService;
import com.itwt.reggie.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    /*
    * 结算
    * */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("orders:{}",orders);
        ordersService.submit(orders);
        return R.success("用户下单成功");
    }

    /*
    *历史订单
    * */
    @GetMapping("/userPage")
    public R<Page> userPage(int page,int pageSize){
        //获取当前id
        Long userId = BaseContext.getCurrentId();
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page,pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //查询当前用户id的订单数据
        queryWrapper.eq(userId != null,Orders::getUserId,userId);
        //按照时间降序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        ordersService.page(pageInfo,queryWrapper);
        List<OrdersDto> list = pageInfo.getRecords().stream().map((item)->{
            OrdersDto ordersDto= new OrdersDto();
            //获取orderId，根据id查询orderDetail表的数据
            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId,orderId);
            List<OrderDetail> list1 = orderDetailService.list(wrapper);
            //拷贝数据
            BeanUtils.copyProperties(item,ordersDto);
            ordersDto.setOrderDetails(list1);
            return ordersDto;
        }).collect(Collectors.toList());
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
        ordersDtoPage.setRecords(list);
        //输出日志
        log.info("list:{}",list);
        return R.success(ordersDtoPage);
    }

    /*
    * 订单详情
    * */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,Long number,String beginTime,String endTime){
        //获取当前id
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page,pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //按时间降序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //订单号
        queryWrapper.eq(number!=null,Orders::getId,number);
        //时间段，大于开始，小于结束
        queryWrapper.gt(!StringUtils.isEmpty(beginTime),Orders::getOrderTime,beginTime)
                .lt(!StringUtils.isEmpty(endTime),Orders::getOrderTime,endTime);
        ordersService.page(pageInfo,queryWrapper);
        List<OrdersDto> list = pageInfo.getRecords().stream().map((item)->{
            OrdersDto ordersDto = new OrdersDto();
            //获取orderId，然后根据这个id，去orderDetail表中查数据
            Long id = item.getId();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId,id);
            List<OrderDetail> details =orderDetailService.list(wrapper);
            BeanUtils.copyProperties(item,ordersDto);
            ordersDto.setOrderDetails(details);
            return ordersDto;
        }).collect(Collectors.toList());
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
        ordersDtoPage.setRecords(list);
        //输出日志
        log.info("list:{}",list);
        return R.success(ordersDtoPage);
    }

    /*
    * 派送
    * */
    @PutMapping
    public R<String> changeStatus(@RequestBody Map<String,String> map){
        int status = Integer.parseInt(map.get("status"));
        Long orderId = Long.valueOf(map.get("id"));
        log.info("修改订单状态：status={status},id={id}",status,orderId);
        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Orders::getId,orderId);
        updateWrapper.set(Orders::getStatus,status);
        ordersService.update(updateWrapper);
        return R.success("订单状态修改成功");
    }
}
