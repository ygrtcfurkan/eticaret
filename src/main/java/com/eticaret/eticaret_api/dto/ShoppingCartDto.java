package com.eticaret.eticaret_api.dto;

import lombok.Data;
import java.util.Set;

@Data
public class ShoppingCartDto {
    private Long id;
    private Long userId;
    private Set<CartItemDto> items;
    private Double totalAmount;
}