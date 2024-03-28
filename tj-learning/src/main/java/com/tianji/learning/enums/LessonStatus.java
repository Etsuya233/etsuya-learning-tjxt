package com.tianji.learning.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tianji.common.enums.BaseEnum;
import lombok.Getter;

@Getter
public enum LessonStatus implements BaseEnum {
    NOT_BEGIN(0, "未学习"),
    LEARNING(1, "学习中"),
    FINISHED(2, "已学完"),
    EXPIRED(3, "已过期"),
    ;
    @JsonValue //注解告诉 Jackson 序列化器在将枚举值序列化为 JSON 字符串时，应该使用 value 方法返回的值。
    @EnumValue //MP的注解，MP从DB查询数据是差的是值，MP自动转化为枚举类
    int value;
    String desc;

    LessonStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    //这个注解：是 Jackson 库中的注解，用于指示一个静态工厂方法或构造函数作为反序列化时使用的创建器。
    //它告诉 Jackson 序列化器/反序列化器使用委托模式来确定如何构造枚举值，即根据传入的参数值来选择相应的枚举值。
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static LessonStatus of(Integer value){
        if (value == null) {
            return null;
        }
        for (LessonStatus status : values()) {
            if (status.equalsValue(value)) {
                return status;
            }
        }
        return null;
    }
}
