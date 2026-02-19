package com.warehouseManagement.demo.services;

import com.warehouseManagement.demo.OfferServiceController;
import com.warehouseManagement.demo.entity.*;
import com.warehouseManagement.demo.repo.CartRepository;
import com.warehouseManagement.demo.repo.PaymentOrderRepository;
import com.warehouseManagement.demo.repo.StoreRepository;
import com.warehouseManagement.demo.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseService {



    public class CheckoutRequest {
        private int paymentId;
        private String userEmail;
        private long price;

        public CheckoutRequest(int paymentId, String userEmail,long price) {
            this.paymentId = paymentId;
            this.userEmail = userEmail;
            this.price = price;
        }

        // getters and setters
        public int getPaymentId() { return paymentId; }
        public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        public void setPrice(long price) { this.price = price; }
        public double getPrice() { return price; }
    }

    public ResponseEntity<HashMap> callPaymentServiceToCreateStripeCheckout(Payment payment, User user,long price) {
        String paymentServiceURL = "http://payment-service/payment/create-checkout-session";


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        CheckoutRequest checkoutRequest = new CheckoutRequest(payment.getPaymentId(),user.getEmail(),price);

        HttpEntity<CheckoutRequest> requestEntity = new HttpEntity<>(checkoutRequest, headers);

        // Send POST request
        ResponseEntity<HashMap> response = restTemplate.postForEntity(paymentServiceURL,requestEntity,HashMap.class);

        return response;

    }

    @Autowired
    OrderService orderService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    PaymentOrderRepository paymentOrderRepository;

    @Autowired
    RestTemplate restTemplate;


    /**
     *
     *
     *
     * **/

    public String handlePurchasingAllCarts(int userId) throws RuntimeException{
        System.out.println(orderService.testOrderingu());

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(userId);

        List<Cart> allCarts = cartRepository.findByStore(store);
        System.out.println("to sie wywolalo");
        Payment payment = orderService.placeOrder(store.getId(),allCarts);


        long price = calculateZlotyToGrosze(calculateTotalForOrdersGrosze(payment));


        ResponseEntity<HashMap> paymentCheckoutResponse = callPaymentServiceToCreateStripeCheckout(payment,user,price);

        if (paymentCheckoutResponse.getStatusCode().is2xxSuccessful()) {
            // Success (200, 201, etc.)
            Map<String, String> body = paymentCheckoutResponse.getBody();
            String checkoutUrl = (String) body.get("stripe-checkout"); // Extract your variable
            return "redirect:"+ checkoutUrl;
        } else {
            throw new RuntimeException("Received unexpected status: " + paymentCheckoutResponse.getStatusCode());
        }
    }

    private long calculateZlotyToGrosze(BigDecimal price) {
        if (price == null) return 0L;

        // Multiply by 100 and get the long value
        return price.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private BigDecimal calculateTotalForOrdersGrosze(Payment payment){
        List<PaymentOrder> listOfOrdersForPayment = paymentOrderRepository.findByPayment(payment);
        BigDecimal totalInPln = BigDecimal.valueOf(0);
        for(PaymentOrder paymentOrder : listOfOrdersForPayment){
            totalInPln = totalInPln.add(paymentOrder.getOrder().getPrice());        }
        return totalInPln;
    }

}
