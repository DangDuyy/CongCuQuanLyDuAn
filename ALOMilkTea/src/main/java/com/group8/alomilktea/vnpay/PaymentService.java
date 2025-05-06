package com.group8.alomilktea.vnpay;

import com.group8.alomilktea.config.payment.VNPAYConfig;
import com.group8.alomilktea.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final VNPAYConfig vnPayConfig;
    public long amount = 0;
    public String fullAddress;
    public String shipingid;

    public PaymentDTO.VNPayResponse createVnPayPayment(HttpServletRequest request) {
        try {
            // Get parameters
            String amountParam = request.getParameter("amount");
            String address = request.getParameter("address");
            shipingid = request.getParameter("shippingId");
            
            // Validate parameters
            if (amountParam == null || amountParam.isEmpty()) {
                throw new IllegalArgumentException("Amount parameter is required");
            }
            if (address == null || address.isEmpty()) {
                throw new IllegalArgumentException("Address is required");
            }
            if (shipingid == null || shipingid.isEmpty()) {
                throw new IllegalArgumentException("Shipping ID is required");
            }

            // Store values for callback
            fullAddress = address;
            double amountDouble = Double.parseDouble(amountParam);
            amount = (long) (amountDouble * 100); // Convert to VNPay format (cents)

            // Create VNPay parameters
            Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();
            vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
            vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

            // Optional bank code
            String bankCode = request.getParameter("bankCode");
            if (bankCode != null && !bankCode.isEmpty()) {
                vnpParamsMap.put("vnp_BankCode", bankCode);
            }

            // Build payment URL
            String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
            String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
            String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
            String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

            return PaymentDTO.VNPayResponse.builder()
                    .code("00")
                    .message("Success")
                    .ketao(paymentUrl)
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create VNPay payment: " + e.getMessage());
        }
    }
}
