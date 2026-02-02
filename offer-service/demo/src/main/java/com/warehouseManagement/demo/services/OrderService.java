package com.warehouseManagement.demo.services;

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


    @Transactional
    /**
     * @param storeId id of the store that places the order
     * @param carts cartsthat store wants to process ( store doesnt have to buy all their carts)
     * */
    public String placeOrder(int storeId, List<Cart> carts) {
        Store store = storeRepository.findById(storeId).get();

        for (Cart cart : carts) {
            // 1. Create and save the Order first to get an ID
            Order newOrder = new Order();
            newOrder.setOrderDate(LocalDate.now());
            newOrder.setStatus("Ordered");
            newOrder.setStore(store);
            newOrder.setPrice(BigDecimal.valueOf(0));
            newOrder = orderRepository.save(newOrder);

            List<CartOffer> itemsInCart = cartOfferRepository.findAllByCart_CartId(cart.getCartId());
            double runningTotal = 0.0;

            // 2. Process items, create OrderOffers, and calculate total
            for (CartOffer cartOffer : itemsInCart) {
                System.out.println("quantity in cart: "+cartOffer.getQuantity());
                System.out.println("available quantity: "+offerRepository.findById(cartOffer.getOffer().getId()).getAvailable_quantity());
                if(cartOffer.getQuantity()>offerRepository.findById(cartOffer.getOffer().getId()).getAvailable_quantity())
                    throw new RuntimeException("It's less than available quantity");
                OrderOffer orderOffer = new OrderOffer();
                orderOffer.setOrder(newOrder);
                orderOffer.setOffer(cartOffer.getOffer());
                orderOffer.setQuantity(cartOffer.getQuantity());


                orderOfferRepository.save(orderOffer);

                runningTotal += (cartOffer.getOffer().getPrice() * cartOffer.getQuantity());
            }

            // 3. Update Order price with final total
            newOrder.setPrice(BigDecimal.valueOf(runningTotal));
            orderRepository.save(newOrder);

            //Create payment for each order
            Payment payment = new Payment();
            payment.setOrder(newOrder);
            payment.setStatus(PaymentStatus.pending);
            paymentRepository.save(payment);

            // 4. CLEANUP: Delete the items and the Cart itself
            // First, delete the child records (CartOffer)
            cartOfferRepository.deleteAll(itemsInCart);

            // Then, delete the parent record (Cart)
            cartRepository.delete(cart);

            System.out.println("Deleted Cart ID: " + cart.getCartId() + " after successful order placement.");
        }

        return "Orders placed and carts cleared successfully";
    }

    public String updateOrderStatus(int userId, int orderId, String desiredStatus) {

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
            return "redirect:/offer/wholesaler/orders";
        }

        //It should check if status change is possible

        order.setStatus(desiredStatus);
        orderRepository.save(order);

        return "redirect:/offer/wholesaler/orders";
    }



}



