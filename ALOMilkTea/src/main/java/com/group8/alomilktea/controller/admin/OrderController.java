package com.group8.alomilktea.controller.admin;

import com.group8.alomilktea.entity.Order;
import com.group8.alomilktea.service.IOrderService;
import com.group8.alomilktea.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderController {
    @Autowired
    private IOrderService orderService;

    @Autowired
    private IUserService userService;

    @GetMapping("/list")
    public ResponseEntity<?> listOrders(
            @RequestParam(name = "page", defaultValue = "1") Integer page) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Get order counts by status
            response.put("newOrders", orderService.countByStatus("New"));
            response.put("pendingOrders", orderService.countByStatus("Pending"));
            response.put("shippingOrders", orderService.countByStatus("Shipping"));
            response.put("deliveredOrders", orderService.countByStatus("Delivered"));
            response.put("cancelledOrders", orderService.countByStatus("Cancelled"));
            
            // Get paginated orders
            Page<Order> orderPage = orderService.getAll(page);
            response.put("orders", orderPage.getContent());
            response.put("totalPages", orderPage.getTotalPages());
            response.put("currentPage", page);
            response.put("totalOrders", orderService.count());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch orders: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Integer orderId) {
        try {
            Order order = orderService.findById(orderId);
            if (order != null) {
                return ResponseEntity.ok(order);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch order: " + e.getMessage()));
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestParam String newStatus) {
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate status transition
            if (!isValidStatusTransition(order.getStatus(), newStatus)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status transition from " + order.getStatus() + " to " + newStatus));
            }

            order.setStatus(newStatus);
            orderService.save(order);

            return ResponseEntity.ok(Map.of(
                "message", "Order status updated successfully",
                "order", order
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to update order status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable Integer orderId) {
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }

            orderService.deleteById(orderId);
            return ResponseEntity.ok(Map.of("message", "Order deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to delete order: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUser(@PathVariable Integer userId) {
        try {
            List<Order> orders = orderService.findOder(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch user orders: " + e.getMessage()));
        }
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // Add your status transition validation logic here
        // Example: Only allow specific transitions
        switch (currentStatus) {
            case "New":
                return newStatus.equals("Pending");
            case "Pending":
                return newStatus.equals("Shipping");
            case "Shipping":
                return newStatus.equals("Delivered") || newStatus.equals("Cancelled");
            default:
                return false;
        }
    }
}
