package com.itwt.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itwt.reggie.common.R;
import com.itwt.reggie.dto.DishDto;
import com.itwt.reggie.entity.Category;
import com.itwt.reggie.entity.Dish;
import com.itwt.reggie.entity.DishFlavor;
import com.itwt.reggie.service.CategoryService;
import com.itwt.reggie.service.DishFlavorService;
import com.itwt.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/*
 * 菜品管理
 * */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /*
     * 新增菜品
     * */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
        String key = "dish_" + dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        return R.success("新增成功");
    }

    /*
     * 菜品管理分页查询
     * */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        Page<Dish> pageinfo = new Page(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        queryWrapper.like(StringUtils.isNotEmpty(name), Dish::getName, name);
        dishService.page(pageinfo, queryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageinfo, dishDtoPage, "records");
        List<Dish> records = pageinfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long categoryId = item.getCategoryId();//分类id
            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    /*
    * 根据id查询菜品信息和对应口味信息
    * */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /*
    * 修改保存菜品信息
    * */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);
        String key = "dish_" + dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        return R.success("保存成功");
    }

   /* *//*
    * 菜品启售停售
    * *//*
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable Integer status, Long  ids){
        log.info("status:{},ids:{}",status,ids);
        Dish dish = dishService.getById(ids);
        if (dish != null){
            dish.setStatus(status);
            dishService.updateById(dish);
            return R.success("售卖状态修改成功");
        }
        return R.error("售卖状态修改失败");
    }*/

    /*
     * 菜品批量启售停售
     * */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status,@RequestParam List<Long>  ids){
        log.info("status:{},ids:{}",status,ids);
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(ids != null,Dish::getId,ids);
        updateWrapper.set(Dish::getStatus,status);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId,ids);
        List<Dish> list = dishService.list(queryWrapper);
        for (Dish dish : list){
            String key = "dish_" + dish.getCategoryId()+"_1";
            redisTemplate.delete(key);
        }
        dishService.update(updateWrapper);
        return R.success("批量操作成功");
    }

    /*
    * 删除菜品
    * */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        dishService.deleteWithFlavor(ids);
        return R.success("删除成功");
    }


    /*
    * 根据条件查询对应的菜品信息
    * */
   /* @GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus,1);
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);
        return R.success(list);
    }*/
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList;
        String key = "dish_" +dish.getCategoryId() + "_" +dish.getStatus();
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        //如果有，则直接返回
        if (dishDtoList !=null){
            return R.success(dishDtoList);
        }
        //如果无，则查询
        //条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus,1);
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);
        log.info("查询到的菜品信息list:{}",list);
        //item就是list中的每一条数据，等于遍历
         dishDtoList = list.stream().map((item)->{
            //创建一个dishDto对象
            DishDto dishDto = new DishDto();
            //将item的属性全部copy到dishDto里面
            BeanUtils.copyProperties(item,dishDto);
            //由于dish表中没有categoryName属性，只存了categoryId
            Long categoryId = item.getCategoryId();
            //所以根据categoryId查询对应的category
            Category category = categoryService.getById(categoryId);
            if (category != null){
                //然后去除categoryName，赋值给dishDto
                dishDto.setCategoryName(category.getName());
            }
            //接下来获取菜品id，根据菜品id去dishFlavor表中查询对应的口味，并赋值给dishDto
            Long itemId = item.getId();
            //条件构造器
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            //查询条件
            lambdaQueryWrapper.eq(itemId != null, DishFlavor::getDishId,itemId);
            //根据菜品id，查询到菜品口味
            List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper);
            //赋给dishDto的对应属性
            dishDto.setFlavors(flavors);
            //将dishDto作为结果返回
            return dishDto;
             //将所有返回结果收集封装成List
        }).collect(Collectors.toList());
         //将查询的结果让Redis缓存，设置存活时间为60分钟
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);
        return R.success(dishDtoList);
    }
}
