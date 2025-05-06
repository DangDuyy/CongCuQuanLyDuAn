package com.group8.alomilktea.controller.web;

import com.group8.alomilktea.entity.*;
import com.group8.alomilktea.model.CartModel;
import com.group8.alomilktea.model.ProductDetailDTO;
import com.group8.alomilktea.repository.CartRepository;
import com.group8.alomilktea.service.ICartService;
import com.group8.alomilktea.service.IProductService;
import com.group8.alomilktea.service.IUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Autowired
    private IUserService userService;
    
    @Autowired
    private ICartService cartService;
    
    @Autowired
    private IProductService productService;
    
    @Autowired
    private CartRepository cartRepository;

    @GetMapping("/items")
    public ResponseEntity<?> getCartItems(@RequestParam("user_id") Integer userId) {
        try {
            List<Cart> cartItems = cartService.findByUserId(userId);
            double total = cartItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getProduct().getProductDetails().get(0).getPrice())
                .sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", cartItems);
            response.put("total", total);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch cart items: " + e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @RequestParam("user_id") Integer userId,
            @RequestParam("pro_id") Integer productId,
            @RequestParam("size") String size,
            @RequestParam("quantity") Integer quantity) {
        
        try {
            User user = userService.findById(userId);
            Product product = productService.findById(productId);
            
            if (user == null || product == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "User or product not found"));
            }

            CartKey cartKey = new CartKey(userId, productId, size);
            Cart cartItem = cartRepository.findById(cartKey)
                .orElse(new Cart(cartKey, quantity, product.getProductDetails().get(0).getPrice(), user, product));
            
            if (cartRepository.existsById(cartKey)) {
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
            }
            
            cartRepository.save(cartItem);

            return ResponseEntity.ok(Map.of(
                "message", "Product added to cart successfully",
                "cartItem", cartItem
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to add to cart: " + e.getMessage()));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeItem(
            @RequestParam("user_id") Integer userId,
            @RequestParam("pro_id") Integer productId,
            @RequestParam("size") String size) {

        try {
            CartKey cartKey = new CartKey(userId, productId, size);
            if (cartService.existsById(cartKey)) {
                cartService.deleteById(cartKey);
                return ResponseEntity.ok(Map.of("message", "Item removed successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Item not found in cart"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to remove item: " + e.getMessage()));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateItemQuantity(
            @RequestParam("user_id") Integer userId,
            @RequestParam("pro_id") Integer productId,
            @RequestParam("size") String size,
            @RequestParam("quantity") Integer quantity) {

        try {
            CartKey cartKey = new CartKey(userId, productId, size);
            Optional<Cart> cartItemOpt = cartRepository.findById(cartKey);

            if (cartItemOpt.isPresent()) {
                Cart cartItem = cartItemOpt.get();
                cartItem.setQuantity(quantity);
                cartRepository.save(cartItem);

                return ResponseEntity.ok(Map.of(
                    "message", "Quantity updated successfully",
                    "cartItem", cartItem
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Item not found in cart"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to update quantity: " + e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(@RequestParam("user_id") Integer userId) {
        try {
            cartService.clearCart(userId);
            return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to clear cart: " + e.getMessage()));
        }
    }
}




