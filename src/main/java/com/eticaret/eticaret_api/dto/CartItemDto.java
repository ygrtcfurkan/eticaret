package com.eticaret.eticaret_api.dto;

import lombok.Data;

@Data
public class CartItemDto {
    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
}