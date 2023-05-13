package com.itwt.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itwt.reggie.common.BaseContext;
import com.itwt.reggie.common.R;
import com.itwt.reggie.entity.ShoppingCart;
import com.itwt.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {


    @Autowired
    private ShoppingCartService shoppingCartService;


    /*
    * 添加购物车
    * */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("shoppingcart={}",shoppingCart);

        //获取当前用户的id
        Long currentId = BaseContext.getCurrentId();
        //设置当前用户的id
        shoppingCart.setUserId(currentId);
        //获取当前菜品的id
        Long dishId = shoppingCart.getDishId();
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //判断当前添加的是菜品还是套餐
        if (dishId != null){
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else {
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        //查询当前菜品或者套餐是否在购物车中
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);
        if (cartServiceOne != null){
            //如果已经存在，就在当前的数量加1
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number+1);
            shoppingCartService.updateById(cartServiceOne);
        }else {
            //如果不存在，设置创建时间
            shoppingCart.setCreateTime(LocalDateTime.now());
            //然后添加到购物车，数量默认为1
            shoppingCartService.save(shoppingCart);
            //统一结果，返回cartServiceOne
            cartServiceOne = shoppingCart;
        }
        return R.success(cartServiceOne);
    }


    /*
    * 查看购物车
    * */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        Long userId = BaseContext.getCurrentId();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);
        return R.success(shoppingCarts);
    }

    /*
    * 清空购物车
    * */
    @DeleteMapping("/clean")
    public R<String> clean(){
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        queryWrapper.eq(userId != null,ShoppingCart::getUserId,userId);
        //删除当前用户id的所有购物车数据
        shoppingCartService.remove(queryWrapper);
        return R.success("成功清空购物车");
    }

    /*
    * 减号功能实现
    * */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //只查询当前用户id的购物车
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        //代表数量减少的是菜品数量
        if (dishId != null){
            //通过dishId查出购物车菜品数据
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
            ShoppingCart dishCart = shoppingCartService.getOne(queryWrapper);
            //将查出来的数据-1
            dishCart.setNumber(dishCart.getNumber()-1);
            Integer number = dishCart.getNumber();
            //判断
            if (number > 0){
                //大于0更新
                shoppingCartService.updateById(dishCart);
            }else if (number == 0){
                //等于0删除
                shoppingCartService.removeById(dishCart.getId());
            }
           return R.success(dishCart);
        }
        if (setmealId != null){
            //通过setmealId查询购物车套餐数据
            queryWrapper.eq(ShoppingCart::getSetmealId,setmealId);
            ShoppingCart setmealCart = shoppingCartService.getOne(queryWrapper);
            //将查出来的数据-1
            setmealCart.setNumber(setmealCart.getNumber()-1);
            Integer number = setmealCart.getNumber();
            //判断
            if (number > 0){
                //大于0更新
                shoppingCartService.updateById(setmealCart);
            } else if (number == 0) {
                //等于0删除
                shoppingCartService.removeById(setmealCart.getId());
            }
            return R.success(setmealCart);
        }
        return R.error("系统繁忙，请稍后再试");
    }
}
