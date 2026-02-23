package com.warehouseManagement.demo.services;

import com.warehouseManagement.demo.Exceptions.CartsAreEmpty;
import com.warehouseManagement.demo.Exceptions.InvalidOrderStatusChange;
import com.warehouseManagement.demo.PaymentStatus;
import com.warehouseManagement.demo.entity.*;
import com.warehouseManagement.demo.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    @Autowired
    StoreRepository storeRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderOfferRepository orderOfferRepository;
    @Autowired
    CartOfferRepository cartOfferRepository;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    PaymentRepository paymentRepository;
    public String testOrderingu(){
        return "fajny Test zamawiania serwisowego";
    }
    @Autowired
    OfferRepository offerRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WholesalerRepository wholesalerRepository;
    @Autowired
    PaymentOrderRepository paymentOrderRepository;
    @Autowired
    RestTemplate restTemplate;



    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment placeOrder(int storeId, List<Cart> carts) {
        System.out.println("Trying to place order");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found with ID: " + storeId));

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.pending);

        payment = paymentRepository.save(payment);

        for (Cart cart : carts) {
            List<CartOffer> itemsInCart = cartOfferRepository.findAllByCart_CartId(cart.getCartId());

            // Validation: Don't process empty carts
            if (itemsInCart.isEmpty()) continue;

            // 1. Initialize the Order
            Order newOrder = new Order();
            newOrder.setOrderDate(LocalDate.now());
            newOrder.setStatus("Ordered");
            newOrder.setStore(store);

            // 2. FIX: Set the wholesaler BEFORE the first save
            // Assuming one cart = one wholesaler.
            Wholesaler wholesaler = itemsInCart.get(0).getOffer().getWholesaler();
            newOrder.setWholesaler(wholesaler);

            // 3. Save the order once to get the ID for the OrderOffers
            newOrder.setPrice(BigDecimal.valueOf(0));
            newOrder = orderRepository.save(newOrder);

            BigDecimal runningTotal = BigDecimal.ZERO;
            for (CartOffer cartOffer : itemsInCart) {
                Offer currentOffer = offerRepository.findById(cartOffer.getOffer().getId());

                // Availability Check
                if (cartOffer.getQuantity() > currentOffer.getAvailable_quantity()) {
                    throw new RuntimeException("Quantity exceeds availability for item: " + currentOffer.getId());
                }

                // Update Stock
                currentOffer.setAvailable_quantity(currentOffer.getAvailable_quantity() - cartOffer.getQuantity());
                offerRepository.save(currentOffer);

                // Create Order Item
                OrderOffer orderOffer = new OrderOffer();
                orderOffer.setOrder(newOrder);
                orderOffer.setOffer(cartOffer.getOffer());
                orderOffer.setQuantity(cartOffer.getQuantity());

                orderOfferRepository.save(orderOffer);

                runningTotal = runningTotal.add(
                        cartOffer.getOffer().getPrice().multiply(BigDecimal.valueOf(cartOffer.getQuantity()))
                );            }



            // 4. Update final price
            newOrder.setPrice(runningTotal);
            orderRepository.save(newOrder);

            // 5. Link Payment
            PaymentOrder po = new PaymentOrder();
            po.setOrder(newOrder);
            po.setPayment(payment);

            paymentOrderRepository.save(po);

            // Cleanup
//            cartOfferRepository.deleteAll(itemsInCart);
//            cartRepository.delete(cart);
        }

        return payment;
    }

    private double calculateTotal(){

        return 15d;
    }

    private boolean isValidStatusChange(String current, String next,Order order) {

        System.out.println("current: "+current);
        System.out.println("next: "+next);

        switch (current) {
            case "Created":
                return false;

            case "Ordered":
                PaymentOrder paymentOrder = paymentOrderRepository.findByOrder(order).orElseThrow(()->new RuntimeException("Payment doesnt exist for this order"));

                System.out.println("payment details:payment id "+paymentOrder.getPayment().getPaymentId()+" status"+paymentOrder.getPayment().getStatus());

                if(!(paymentOrder.getPayment().getStatus() ==PaymentStatus.succeeded)){
                    System.out.println("FALSE!!");
                    return false;
                }


                return next.equals("In progress");

            case "In progress":
                return next.equals("Shipped");

            case "Shipped":
                return next.equals("Delivered");

            case "Cancelled":
                return false;

            default:
                return false;
        }

    }

    public void updateOrderStatus(int userId, int orderId, String desiredStatus) {

        System.out.println("user wants to change state to " + desiredStatus);
        User user = userRepository.findById(userId);
        Wholesaler wholesaler = wholesalerRepository.findByUser_Id(user.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // użytkownik może edytować tylko swoje zamówienia
        boolean belongsToWholesaler = orderOfferRepository
                .findByOrder(order)
                .stream()
                .anyMatch(orderOffer ->
                        orderOffer.getOffer()
                                .getWholesaler()
                                .getId() == wholesaler.getId()
                );




        if (!belongsToWholesaler) {
            throw new RuntimeException("You are not authorized to do it");
        }

        boolean isValidStatusChange = false;
        String statusMessage = "Transition allowed"; // Default message
        String currentStatus = order.getStatus();

        switch (currentStatus) {
            case "Created":
                statusMessage = "Order is in 'Created' state; it must be 'Ordered' before processing.";
                isValidStatusChange = false;
                break;

            case "Ordered":
                PaymentOrder paymentOrder = paymentOrderRepository.findByOrder(order)
                        .orElseThrow(() -> new RuntimeException("Payment doesn't exist for this order"));

                if (paymentOrder.getPayment().getStatus() != PaymentStatus.succeeded) {
                    statusMessage = "Cannot change status if payment has not been completed";

                    //If payment has not yet been cancelled and successful we can cancel the order
                    if(paymentOrder.getPayment().getStatus() == PaymentStatus.pending && desiredStatus.equals("Cancelled")){
                        System.out.println("user wants to cancel order" +order.getOrderId());
                    }

                    isValidStatusChange = false;
                } else if (!desiredStatus.equals("In progress")) {
                    statusMessage = "From 'Ordered', you can only move to 'In progress'.";
                    isValidStatusChange = false;
                } else {
                    isValidStatusChange = true;
                }
                break;

            case "In progress":
                isValidStatusChange = desiredStatus.equals("Shipped");
                if (!isValidStatusChange) statusMessage = "In progress orders can only move to 'Shipped'.";
                break;

            case "Shipped":
                isValidStatusChange = desiredStatus.equals("Delivered");
                if (!isValidStatusChange) statusMessage = "Shipped orders can only move to 'Delivered'.";
                break;

            default:
                statusMessage = "Invalid transition or order is in a terminal state (Cancelled/Delivered).";
                isValidStatusChange = false;
                break;
        }

// Final Execution
        if (isValidStatusChange) {
            order.setStatus(desiredStatus);
            orderRepository.save(order);
        } else {
            throw new InvalidOrderStatusChange(statusMessage);
        }



    }

    public void callPaymentServiceToExpireCheckout(String checkoutSessionId){
        String paymentServiceURL = "http://payment-service/payment/expire-checkout";



        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);



        HttpEntity<String> requestEntity = new HttpEntity<>(checkoutSessionId, headers);

        // Send POST request
        ResponseEntity<HashMap> response = restTemplate.postForEntity(paymentServiceURL,requestEntity,HashMap.class);

    }

    @Transactional
    public String cancelOrder(int userId, int orderId) {
        System.out.println("Probuje anulowac");

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        PaymentOrder paymentOrder = paymentOrderRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Payment not found for given order"));

        Payment payment = paymentOrder.getPayment();

        // Get all orders associated with the same payment
        List<PaymentOrder> associatedOrders = paymentOrderRepository.findByPayment(payment);
        System.out.println("associatedOrders: " + associatedOrders);

        for (PaymentOrder po : associatedOrders) {
            Order o = po.getOrder();

            // Only allow cancelling orders from this user's store
            if (o.getStore().getId() != store.getId()) {
                continue; // skip orders that belong to another store
            }

            // Only cancel orders that are still in 'Ordered' status
            if (!"Ordered".equals(o.getStatus())) {
                System.out.println("Skipping order " + o.getOrderId() + " because it's already " + o.getStatus());
                continue;
            }

            // Cancel payment if not already succeeded and cancelec
            if (payment.getStatus() != PaymentStatus.succeeded && payment.getStatus() != PaymentStatus.canceled) {
                callPaymentServiceToExpireCheckout(payment.getStripeSessionId());
                payment.setStatus(PaymentStatus.canceled);
            }

            // Add products back to available pool
            List<OrderOffer> listOfOrderOffers = orderOfferRepository.findByOrder(o);
            for (OrderOffer orderOffer : listOfOrderOffers) {
                Offer currentOffer = orderOffer.getOffer();
                currentOffer.setAvailable_quantity(currentOffer.getAvailable_quantity() + orderOffer.getQuantity());
                offerRepository.save(currentOffer);
            }

            o.setStatus("Cancelled");
            orderRepository.save(o);
            System.out.println("Cancelled order: " + o.getOrderId());
        }

        return "redirect:/offer/orders";
    }








}



