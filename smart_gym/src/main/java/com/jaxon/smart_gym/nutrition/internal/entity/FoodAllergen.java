package com.jaxon.smart_gym.nutrition.internal.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodAllergen {

    private Long foodId;
    private Long allergenId;
    private String source;
}
