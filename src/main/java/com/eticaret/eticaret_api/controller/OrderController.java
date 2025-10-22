package com.eticaret.eticaret_api.controller;

import com.eticaret.eticaret_api.dto.OrderDto;
import com.eticaret.eticaret_api.dto.OrderItemDto;
import com.eticaret.eticaret_api.entity.Order;
import com.eticaret.eticaret_api.entity.User;
import com.eticaret.eticaret_api.repository.UserRepository;
import com.eticaret.eticaret_api.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @Autowired
    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = findUserByUsername(userDetails.getUsername());
            Order createdOrderEntity = orderService.createOrderFromCart(currentUser.getId());
            OrderDto createdOrderDto = convertToDto(createdOrderEntity);
            return new ResponseEntity<>(createdOrderDto, HttpStatus.CREATED);

        } catch (EntityNotFoundException enfe) {
            Map<String, String> errorResponse = Map.of("error", enfe.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (RuntimeException ex) {
            Map<String, String> errorResponse = Map.of("error", ex.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<?> getOrdersByUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = findUserByUsername(userDetails.getUsername());
            List<OrderDto> orders = orderService.getOrdersDtoByUserId(currentUser.getId());
            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (EntityNotFoundException enfe) {
            Map<String, String> errorResponse = Map.of("error", enfe.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUsername(order.getUser().getUsername());
        }
        if (order.getOrderItems() != null) {
            dto.setItems(order.getOrderItems().stream()
                    .map(item -> { // OrderItemDto mapping
                        OrderItemDto itemDto = new OrderItemDto();
                        itemDto.setQuantity(item.getQuantity());
                        itemDto.setPrice(item.getPrice());
                        if (item.getProduct() != null) {
                            itemDto.setProductId(item.getProduct().getId());
                            itemDto.setProductName(item.getProduct().getName());
                        }
                        return itemDto;
                    }).collect(Collectors.toList()));
        }
        return dto;
    }
}