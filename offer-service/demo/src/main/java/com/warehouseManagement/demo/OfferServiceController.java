package com.warehouseManagement.demo;


import com.warehouseManagement.demo.dto.OfferFormDTO;
import com.warehouseManagement.demo.dto.OfferPurchaseDTO;
import com.warehouseManagement.demo.entity.*;
import com.warehouseManagement.demo.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.math.BigDecimal;

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

    //We derive ID from a token, if its invalid, we return -1
    public int deriveID(String token){
        System.out.println("Im deriving ID");
        int id = 1;

        //We validate JWT token using jwt-service (microservice). If it's correct it will return userId, if it's invalid it will throw HTTP status of 504 or 401.
        try{
            //User jwt-service
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Integer> resp = restTemplate.postForEntity("http://localhost:8085/jwt/verifyJWTToken?token={token}",null,Integer.class,token);
            id = Integer.parseInt((String.valueOf(resp.getBody())));
            int statusCode = resp.getStatusCode().value();
            return id;
        }

        catch (Exception e) {
            System.out.println("Error when deriving ID,"+e.getMessage());
            return -1;
        }
    }



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

            Order activeOrder =
                    orderRepository.findFirstByStoreAndStatus(storeEntity, "CREATED");

            if (activeOrder != null) {
                model.addAttribute("basketOrder", activeOrder);
                model.addAttribute(
                        "basketItems",
                        orderOfferRepository.findByOrder(activeOrder)
                );
            }
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

    @Transactional
    @PostMapping("/purchase")
    public String purchaseOffer(@RequestHeader("X-User-Id") int userId,
                                @ModelAttribute OfferPurchaseDTO purchaseDTO,
                                Model model) {

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        Offer offer = offerRepository.findById(purchaseDTO.getOfferId())
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        int quantity = purchaseDTO.getQuantity();

        offer.setAvailable_quantity(
                offer.getAvailable_quantity() - quantity
        );
        offerRepository.save(offer);

        Order order = orderRepository.findFirstByStoreAndStatus(store, "CREATED");

        if (order == null) {
            order = new Order();
            order.setStore(store);
            order.setOrderDate(LocalDate.now());
            order.setStatus("CREATED");
            order.setPrice(BigDecimal.ZERO);
            orderRepository.save(order);
        }

        boolean differentWholesaler = orderOfferRepository
                .findByOrder(order)
                .stream()
                .anyMatch(orderOffer ->
                        orderOffer.getOffer()
                                .getWholesaler()
                                .getId() != offer.getWholesaler().getId()
                );

        if (differentWholesaler) {
            model.addAttribute(
                    "error",
                    "You can only order products from one wholesaler at a time."
            );
            return "redirect:/offer/account";
        }

        // DODAJEMY PRODUKT DO ZAMÓWIENIA
        OrderOffer orderOffer =
                orderOfferRepository.findByOrderAndOffer(order, offer);

        if (orderOffer == null) {
            orderOffer = new OrderOffer();
            orderOffer.setOrder(order);
            orderOffer.setOffer(offer);
            orderOffer.setQuantity(quantity);
        } else {
            orderOffer.setQuantity(
                    orderOffer.getQuantity() + quantity
            );
        }

        orderOfferRepository.save(orderOffer);


        // AKTUALIZACJA CENY ZAMÓWIENIA
        BigDecimal itemPrice = BigDecimal
                .valueOf(offer.getPrice())
                .multiply(BigDecimal.valueOf(quantity));

        order.setPrice(order.getPrice().add(itemPrice));
        orderRepository.save(order);

        return "redirect:/offer/account";
    }


    @GetMapping("/orders")
    public String myOrders(@RequestHeader("X-User-Id") int userId, Model model) {

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        model.addAttribute(
                "orders",
                orderRepository.findByStore(store)
        );

        return "orders";
    }

    @GetMapping("/wholesaler/orders")
    public String wholesalerOrders(@RequestHeader("X-User-Id") int userId, Model model) {

        User user = userRepository.findById(userId);
        Wholesaler wholesaler = wholesalerRepository.findByUser_Id(user.getId());

        model.addAttribute(
                "orderOffers",
                orderOfferRepository.findByOffer_Wholesaler(wholesaler)
        );

        return "wholesaler-orders";
    }

    private boolean isValidStatusChange(String current, String next) {

        return switch (current) {
            case "CREATED" -> false;
            case "ORDERED" -> next.equals("IN_PROGRESS");
            case "IN_PROGRESS" -> next.equals("SHIPPED");
            case "SHIPPED" -> next.equals("DELIVERED");
            default -> false;
        };
    }


    @PostMapping("/wholesaler/orders/status")
    public String updateOrderStatus(@RequestHeader("X-User-Id") int userId,
                                    @RequestParam int orderId,
                                    @RequestParam String status) {

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

        if (!isValidStatusChange(order.getStatus(), status)) {
            return "redirect:/offer/wholesaler/orders";
        }

        order.setStatus(status);
        orderRepository.save(order);

        return "redirect:/offer/wholesaler/orders";
    }


    @GetMapping("/orders/{orderId}")
    public String orderDetails(@PathVariable int orderId,
                               @RequestHeader("X-User-Id") int userId,
                               Model model) {

        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStore().getId() != store.getId()) {
            return "redirect:/offer/orders";
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
}


