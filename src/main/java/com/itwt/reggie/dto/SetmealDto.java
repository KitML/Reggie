package com.itwt.reggie.dto;

import com.itwt.reggie.entity.Setmeal;
import com.itwt.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
