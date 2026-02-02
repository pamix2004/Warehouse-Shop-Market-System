package com.warehouseManagement.demo;


import com.warehouseManagement.demo.dto.*;
import com.warehouseManagement.demo.entity.*;
import com.warehouseManagement.demo.repo.*;
import com.warehouseManagement.demo.repo.CartOfferRepository;
import com.warehouseManagement.demo.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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





    @GetMapping("/account")
    public String account(@RequestHeader("X-User-Id") int userId, Model model){
        User userEntity = userRepository.findById(userId);
        model.addAttribute("email",userEntity.getEmail());
        model.addAttribute("id",userEntity.getId());

        String role = userEntity.getRole();
        model.addAttribute("role",role);
        if (role.equals("store")) {
            Store storeEntity = storeRepository.findByUser_Id(userEntity.getId());
            model.addAttribute("est_name",storeEntity.getName());
            model.addAttribute("address",storeEntity.getAddress());

            model.addAttribute("offers", offerRepository.findAll());


            List<CartInformationDTO> cartList = new ArrayList<>();
            List<Cart> allCarts = cartRepository.findByStore(storeEntity);
            float allCartsPrice = 0;

            for (Cart cart : allCarts) {

                CartInformationDTO dto = new CartInformationDTO();
                dto.setCartId(cart.getCartId());
                dto.setStoreId(cart.getStore().getId());
                dto.setWholesalerName(cart.getWholesaler().getName());

                double sum = 0;

                List<CartItemDTO> items = new ArrayList<>();


                // IMPORTANT: loop variable should be cartOffer, not cart
                for (CartOffer cartOffer : cartOfferRepository.findAllByCart_CartId(cart.getCartId())) {

                    CartItemDTO item = new CartItemDTO();


                     item.setName(cartOffer.getOffer().getProduct().getName());

                    item.setQuantity(cartOffer.getQuantity());
                    item.setUnitPrice(cartOffer.getOffer().getPrice());
                    item.setLineTotal(cartOffer.getOffer().getPrice()*cartOffer.getQuantity());
                    items.add(item);


                    // If you want total PRICE instead:
                     sum += cartOffer.getQuantity() * cartOffer.getOffer().getPrice();
                }

                dto.setItems(items);
                dto.setCartTotal(sum);
                allCartsPrice = (float) (allCartsPrice + sum);


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




    @PostMapping("/account/add")
    public String addOffer(@RequestHeader("X-User-Id") int userId,@ModelAttribute("offerForm") OfferFormDTO form) {



        User user = userRepository.findById(userId);
        Wholesaler wholesaler = wholesalerRepository.findByUser_Id(user.getId());

        Product product;

        // ===== WYBRANY ISTNIEJĄCY PRODUKT =====
        if (form.getProductId() != null) {

            product = productRepository.findById(form.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

        }
        // ===== NOWY PRODUKT =====
        else {

            Product newProduct = new Product();
            newProduct.setName(form.getProductName());
            newProduct.setProducer(
                    producerRepository.findById(form.getProducerId()).orElseThrow()
            );
            newProduct.setCategory(
                    categoryRepository.findById(form.getCategoryId()).orElseThrow()
            );

            product = productRepository.save(newProduct);
        }

        // ===== OFERTA =====
        Offer offer = new Offer();
        offer.setProduct(product);
        offer.setWholesaler(wholesaler);
        offer.setPrice(form.getPrice());
        offer.setAvailable_quantity(form.getAvailable_quantity());
        offer.setMinimal_quantity(form.getMinimal_quantity());

        offerRepository.save(offer);

        return "redirect:/offer/account";
    }

    @GetMapping("/testCSS")
    public String TestCSS(){
        return "test";
    }

    @PostMapping("/purchaseAllCarts")
    public String purchaseOffer(@RequestHeader("X-User-Id") int userId,
                                @ModelAttribute OfferPurchaseDTO purchaseDTO,
                                Model model) {
        System.out.println(orderService.testOrderingu());

        Store store = storeRepository.findByUser_Id(userId);
         List<Cart> allCarts = cartRepository.findByStore(store);
         orderService.placeOrder(store.getId(),allCarts);

        System.out.println("All carts");
        return "redirect:/offer/account";
    }


    @Transactional
    @PostMapping("/addToCart")
    public String addToCart(@RequestHeader("X-User-Id") int userId,
                            @ModelAttribute OfferPurchaseDTO purchaseDTO,
                            Model model) {

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        Offer offer = offerRepository.getReferenceById(purchaseDTO.getOfferId());

        if (purchaseDTO.getQuantity() < offer.getMinimal_quantity()
                || purchaseDTO.getQuantity() > offer.getAvailable_quantity()) {
            throw new RuntimeException("Not enough quantity");
        }

        // 1) znajdź cart dla (store, wholesaler) albo utwórz nowy
        Cart cartEntity;

        List<Cart> carts = cartRepository.findByStore_IdAndWholesaler_Id(
                store.getId(),
                offer.getWholesaler().getId()
        );

        if (!carts.isEmpty()) {
            cartEntity = carts.get(0); // zakładamy 1 cart na (store, wholesaler)
            System.out.println("Cart exists, cart_id=" + cartEntity.getCartId());
        } else {
            Cart newCart = new Cart();
            newCart.setStore(store);
            newCart.setWholesaler(offer.getWholesaler());
            cartEntity = cartRepository.save(newCart);
            System.out.println("Created cart_id=" + cartEntity.getCartId());
        }

        // 2) insert/update cart_product
        Integer cartId = cartEntity.getCartId();


        CartOffer cp = cartOfferRepository
                .findByCart_CartIdAndOffer_Id(cartId, offer.getId())
                .orElseGet(() -> {
                    com.warehouseManagement.demo.entity.CartOffer x = new com.warehouseManagement.demo.entity.CartOffer();
                    x.setCart(cartEntity);
                    x.setOffer(offer);
                    x.setQuantity(0);
                    return x;
                });

        cp.setQuantity(cp.getQuantity() + purchaseDTO.getQuantity()); // albo = purchaseDTO.getQuantity() jeśli ma nadpisywać
        cartOfferRepository.save(cp);

        return "redirect:/offer/account";
    }



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

            dto.setPaymentStatus(
                    paymentRepository.findByOrder_OrderId(order.getOrderId())
                            .map(Payment::getStatus)         // If payment exists, get its status
                            .orElse(PaymentStatus.pending)   // If it doesn't, use pending
            );

            orderSummaryList.add(dto);
        }

        model.addAttribute("orders", orderSummaryList);
        return "orders";
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
                            PaymentStatus pStatus = paymentRepository.findByOrder_OrderId(order.getOrderId())
                                    .map(Payment::getStatus)
                                    .orElse(PaymentStatus.pending);
                            dto.setPaymentStatus(pStatus);

                            return dto;
                        })
                        .toList()
        );
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


    private boolean isValidStatusChange(String current, String next) {

        System.out.println("current: "+current);
        System.out.println("next: "+next);
        return switch (current) {
            case "Created" -> false;
            case "Ordered" -> next.equals("In progress");
            case "In progress" -> next.equals("Shipped");
            case "Shipped" -> next.equals("Delivered");
            case "Cancelled" -> false;
            default -> false;
        };
    }


    @PostMapping("/wholesaler/orders/status")
    public String updateOrderStatus(@RequestHeader("X-User-Id") int userId,
                                    @RequestParam int orderId,
                                    @RequestParam String status) {

        orderService.updateOrderStatus(userId, orderId, status);

        return "redirect:/offer/wholesaler/orders";
    }


    @GetMapping("/orders/{orderId}")
    public String orderDetails(@PathVariable int orderId,
                               @RequestHeader("X-User-Id") int userId,
                               Model model) {
//        System.out.println("tutaj jest problem!");
        System.out.println("userId: "+userId);
        User user = userRepository.findById(userId);
        Wholesaler wholesaler = wholesalerRepository.findByUser_Id(user.getId());





        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));


        //U SHOULD CHECK IF the currentUser is the seller for given orderID
        List<OrderOffer> orderOffers =  orderOfferRepository.findByOffer_Wholesaler(wholesaler);
        System.out.println(orderOffers=orderOffers.get(0).);



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

        BigDecimal itemPrice = BigDecimal
                .valueOf(orderOffer.getOffer().getPrice())
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

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStore().getId() != store.getId()) {
            return "redirect:/offer/orders";
        }

        // można anulować tylko zamówienie jeszcze nierozpoczęte
        if (!"ORDERED".equals(order.getStatus())) {
            return "redirect:/offer/orders/" + orderId;
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        return "redirect:/offer/orders";
    }
}


