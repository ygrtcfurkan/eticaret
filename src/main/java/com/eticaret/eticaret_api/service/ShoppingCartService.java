package com.eticaret.eticaret_api.service;

import com.eticaret.eticaret_api.dto.CartItemDto;
import com.eticaret.eticaret_api.dto.ShoppingCartDto;
import com.eticaret.eticaret_api.entity.CartItem;
import com.eticaret.eticaret_api.entity.Product;
import com.eticaret.eticaret_api.entity.ShoppingCart;
import com.eticaret.eticaret_api.repository.CartItemRepository;
import com.eticaret.eticaret_api.repository.ProductRepository;
import com.eticaret.eticaret_api.repository.ShoppingCartRepository;
import com.eticaret.eticaret_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShoppingCartService {
    private final ShoppingCartRepository shoppingCartRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    @Autowired
    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository,
                               ProductRepository productRepository,
                               UserRepository userRepository,
                               CartItemRepository cartItemRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public void addProductToCart(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

        ShoppingCart cart = shoppingCartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping cart not found for user id: " + userId));

        Optional<CartItem> existingCartItemOpt = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId)).findFirst();

        if (existingCartItemOpt.isPresent()) {
            CartItem existingCartItem = existingCartItemOpt.get();
            existingCartItem.setQuantity(existingCartItem.getQuantity() + quantity);
            cartItemRepository.save(existingCartItem);
        } else {
            CartItem newCartItem = new CartItem();
            newCartItem.setShoppingCart(cart);
            newCartItem.setProduct(product);
            newCartItem.setQuantity(quantity);
            cart.getCartItems().add(newCartItem);
        }
        shoppingCartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Optional<ShoppingCartDto> getCartDtoByUserId(Long userId) {
        return shoppingCartRepository.findByUserId(userId)
                .map(this::convertToDto);
    }

    @Transactional
    public void removeProductFromCart(Long userId, Long productId) {
        ShoppingCart cart = shoppingCartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Shopping cart not found for user id: " + userId));

        CartItem itemToRemove = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Product with id: " + productId + " not found in cart"));

        cart.getCartItems().remove(itemToRemove);
        shoppingCartRepository.save(cart);
    }

    private ShoppingCartDto convertToDto(ShoppingCart cart) {
        ShoppingCartDto dto = new ShoppingCartDto();
        dto.setId(cart.getId());
        if (cart.getUser() != null) {
            dto.setUserId(cart.getUser().getId());
        }
        // CartItem'ları CartItemDto'lara çevir
        dto.setItems(cart.getCartItems().stream()
                .map(this::convertCartItemToDto)
                .collect(Collectors.toSet()));
        dto.setTotalAmount(calculateTotalAmount(cart));
        return dto;
    }

    private CartItemDto convertCartItemToDto(CartItem cartItem) {
        CartItemDto dto = new CartItemDto();
        dto.setQuantity(cartItem.getQuantity());
        if (cartItem.getProduct() != null) {
            dto.setProductId(cartItem.getProduct().getId());
            dto.setProductName(cartItem.getProduct().getName());
            dto.setPrice(cartItem.getProduct().getPrice());
        }
        return dto;
    }

    private Double calculateTotalAmount(ShoppingCart cart) {
        if (cart.getCartItems() == null) {
            return 0.0;
        }
        return cart.getCartItems().stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }
}