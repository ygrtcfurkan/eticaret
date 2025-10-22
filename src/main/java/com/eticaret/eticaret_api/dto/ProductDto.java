package com.eticaret.eticaret_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class ProductDto {

    private Long id;

    @NotBlank(message = "{product.name.notblank}")
    @Size(min = 2, max = 100, message = "{product.name.size}")
    private String name;

    @Size(max = 500, message = "{product.description.size}")
    private String description;

    @NotNull(message = "{product.price.notnull}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{product.price.decimalmin}")
    private Double price;

    private Integer stock;

}