package com.eticaret.eticaret_api.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private Long userId;
    private String username;
    private LocalDateTime orderDate;
    private Double totalAmount;
    private List<OrderItemDto> items;
}