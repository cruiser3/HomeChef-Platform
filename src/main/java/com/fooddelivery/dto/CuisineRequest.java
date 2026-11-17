package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuisineRequest {

    @NotBlank(message = "菜系名称不能为空")
    @Size(max = 50, message = "菜系名称不能超过 50 字")
    private String name;
}
