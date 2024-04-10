package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 学霸天梯榜
 * </p>
 *
 * @author Etsuya
 * @since 2024-04-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("points_board") //TODO 这张表不存在不会出错？
public class PointsBoard implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 榜单id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 学生id
     */
    private Long userId;

    /**
     * 积分值
     */
    private Integer points;

}
