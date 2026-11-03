package com.jaxon.smart_gym.nutrition.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.jaxon.smart_gym.nutrition.internal.entity.FoodAllergen;

@Mapper
public interface FoodAllergenMapper {

    @Select({
            "<script>",
            "SELECT food_id, allergen_id, source",
            "FROM food_allergen",
            "WHERE food_id IN",
            "<foreach collection='foodIds' item='foodId' open='(' separator=',' close=')'>",
            "#{foodId}",
            "</foreach>",
            "ORDER BY food_id ASC, allergen_id ASC",
            "</script>"
    })
    List<FoodAllergen> selectByFoodIds(@Param("foodIds") List<Long> foodIds);
}
