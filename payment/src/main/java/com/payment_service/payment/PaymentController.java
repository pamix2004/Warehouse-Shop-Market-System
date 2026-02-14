package com.payment_service.payment;

import com.payment_service.payment.entity.Payment;
import com.payment_service.payment.entity.PaymentOrder;
import com.payment_service.payment.repository.PaymentOrderRepository;
import com.payment_service.payment.repository.PaymentRepository;
import com.payment_service.payment.services.GatewayUrlResolver;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/payment")
public class PaymentController {
    @Autowired
    GatewayUrlResolver gatewayUrlResolver;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    PaymentOrderRepository paymentOrderRepository;
    @Autowired
    RestTemplate restTemplate;

    @Value("${app.stripeTestSecretKey}")
    private String stripeTestSecretKey;

    @Value("${app.stripeEndpointSecret}")
    private String endPointSecret;

    @Transactional
    public void fulfillPurchase(String sessionId){
        try{
            //Make sure fulfillment hasn't already been performed for this checkout session
            Payment payment = paymentRepository.findByStripeSessionId(sessionId).orElseThrow(()->new RuntimeException("Payment not found"));
            System.out.println("Co tu nie dziala");
            //We take action only if payment has not been yet processed( still pending)
            if(payment.getStatus()==PaymentStatus.pending){
                // 1. Setup expansion for both line_items and payment_intent
                SessionRetrieveParams params = SessionRetrieveParams.builder()
                        .addExpand("line_items")
                        .addExpand("payment_intent") // Add this line
                        .build();

                // 2. Retrieve the session
                Session checkoutSession = Session.retrieve(sessionId, params, null);

                // 3. Access the full PaymentIntent object
                PaymentIntent paymentIntent = checkoutSession.getPaymentIntentObject();

                if (paymentIntent != null) {
                    if ("succeeded".equals(paymentIntent.getStatus())) {
                        System.out.println("Payment succeeded. Updating local status and calling remote fulfillment...");

                        // 1. Update local database first
                        payment.setStatus(PaymentStatus.succeeded);
                        paymentRepository.save(payment);

                        // 2. Call the external endpoint
                        String url = "http://10.10.10.70:8085/offer/fulfillOrder";

                        int paymentId = payment.getPaymentId();

                        try {
                            // Sends the int as the request body
                            ResponseEntity<String> response = restTemplate.postForEntity(url, paymentId, String.class);

                            if (response.getStatusCode().is2xxSuccessful()) {
                                System.out.println("Remote fulfillment successful: " + response.getBody());
                            } else {
                                System.err.println("Remote service returned error: " + response.getStatusCode());
                                // Optional: Roll back local status or flag for manual review
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to connect to fulfillment service: " + e.getMessage());
                            // This is a critical point: The payment succeeded but the fulfillment call failed.
                            // You might want to log this in a 'failed_tasks' table for a retry mechanism.
                        }

                    } else {
                        System.out.println("Payment status: " + paymentIntent.getStatus() + ". Fulfillment skipped.");
                    }
                }
            }
        }
        catch (Exception e){
            System.out.println("fulfill purchase error!");
        }

    }


    @PostMapping("/stripeWebhook")
    public ResponseEntity<?> handleStripeWebhook(@RequestBody String payload,@RequestHeader("Stripe-Signature") String sigHeader) {

            Event stripeEvent = null;

            if(endPointSecret != null && sigHeader != null){
                try{
                    stripeEvent = Webhook.constructEvent(payload,sigHeader,endPointSecret);
                } catch (SignatureVerificationException e) {
                    System.out.println("Webhook error while parsing basic request.");

                    return ResponseEntity.badRequest().build();
                }
            }

            // Deserialize the nested object inside the event
            EventDataObjectDeserializer dataObjectDeserializer = stripeEvent.getDataObjectDeserializer();
            StripeObject stripeObject = null;
            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            } else {
                // Deserialization failed, probably due to an API version mismatch.
                return ResponseEntity.internalServerError().build();
            }

        switch (stripeEvent.getType()) {
            case "checkout.session.completed":
                // Correct cast for Checkout
                Session session = (Session) stripeObject;
                System.out.println("Webhook sessionID: " + session.getId());
                fulfillPurchase(session.getId());
                break;

        }


        return ResponseEntity.ok().build();
    }

    @GetMapping("/paymentPage")
    public String  payment() {
        System.out.println("stripeTestSecretKey: " + stripeTestSecretKey);
        return "paymentIndex";
    }



    @GetMapping("/success")
    @ResponseBody
    public String success()
    {

        return "success";
    }

    public void sendMail(String email, String subject, String body)
    {
        String url = "http://10.10.10.70:8085/email/sendEmail";

        // Build a Map or a DTO to represent the JSON body
        Map<String, String> payload = new HashMap<>();
        payload.put("toEmail", email);
        payload.put("subject", subject);
        payload.put("body", body);

        // postForObject handles the JSON conversion automatically
        String response = restTemplate.postForObject(url, payload, String.class);
        System.out.println("Response: " + response);
        System.out.println("Email sent");
    }

    @GetMapping("/cancel")
    @ResponseBody
    public  String cancel()
    {
        return "cancel";
    }

    public static class CheckoutRequest {
        public int paymentId;
        public String userEmail;
        public long price;

        public CheckoutRequest() {} // Jackson needs default constructor

        public CheckoutRequest(int paymentId, String userEmail,long price) {
            this.paymentId = paymentId;
            this.userEmail = userEmail;
            this.price = price;
        }

        // getters/setters if needed
    }

    @PostMapping("/create-checkout-session")
    @ResponseBody
    public ResponseEntity<HashMap<String,String>> createCheckoutSession(@RequestBody CheckoutRequest checkoutRequest) {
        int paymentId = checkoutRequest.paymentId;
        String userEmail = checkoutRequest.userEmail;
        long price = checkoutRequest.price;

        Stripe.apiKey = "sk_test_51SnIukC5Gg4G70G2pvf9hpuvx9tfTMgLJnggFCi2suk7rlczOskEdq7SIHreXfdIqnfe4JdJaQMloCoNaK5Ma3hd00CgaStEcJ";
        System.out.println("User wants to complete payment_id" + paymentId);

        try {
            String baseUrl = gatewayUrlResolver.gatewayBaseUrl();
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(baseUrl + "/payment/success")
                    .setCancelUrl(baseUrl + "/payment/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("pln")
                                                    .setUnitAmount(price)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Wholesaler-store system")
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);

            Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new RuntimeException("Payment not found(create-checkout-session)"));
            payment.setStripeSessionId(session.getId());
            paymentRepository.save(payment);
            sendMail(userEmail, "Platnosc", session.getUrl());

            System.out.println("I should send confirmation email to " + userEmail);


            HashMap<String, String> body = new HashMap<>();
            body.put("stripe-checkout", session.getUrl());
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }


    }
}
