package com.warehouseManagement.demo;


import com.warehouseManagement.demo.dto.OfferFormDTO;
import com.warehouseManagement.demo.dto.OfferPurchaseDTO;
import com.warehouseManagement.demo.entity.*;
import com.warehouseManagement.demo.repo.*;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/test")
    @ResponseBody
    public String account(@RequestHeader("X-User-Id") int userId) {
        return String.valueOf(userId);
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

    @PostMapping("/purchase")
    @ResponseBody
    public ResponseEntity<?> purchaseOffer(
            @RequestHeader("X-User-Id") int userId,
            @RequestBody OfferPurchaseDTO dto
    ) {
        // 1. Sprawdź czy user to STORE
        User user = userRepository.findById(userId);
        if (!"store".equals(user.getRole())) {
            return ResponseEntity.status(403).body("Only stores can buy offers");
        }

        // 2. Pobierz ofertę
        Offer offer = offerRepository.findById(dto.getOfferId())
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        // 3. Walidacja ilości
        if (dto.getQuantity() < offer.getMinimal_quantity()) {
            return ResponseEntity.badRequest()
                    .body("Quantity below minimal order amount");
        }

        if (dto.getQuantity() > offer.getAvailable_quantity()) {
            return ResponseEntity.badRequest()
                    .body("Not enough quantity available");
        }

        // 4. ZMNIEJSZ ILOŚĆ
        offer.setAvailable_quantity(
                offer.getAvailable_quantity() - dto.getQuantity()
        );

        offerRepository.save(offer);

        return ResponseEntity.ok("Offer purchased successfully");
    }



}


