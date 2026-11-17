package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuisineClassifyResult {
    /** 本次待分类（未分类）的菜谱总数 */
    private int total;
    /** 成功分类数量 */
    private int classified;
    /** 失败/未分类数量 */
    private int failed;
    /** 汇总说明 */
    private String message;
}
