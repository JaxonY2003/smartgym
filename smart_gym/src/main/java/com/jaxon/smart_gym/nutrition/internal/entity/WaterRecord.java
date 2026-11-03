package com.jaxon.smart_gym.nutrition.internal.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("water_record")
public class WaterRecord extends BaseEntity {

    private Long userId;
    private LocalDateTime recordedAt;
    private Integer amountMl;
    private String source;
}
