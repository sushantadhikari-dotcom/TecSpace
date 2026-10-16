package com.tecspace.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Talks to eSewa ePay v2 (test environment by default). */
@Component
public class EsewaClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};

    private final RestClient http = RestClient.create();

    @Value("${tecspace.esewa.secret-key}")
    private String secretKey;

    @Value("${tecspace.esewa.product-code}")
    private String productCode;

    @Value("${tecspace.esewa.form-url}")
    private String formUrl;

    @Value("${tecspace.esewa.status-url}")
    private String statusUrl;

    @Value("${tecspace.esewa.success-url}")
    private String successUrl;

    @Value("${tecspace.esewa.failure-url}")
    private String failureUrl;

    public String formUrl() {
        return formUrl.trim();
    }

    /** 12000.00 -> "12000". The SAME text is used in the form, the signature and the status check. */
    public static String amountText(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    /** The hidden fields the payer's browser must POST to eSewa. */
    public Map<String, String> formFields(String transactionUuid, BigDecimal amount) {
        String total = amountText(amount);
        Map<String, String> f = new LinkedHashMap<>();
        f.put("amount", total);
        f.put("tax_amount", "0");
        f.put("total_amount", total);
        f.put("transaction_uuid", transactionUuid);
        f.put("product_code", productCode.trim());
        f.put("product_service_charge", "0");
        f.put("product_delivery_charge", "0");
        f.put("success_url", successUrl.trim());
        f.put("failure_url", failureUrl.trim());
        f.put("signed_field_names", "total_amount,transaction_uuid,product_code");
        f.put("signature", sign("total_amount=" + total + ",transaction_uuid=" + transactionUuid
                + ",product_code=" + productCode.trim()));
        return f;
    }

    /** Asks eSewa directly for the real status. Never trust the redirect alone. */
    public Map<String, Object> status(String transactionUuid, BigDecimal amount) {
        return http.get()
                .uri(statusUrl.trim() + "?product_code={p}&total_amount={a}&transaction_uuid={u}",
                        productCode.trim(), amountText(amount), transactionUuid)
                .retrieve()
                .body(MAP);
    }

    private String sign(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign eSewa request", e);
        }
    }
}