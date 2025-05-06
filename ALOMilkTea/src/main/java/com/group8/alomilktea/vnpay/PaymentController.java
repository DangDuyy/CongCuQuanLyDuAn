package com.group8.alomilktea.vnpay;

import com.group8.alomilktea.config.payment.VNPAYConfig;
import com.group8.alomilktea.config.response.ResponseObject;
import com.group8.alomilktea.entity.*;
import com.group8.alomilktea.service.*;
import com.group8.alomilktea.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final IUserService userService;
    private final ICartService cartService;
    private final IOrderService orderService;
    private final IOrderDetailService orderDetailService;
    private final IShipmentCompany shipmentCompany;
    private final VNPAYConfig vnPayConfig;

    @PostMapping("/vnpay/create")
    public ResponseEntity<?> createVnPayPayment(
            @RequestParam Double amount,
            @RequestParam String address,
            @RequestParam String shippingId,
            HttpServletRequest request) {
        try {
            PaymentDTO.VNPayResponse response = paymentService.createVnPayPayment(request);
            
            // Store payment info for callback
            paymentService.amount = amount.intValue();
            paymentService.fullAddress = address;
            paymentService.shipingid = shippingId;
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to create VNPay payment: " + e.getMessage()));
        }
    }

    @RequestMapping(value = "/vnpay/callback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> vnPayCallback(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "failed");
        
        try {
            // Get all parameters from the request
            Map<String, String> fields = new HashMap<>();
            Enumeration<String> params = request.getParameterNames();
            while (params.hasMoreElements()) {
                String fieldName = params.nextElement();
                String fieldValue = request.getParameter(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    fields.put(fieldName, fieldValue);
                }
            }

            // Remove checksum from fields
            String vnpSecureHash = fields.remove("vnp_SecureHash");
            
            // Check checksum
            if (vnpSecureHash != null) {
                // Generate checksum from received fields
                String signValue = VNPayUtil.getPaymentURL(fields, false);
                String checkSum = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), signValue);
                
                if (checkSum.equals(vnpSecureHash)) {
                    String vnpResponseCode = fields.get("vnp_ResponseCode");
                    
                    if ("00".equals(vnpResponseCode)) {
                        User user = userService.getUserLogged();
                        List<Cart> cartItems = cartService.findByUserId(user.getUserId());
                        
                        if (!cartItems.isEmpty()) {
                            // Create order
                            Order order = createOrder(user, cartItems);
                            
                            // Create order details
                            createOrderDetails(order, cartItems);
                            
                            // Clear cart
                            cartService.clearCart(user.getUserId());
                            
                            response.put("status", "success");
                            response.put("message", "Payment successful");
                            response.put("orderId", order.getOrderId());
                            response.put("orderStatus", order.getStatus());
                            response.put("amount", order.getTotal());
                            
                            return ResponseEntity.ok(response);
                        } else {
                            response.put("message", "Cart is empty");
                            return ResponseEntity.badRequest().body(response);
                        }
                    } else {
                        response.put("message", "Payment failed with code: " + vnpResponseCode);
                        return ResponseEntity.badRequest().body(response);
                    }
                } else {
                    response.put("message", "Invalid signature");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                response.put("message", "Missing secure hash");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("message", "Payment processing failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/cod")
    public ResponseEntity<?> processCashOnDelivery(
            @RequestParam Double amount,
            @RequestParam String province,
            @RequestParam String city,
            @RequestParam String commune,
            @RequestParam String address,
            @RequestParam String shippingId) {
        try {
            User user = userService.getUserLogged();
            List<Cart> cartItems = cartService.findByUserId(user.getUserId());
            
            if (cartItems.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cart is empty"));
            }

            String fullAddress = String.format("%s, %s, %s, %s", address, commune, city, province);
            ShipmentCompany shipment = shipmentCompany.findById(Integer.parseInt(shippingId));
            
            if (shipment == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid shipping method"));
            }

            // Create order
            Order order = new Order();
            order.setUser(user);
            order.setCurrency("VND");
            order.setTotal(amount);
            order.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            order.setPaymentMethod("CashOnDelivery");
            order.setStatus("Pending");
            order.setDeliAddress(fullAddress);
            order.setShipmentCompany(shipment);
            orderService.save(order);

            // Create order details
            createOrderDetails(order, cartItems);
            
            // Clear cart
            cartService.clearCart(user.getUserId());

            return ResponseEntity.ok(Map.of(
                "message", "COD order created successfully",
                "order", order
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to process COD order: " + e.getMessage()));
        }
    }

    private Order createOrder(User user, List<Cart> cartItems) {
        Order order = new Order();
        order.setUser(user);
        order.setCurrency("VND");
        order.setTotal((double) paymentService.amount);
        order.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        order.setPaymentMethod("VNPay");
        order.setStatus("Pending");
        order.setDeliAddress(paymentService.fullAddress);
        order.setShipmentCompany(shipmentCompany.findById(Integer.parseInt(paymentService.shipingid)));
        return orderService.save(order);
    }

    private void createOrderDetails(Order order, List<Cart> cartItems) {
        for (Cart cart : cartItems) {
            if (cart.getId() == null || cart.getProduct() == null || cart.getId().getSize() == null) {
                continue;
            }

            OrderDetailKey orderDetailKey = new OrderDetailKey(
                order.getOrderId(),
                cart.getProduct().getProId(),
                cart.getId().getSize()
            );

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setId(orderDetailKey);
            orderDetail.setProduct(cart.getProduct());
            orderDetail.setQuantity(cart.getQuantity());
            orderDetailService.save(orderDetail);
        }
    }
}



