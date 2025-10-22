package com.eticaret.eticaret_api.service;

import com.eticaret.eticaret_api.dto.OrderDto;
import com.eticaret.eticaret_api.dto.OrderItemDto;
import com.eticaret.eticaret_api.entity.*;
import com.eticaret.eticaret_api.repository.OrderRepository;
import com.eticaret.eticaret_api.repository.ProductRepository;
import com.eticaret.eticaret_api.repository.ShoppingCartRepository;
import com.eticaret.eticaret_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        ShoppingCartRepository shoppingCartRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }
    @Transactional
    public Order createOrderFromCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        ShoppingCart cart = shoppingCartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping cart not found for user id: " + userId));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cannot create order from an empty cart.");
        }
        Map<Long, Integer> productRequestedQuantities = new HashMap<>();
        for (CartItem cartItem : cart.getCartItems()) {
            productRequestedQuantities.merge(cartItem.getProduct().getId(), cartItem.getQuantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : productRequestedQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId + " during stock check."));
            if (product.getStock() < requestedQuantity) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() +
                        ". Requested: " + requestedQuantity + ", Available: " + product.getStock());
            }
        }

        Order newOrder = new Order();
        newOrder.setUser(user);
        newOrder.setOrderDate(LocalDateTime.now());
        double totalAmount = 0.0;

        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem();
            Product product = cartItem.getProduct();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            newOrder.addOrderItem(orderItem);
            totalAmount += (product.getPrice() * cartItem.getQuantity());
        }
        newOrder.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(newOrder);

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            int orderedQuantity = cartItem.getQuantity();
            product.setStock(product.getStock() - orderedQuantity);
            productRepository.save(product);
        }

        cart.getCartItems().clear();
        shoppingCartRepository.save(cart);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersDtoByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        return orders.stream().map(this::convertToDto).collect(Collectors.toList());
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
            dto.setItems(order.getOrderItems().stream().map(this::convertOrderItemToDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private OrderItemDto convertOrderItemToDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
        }
        return dto;
    }
}