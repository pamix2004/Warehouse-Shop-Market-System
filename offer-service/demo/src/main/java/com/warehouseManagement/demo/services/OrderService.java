package com.warehouseManagement.demo.services;

import com.warehouseManagement.demo.Exceptions.InvalidOrderStatusChange;
import com.warehouseManagement.demo.PaymentStatus;
import com.warehouseManagement.demo.entity.*;
import com.warehouseManagement.demo.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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



    @Transactional
/**
 * @param storeId id of the store that places the order
 * @param carts carts that store wants to process
 */
    public String placeOrder(int storeId, List<Cart> carts) {
        // 1. Fetch the store or throw error if missing
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found with ID: " + storeId));

        // 2. CREATE ONE PAYMENT for all carts in this request
        // This allows one payment to be associated with multiple orders
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.pending);
        payment = paymentRepository.save(payment);

        for (Cart cart : carts) {
            // 3. Create and save the Order first to get an ID
            Order newOrder = new Order();
            newOrder.setOrderDate(LocalDate.now());
            newOrder.setStatus("Ordered");
            newOrder.setStore(store);
            newOrder.setPrice(BigDecimal.ZERO);
//            newOrder = orderRepository.save(newOrder);

            List<CartOffer> itemsInCart = cartOfferRepository.findAllByCart_CartId(cart.getCartId());
            double runningTotal = 0.0;

            // 4. Process items, create OrderOffers, and calculate total
            for (CartOffer cartOffer : itemsInCart) {
                // Check availability
                Offer currentOffer = offerRepository.findById(cartOffer.getOffer().getId());
                newOrder.setWholesaler(currentOffer.getWholesaler());
                orderRepository.save(newOrder);

                if (cartOffer.getQuantity() > currentOffer.getAvailable_quantity()) {
                    throw new RuntimeException("Requested quantity for " + currentOffer.getId() + " exceeds availability");
                }

                int leftAvailableQuantity = currentOffer.getAvailable_quantity() - cartOffer.getQuantity();
                currentOffer.setAvailable_quantity(leftAvailableQuantity);
                offerRepository.save(currentOffer);

                OrderOffer orderOffer = new OrderOffer();
                orderOffer.setOrder(newOrder);
                orderOffer.setOffer(cartOffer.getOffer());
                orderOffer.setQuantity(cartOffer.getQuantity());
                orderOfferRepository.save(orderOffer);

                runningTotal += (cartOffer.getOffer().getPrice() * cartOffer.getQuantity());
            }

            // 5. Update Order price with final total
            newOrder.setPrice(BigDecimal.valueOf(runningTotal));
            orderRepository.save(newOrder);

            // 6. LINK THE ORDER TO THE SHARED PAYMENT
            // This populates the payment_order junction table
            PaymentOrder po = new PaymentOrder();
            po.setOrder(newOrder);
            po.setPayment(payment);
            paymentOrderRepository.save(po);

            // 7. CLEANUP: Delete the items and the Cart itself
            cartOfferRepository.deleteAll(itemsInCart);
            cartRepository.delete(cart);

            System.out.println("Order " + newOrder.getOrderId() + " linked to Payment " + payment.getPaymentId());
        }

        return "Orders placed and linked to Payment ID: " + payment.getPaymentId() + " successfully.";
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

    @Transactional
    public String cancelOrder(int userId, int orderId) {

        System.out.println("Probuje anulowac");
        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));



        //You can only cancel your own order
        if (order.getStore().getId() != store.getId()) {
            return "redirect:/offer/orders";
        }

        // można anulować tylko zamówienie jeszcze nierozpoczęte
        if (!"Ordered".equals(order.getStatus())) {
            System.out.println("tutaj jest blad bo dalem UPPERCASE!");
            return "redirect:/offer/orders/" + orderId;
        }

        //You should add products to available pool when u cancel
        List<OrderOffer> listOfOrderOffers =  orderOfferRepository.findByOrder(order);
        Offer currentOffer;
        for(OrderOffer orderOffer: listOfOrderOffers) {
            currentOffer = orderOffer.getOffer();
            currentOffer.setAvailable_quantity(currentOffer.getAvailable_quantity()+orderOffer.getQuantity());
            offerRepository.save(currentOffer);
        }

        order.setStatus("Cancelled");
        orderRepository.save(order);
        System.out.println("Chyba anulowało");


        return "redirect:/offer/orders";
    }




}



