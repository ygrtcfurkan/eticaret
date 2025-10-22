package com.eticaret.eticaret_api.controller;

import com.eticaret.eticaret_api.dto.AddItemRequestDto;
import com.eticaret.eticaret_api.dto.ShoppingCartDto;
import com.eticaret.eticaret_api.entity.User;
import com.eticaret.eticaret_api.repository.UserRepository;
import com.eticaret.eticaret_api.service.ShoppingCartService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class ShoppingCartController {

    private static final String ERROR_KEY = "error";

    private final ShoppingCartService shoppingCartService;
    private final UserRepository userRepository;

    @Autowired
    public ShoppingCartController(ShoppingCartService shoppingCartService, UserRepository userRepository) {
        this.shoppingCartService = shoppingCartService;
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public ResponseEntity<Object> addProductToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddItemRequestDto addItemRequestDto) {
        try {
            User currentUser = findUserByUsername(userDetails.getUsername());
            shoppingCartService.addProductToCart(
                    currentUser.getId(),
                    addItemRequestDto.getProductId(),
                    addItemRequestDto.getQuantity());

            Optional<ShoppingCartDto> cartDtoOpt = shoppingCartService.getCartDtoByUserId(currentUser.getId());
            if (cartDtoOpt.isPresent()) {
                return new ResponseEntity<>(cartDtoOpt.get(), HttpStatus.OK);
            } else {
                Map<String, String> errorResponse = Map.of(ERROR_KEY, "Could not retrieve cart after adding item.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (EntityNotFoundException enfe) {
            Map<String, String> errorResponse = Map.of(ERROR_KEY, enfe.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (RuntimeException ex) {
            Map<String, String> errorResponse = Map.of(ERROR_KEY, ex.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<Object> getCartByUserId(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = findUserByUsername(userDetails.getUsername());
            Optional<ShoppingCartDto> cartDtoOpt = shoppingCartService.getCartDtoByUserId(currentUser.getId());

            if (cartDtoOpt.isPresent()){
                return new ResponseEntity<>(cartDtoOpt.get(), HttpStatus.OK);
            } else {
                Map<String, String> errorResponse = Map.of(ERROR_KEY, "Shopping cart not found for this user.");
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
        } catch (EntityNotFoundException enfe) {
            Map<String, String> errorResponse = Map.of(ERROR_KEY, enfe.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<Object> removeProductFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        try {
            User currentUser = findUserByUsername(userDetails.getUsername());
            shoppingCartService.removeProductFromCart(currentUser.getId(), productId);
            Optional<ShoppingCartDto> cartDtoOpt = shoppingCartService.getCartDtoByUserId(currentUser.getId());
            if (cartDtoOpt.isPresent()) {
                return new ResponseEntity<>(cartDtoOpt.get(), HttpStatus.OK);
            } else {
                Map<String, String> errorResponse = Map.of(ERROR_KEY, "Could not retrieve cart after removing item.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (EntityNotFoundException enfe) {
            Map<String, String> errorResponse = Map.of(ERROR_KEY, enfe.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (RuntimeException ex) {
            Map<String, String> errorResponse = Map.of(ERROR_KEY, ex.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username + ". Please ensure the user exists."));
    }
}