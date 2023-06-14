package com.itwt.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itwt.reggie.common.R;
import com.itwt.reggie.dto.DishDto;
import com.itwt.reggie.dto.SetmealDto;
import com.itwt.reggie.entity.Category;
import com.itwt.reggie.entity.Dish;
import com.itwt.reggie.entity.Setmeal;
import com.itwt.reggie.entity.SetmealDish;
import com.itwt.reggie.service.CategoryService;
import com.itwt.reggie.service.DishService;
import com.itwt.reggie.service.SetmealDishService;
import com.itwt.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    /*
    * 新增套餐
    * */
    @PostMapping
    //设置allEntries为true，清空缓存名称为setmealCache的所有缓存
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息,{}",setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐信息成功");
    }
    /*
    * 分页查询
    * */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>();
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.like(name != null,Setmeal::getName,name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo,queryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageInfo,setmealDtoPage,"records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map((item)->{
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item,setmealDto);
            Long categoryId = item.getCategoryId();//分类id
            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }

            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPage.setRecords(list);
        return R.success(setmealDtoPage);
    }

    /*
     * 菜品批量启售停售
     * */
    @PostMapping("/status/{status}")
    //设置allEntries为true，清空缓存名称为setmealCache的所有缓存
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> status(@PathVariable Integer status,@RequestParam List<Long>  ids){
        log.info("status:{},ids:{}",status,ids);
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(ids != null,Setmeal::getId,ids);
        updateWrapper.set(Setmeal::getStatus,status);
        setmealService.update(updateWrapper);
        return R.success("批量操作成功");
    }

    /*
    * 删除套餐
    * */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("ids,{}",ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功");
    }

    /*
    * 套餐展示
    * */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId +'_'+#setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal){
        //条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus()!= null,Setmeal::getStatus,1);
        //排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }

    /*
    * 套餐修改之数据回显
    * */
    @GetMapping("/{id}")
    public R<Setmeal> getById(@PathVariable Long id){
        Setmeal setmeal = setmealService.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        //拷贝数据
        BeanUtils.copyProperties(setmeal,setmealDto);
        //条件构造器
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        //根据setmealId查询具体的setmealDish
        queryWrapper.eq(SetmealDish::getSetmealId,id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        //设置属性
        setmealDto.setSetmealDishes(list);
        return R.success(setmealDto);
    }

    /*
    * 套餐修改之保存修改
    * */
    @PutMapping
    //设置allEntries为true，清空缓存名称为setmealCache的所有缓存
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<Setmeal> updateDish(@RequestBody SetmealDto setmealDto){
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        Long setmealDtoId = setmealDto.getId();
        //先根据id把setmealDish表里对应的数据删除
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealDtoId);
        setmealDishService.remove(queryWrapper);
        //重新添加新的数据
        setmealDishes = setmealDishes.stream().map((item)->{
            //item属性没有，设置一下
            item.setSetmealId(setmealDtoId);
            return item;
        }).collect(Collectors.toList());
        //更新套餐数据
        setmealService.updateById(setmealDto);
        setmealDishService.saveBatch(setmealDishes);
        return R.success(setmealDto);
    }

    /*
    * 点击图片显示详情
    * */
    @GetMapping("/dish/{id}")
    public R<List<DishDto>> showSetmealDish(@PathVariable Long id){
        //条件构造器
        LambdaQueryWrapper<SetmealDish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //利用获得的setmealId
        dishLambdaQueryWrapper.eq(SetmealDish::getSetmealId,id);
        //查询数据
        List<SetmealDish> records = setmealDishService.list(dishLambdaQueryWrapper);
        List<DishDto> list = records.stream().map((item)->{
            DishDto dishDto = new DishDto();
            //拷贝数据
            BeanUtils.copyProperties(item,dishDto);
            //查询对应菜品id
            Long dishId = item.getDishId();
            //根据菜品id获取菜品数据
            Dish dish = dishService.getById(dishId);
            BeanUtils.copyProperties(dish,dishDto);
            return dishDto;
        }).collect(Collectors.toList());
      return R.success(list);
    }
}
