package com.warehouseManagement.demo;


import com.warehouseManagement.demo.Exceptions.ProductAlreadyExistsException;
import com.warehouseManagement.demo.dto.*;
import com.warehouseManagement.demo.services.PurchaseService;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClient;
import com.warehouseManagement.demo.entity.*;
import com.warehouseManagement.demo.repo.*;
import com.warehouseManagement.demo.repo.CartOfferRepository;
import com.warehouseManagement.demo.services.CartService;
import com.warehouseManagement.demo.services.OfferService;
import com.warehouseManagement.demo.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/offer")
public class OfferServiceController {

    @Autowired
    private UserRepository userRepository; // Spring injects it

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private WholesalerRepository wholesalerRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ProductRepository productRepository; // do selecta produktów

    @Autowired
    private ProducerRepository producerRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderOfferRepository orderOfferRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartOfferRepository cartOfferRepository;

    @Autowired
    OrderService orderService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    PaymentOrderRepository paymentOrderRepository;

    @Autowired
    CartService cartService;

    @Autowired
    OfferService offerService;

    @Autowired
    PurchaseService purchaseService;

    @Autowired
    private RestTemplate restTemplate;



    @GetMapping("/account")
    public String account(@RequestHeader("X-User-Id") int userId,  @RequestParam(name = "successPayment", required = false) Boolean successPayment,@RequestParam(name = "errorMessageParam", required = false) String errorMessageParam,Model model){
        User userEntity = userRepository.findById(userId);
        model.addAttribute("email",userEntity.getEmail());
        model.addAttribute("id",userEntity.getId());

        if (successPayment != null) {
            model.addAttribute("successPayment", successPayment);
        }

        if (errorMessageParam != null) {
            model.addAttribute("errorMessageParam", errorMessageParam);
        }


        String role = userEntity.getRole();
        model.addAttribute("role",role);
        if (role.equals("store")) {
            Store storeEntity = storeRepository.findByUser_Id(userEntity.getId());
            model.addAttribute("est_name",storeEntity.getName());
            model.addAttribute("address",storeEntity.getAddress());

            model.addAttribute("offers",offerService.getAllActiveOffers());


            List<CartInformationDTO> cartList = new ArrayList<>();
            List<Cart> allCarts = cartRepository.findByStore(storeEntity);
            BigDecimal allCartsPrice = BigDecimal.ZERO;

            for (Cart cart : allCarts) {

                CartInformationDTO dto = new CartInformationDTO();
                dto.setCartId(cart.getCartId());
                dto.setStoreId(cart.getStore().getId());
                dto.setWholesalerName(cart.getWholesaler().getName());

                BigDecimal sum = BigDecimal.ZERO;

                List<CartItemDTO> items = new ArrayList<>();


                // IMPORTANT: loop variable should be cartOffer, not cart
                for (CartOffer cartOffer : cartOfferRepository.findAllByCart_CartId(cart.getCartId())) {

                    CartItemDTO item = new CartItemDTO();


                     item.setName(cartOffer.getOffer().getProduct().getName());

                    item.setQuantity(cartOffer.getQuantity());
                    item.setUnitPrice(cartOffer.getOffer().getPrice());
                    item.setLineTotal(
                            cartOffer.getOffer().getPrice().multiply(new BigDecimal(cartOffer.getQuantity()))
                    );                    item.setOfferId(cartOffer.getOffer().getId());
                    items.add(item);


                    // If you want total PRICE instead:
                    sum = sum.add(
                            BigDecimal.valueOf(cartOffer.getQuantity())
                                    .multiply(cartOffer.getOffer().getPrice())
                    );                }

                dto.setItems(items);
                dto.setCartTotal(sum);
                allCartsPrice = allCartsPrice.add(sum);
                System.out.println("sum = "+sum);

                if(!items.isEmpty())
                    cartList.add(dto);
            }

            model.addAttribute("isAnyCartActive", !cartList.isEmpty());


            model.addAttribute("cartDTOList", cartList);
            model.addAttribute("allCartsPrice", allCartsPrice);







        } else if (role.equals("wholesaler")) {
            Wholesaler wsEntity = wholesalerRepository.findByUser_Id(userEntity.getId());
            model.addAttribute("est_name", wsEntity.getName());
            model.addAttribute("address", wsEntity.getAddress());
            model.addAttribute("possibleOfferStates",OfferState.values());


            // OFERTY HURTOWNIKA
            model.addAttribute("offers", offerRepository.findByWholesaler(wsEntity));

            // FORMULARZ
            model.addAttribute("offerForm", new OfferFormDTO());
            model.addAttribute("products", productRepository.findAll());
            model.addAttribute("producers", producerRepository.findAll());
            model.addAttribute("categories", categoryRepository.findAll());

        }
        System.out.println("Your role is "+role);

        return "account";
}




    @ResponseBody
    @PostMapping("/updateState")
    public ResponseEntity<?> changeOfferStatus(@RequestHeader("X-User-Id") int userId,int offerId,OfferState desiredState){
        offerService.changeOfferState(userId,offerId,desiredState);
        return ResponseEntity.ok().build();
    }


    /**
     * Function used for adding new offers( by wholesalers)
     */
    @PostMapping("/account/add")
    @ResponseBody // Return JSON instead of a view
    public ResponseEntity<?> addOffer(@RequestHeader("X-User-Id") int userId, @ModelAttribute("offerForm") OfferFormDTO form) {
        try {
            User user = userRepository.findById(userId);
            Wholesaler wholesaler = wholesalerRepository.findByUser_Id(user.getId());

            Product product;
            if (form.getProductId() != null) {
                product = productRepository.findById(form.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
            } else {
                if(productRepository.existsByName(form.getProductName())){
                    // Return a 400 error with the message
                    return ResponseEntity.badRequest().body("Product already exists");
                }

                Product newProduct = new Product();
                newProduct.setName(form.getProductName());
                newProduct.setProducer(producerRepository.findById(form.getProducerId()).orElseThrow());
                newProduct.setCategory(categoryRepository.findById(form.getCategoryId()).orElseThrow());
                product = productRepository.save(newProduct);
            }

            Offer offer = new Offer();
            offer.setProduct(product);
            offer.setWholesaler(wholesaler);
            offer.setPrice(form.getPrice());
            offer.setAvailable_quantity(form.getAvailable_quantity());
            offer.setMinimal_quantity(form.getMinimal_quantity());
            offer.setState(OfferState.active);
            offerRepository.save(offer);

            return ResponseEntity.ok("Offer added successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/testCSS")
    public String TestCSS(){
        return "test";
    }


    @PostMapping("/purchaseAllCarts")
    public String purchaseOffer(@RequestHeader("X-User-Id") int userId,
                                @ModelAttribute OfferPurchaseDTO purchaseDTO,
                                Model model) {
        try{
            return purchaseService.handlePurchasingAllCarts(userId);
        }
        catch (Exception e){
            return e.getMessage();
        }


    }

    @ResponseBody
    @PostMapping("/fulfillOrder")
    public ResponseEntity<?> fulfillOrder(@RequestBody int paymentId){
        paymentRepository.findById(paymentId).orElseThrow(() -> new RuntimeException("Payment not found"));
        System.out.println("I want to fulfill order for payment: "+paymentId);


        List<Integer> cartsInPayment = cartRepository.findCartIdsNative(paymentId);
        System.out.println("carts in payment: "+cartsInPayment);

        cartService.clearCarts(cartsInPayment);


        return ResponseEntity.ok().build();
    }

    @PostMapping("/addToCart")
    public String addToCart(@RequestHeader("X-User-Id") int userId,
                            @ModelAttribute OfferPurchaseDTO purchaseDTO,
                            RedirectAttributes redirectAttributes) {
        try {
            cartService.addOfferToCart(userId, purchaseDTO);
        } catch (Exception e) {
            // This catches your "Invalid quantity" or "Not enough stock" errors
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/offer/account";
    }

//    public String callPaymentServiceToGetStripeCheckoutLink(String checkoutSessionId){
//
//        String paymentServiceURL = "http://payment-service/payment/getLinkForCheckout";
//
//
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<String> request = new HttpEntity<>("\"" + checkoutSessionId + "\"", headers);
//
//        ResponseEntity<String> response =
//                restTemplate.postForEntity(paymentServiceURL, request, String.class);
//
//        return response.getBody();
//    }




    @GetMapping("/orders")
    public String myOrders(@RequestHeader("X-User-Id") int userId, Model model) {
        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        List<Order> orders = orderRepository.findByStore(store);
        List<OrderSummaryDTO> orderSummaryList = new ArrayList<>();

        for (Order order : orders) {
            OrderSummaryDTO dto = new OrderSummaryDTO();

            // Basic field mapping
            dto.setId(order.getOrderId());
            dto.setOrderDate(order.getOrderDate());
            dto.setStatus(order.getStatus());
            dto.setTotalPrice(order.getPrice());
            dto.setWholesalerName(order.getWholesaler().getName());

            //System.out.println("id orderu = "+offerRepository.findById(order.getOrderId())+"id wholeseerla "+offerRepository.findById(order.getOrderId()).get().getWholesaler().getId());
            System.out.println("id orderu "+order.getOrderId());
            //dto.setWholesalerName(offerRepository.findById(order.getOrderId()).get().getWholesaler().getName());
            List<OrderOffer> ord =  orderOfferRepository.findByOrder(order);
            //dto.setWholesalerName(ord.get(0).getOffer().getWholesaler().getName());




//            Szukaj tutaj!
//            dto.setPaymentStatus(
//                    paymentRepository.findByOrder_OrderId(order.getOrderId())
//                            .map(Payment::getStatus)         // If payment exists, get its status
//                            .orElse(PaymentStatus.pending)   // If it doesn't, use pending
//            );
            PaymentOrder po = paymentOrderRepository.findByOrder(order)
                    .orElseThrow(() -> new RuntimeException("No payment found for this order"));

            Payment payment = po.getPayment();
        // Now you have the payment object and can do payment.getPaymentId(), etc.
            dto.setPaymentStatus(payment.getStatus());









            orderSummaryList.add(dto);
        }

        model.addAttribute("orders", orderSummaryList);
        return "orders";
    }


    @PostMapping("/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateCartQuantity(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam int cartId,
            @RequestParam int offerId,
            @RequestParam int quantity) {

        try {
            cartService.updateOfferQuantity(userId, cartId, offerId, quantity);
            List<CartInformationDTO> cartInformationDTOList = cartService.getCartDetailsForStore(userId);
            System.out.println(cartInformationDTOList);
            return ResponseEntity.ok(cartInformationDTOList);
        } catch (Exception e) {
            // Change body(null) to body(e.getMessage())
            // Also changed return type to ResponseEntity<?> to allow both List and String
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/wholesaler/orders")
    public String wholesalerOrders(@RequestHeader("X-User-Id") int userId, Model model) {

        User user = userRepository.findById(userId);
        Wholesaler wholesaler = wholesalerRepository.findByUser_Id(user.getId());

        model.addAttribute(
                "orders",
                orderOfferRepository.findByOffer_Wholesaler(wholesaler)
                        .stream()
                        .map(OrderOffer::getOrder) // Get the Order from the OrderOffer
                        .filter(order -> !"CANCELLED".equals(order.getStatus()))
                        .distinct()
                        .map(order -> {
                            // Create the DTO
                            OrderSummaryDTO dto = new OrderSummaryDTO();
                            dto.setId(order.getOrderId());
                            dto.setOrderDate(order.getOrderDate());
                            dto.setStatus(order.getStatus());
                            dto.setTotalPrice(order.getPrice());
                            dto.setStoreName(order.getStore().getName());

                            // Fetch and set the Payment Status
//                            Szukaj tutaj
//                            PaymentStatus pStatus = paymentRepository.findByOrder_OrderId(order.getOrderId())
//                                    .map(Payment::getStatus)
//                                    .orElse(PaymentStatus.pending);
//                            dto.setPaymentStatus(pStatus);

                            PaymentOrder po = paymentOrderRepository.findByOrder(order)
                                    .orElseThrow(() -> new RuntimeException("No payment found for this order"));

                            Payment payment = po.getPayment();
                            // Now you have the payment object and can do payment.getPaymentId(), etc.
                            dto.setPaymentStatus(payment.getStatus());

                            return dto;
                        })
                        .toList()
        );

        System.out.println(                orderOfferRepository.findByOffer_Wholesaler(wholesaler)
                .stream()
                .map(OrderOffer::getOrder) // Get the Order from the OrderOffer
                .filter(order -> !"CANCELLED".equals(order.getStatus()))
                .distinct()
                .map(order -> {
                    // Create the DTO
                    OrderSummaryDTO dto = new OrderSummaryDTO();
                    dto.setId(order.getOrderId());
                    dto.setOrderDate(order.getOrderDate());
                    dto.setStatus(order.getStatus());
                    dto.setTotalPrice(order.getPrice());
                    dto.setStoreName(order.getStore().getName());

                    // Fetch and set the Payment Status
//                            Szukaj tutaj
//                            PaymentStatus pStatus = paymentRepository.findByOrder_OrderId(order.getOrderId())
//                                    .map(Payment::getStatus)
//                                    .orElse(PaymentStatus.pending);
//                            dto.setPaymentStatus(pStatus);

                    PaymentOrder po = paymentOrderRepository.findByOrder(order)
                            .orElseThrow(() -> new RuntimeException("No payment found for this order"));

                    Payment payment = po.getPayment();
                    // Now you have the payment object and can do payment.getPaymentId(), etc.
                    dto.setPaymentStatus(payment.getStatus());

                    return dto;
                })
                .toList());

        return "wholesaler-orders";
    }

    @GetMapping("/wholesaler/orders/{orderId}")
    public String wholesalerOrderDetails(@PathVariable int orderId,
                                         @RequestHeader("X-User-Id") int userId,
                                         Model model) {


        User user = userRepository.findById(userId);
        Wholesaler wholesaler = wholesalerRepository.findByUser_Id(user.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // sprawdzamy czy zamówienie zawiera produkty tego hurtownika
        boolean belongsToWholesaler = orderOfferRepository.findByOrder(order)
                .stream()
                .anyMatch(oo -> oo.getOffer().getWholesaler().getId() == wholesaler.getId());

        if (!belongsToWholesaler) {
            return "redirect:/offer/wholesaler/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("orderOffers", orderOfferRepository.findByOrder(order));

        return "wholesaler-order-details";
    }



    @PostMapping("/changeOffer")
    @ResponseBody
    public ResponseEntity<?> changeOffer(@RequestHeader("X-User-Id") int userId,@RequestParam int offerId,@RequestParam BigDecimal price,@RequestParam int availableQuantity,@RequestParam int minimalQuantity) {
        try{
            offerService.changeOffer(userId, offerId, price, availableQuantity, minimalQuantity);
            return ResponseEntity.ok(Map.of());
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/wholesaler/orders/status")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(@RequestHeader("X-User-Id") int userId,
                                               @RequestParam int orderId,
                                               @RequestParam String status) {
        try {
            // Your Service Logic
            orderService.updateOrderStatus(userId, orderId, status);

            // Return JSON Success
            return ResponseEntity.ok(Map.of("message", "Status updated successfully!"));

        } catch (Exception e) {
            // This catches your service errors (like "Payment missing")
            // and returns them as a 400 Bad Request JSON
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    @GetMapping("/orders/{orderId}")
    public String orderDetails(@PathVariable int orderId,
                               @RequestHeader("X-User-Id") int userId,
                               Model model) {
//        System.out.println("tutaj jest problem!");
        System.out.println("userId: "+userId);
        User user = userRepository.findById(userId);
        Wholesaler wholesaler =  wholesalerRepository.findByUser_Id(user.getId());
        Store store = storeRepository.findByUser_Id(userId);

        System.out.println("User:"+user);
        System.out.println("Wholesaler:"+wholesaler);
        System.out.println("Store:"+store);



        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        //Happens if we are a store
       if(store != null) {
           if(!orderRepository.existsByorderIdAndStoreId(orderId, store.getId())) {
               throw  new RuntimeException("To nie twoja oferta!");
           }
       }
       //Happens if we are the wholesaler
       else{
           if(orderRepository.doesOrderBelongToWholesaler(orderId, wholesaler.getId())==0){
               throw  new RuntimeException("To nie twoja oferta!");

           }
       }

        



        model.addAttribute("order", order);
        model.addAttribute(
                "orderOffers",
                orderOfferRepository.findByOrder(order)
        );

        return "order-details";
    }

    @PostMapping("/orders/item/remove")
    @Transactional
    public String removeItemFromBasket(@RequestHeader("X-User-Id") int userId,
                                       @RequestParam int orderOfferId) {

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        OrderOffer orderOffer = orderOfferRepository.findById(orderOfferId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        Order order = orderOffer.getOrder();

        if (order.getStore().getId() != store.getId()) {
            return "redirect:/offer/orders";
        }

        if (!"CREATED".equals(order.getStatus())) {
            return "redirect:/offer/orders/" + order.getOrderId();
        }

        BigDecimal itemPrice = orderOffer.getOffer().getPrice()
                .multiply(BigDecimal.valueOf(orderOffer.getQuantity()));

        order.setPrice(order.getPrice().subtract(itemPrice));

        orderOfferRepository.delete(orderOffer);

        if (orderOfferRepository.findByOrder(order).isEmpty()) {
            orderRepository.delete(order);
            return "redirect:/offer/account";
        }

        orderRepository.save(order);

        return "redirect:/offer/orders/" + order.getOrderId();
    }

    @PostMapping("/orders/finalize")
    public String finalizeOrder(@RequestHeader("X-User-Id") int userId,
                                @RequestParam int orderId) {

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStore().getId() != store.getId()) {
            return "redirect:/offer/orders";
        }

        if (!"CREATED".equals(order.getStatus())) {
            return "redirect:/offer/orders/" + orderId;
        }

        if (orderOfferRepository.findByOrder(order).isEmpty()) {
            return "redirect:/offer/orders/" + orderId;
        }

        order.setStatus("ORDERED");
        orderRepository.save(order);

        return "redirect:/offer/account";
    }

    @PostMapping("/orders/cancel")
    @Transactional
    public String cancelOrder(@RequestHeader("X-User-Id") int userId,
                              @RequestParam int orderId) {
        return orderService.cancelOrder(userId, orderId);
    }
}


