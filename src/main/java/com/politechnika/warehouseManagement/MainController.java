package com.politechnika.warehouseManagement;

import com.politechnika.warehouseManagement.dto.OfferFormDTO;
import com.politechnika.warehouseManagement.entity.*;
import com.politechnika.warehouseManagement.repo.*;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;



@Controller
public class MainController {

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
    private EmailSenderService senderService;
    @Autowired
    private JwtService jwtService;



    @GetMapping("/login")
    public String loginPage() {
        return "login"; // resolves to login.jsp or login.html
    }

    @GetMapping("/account")
    public String account(Model model){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User userEntity = userRepository.findByEmail(email);
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

        return "account";
    }

    @GetMapping("/register")
    public String getRegister(Model model){
        return  "register";
    }

    public void sendConfirmationMail(String recipient,int id){
        String token = jwtService.createVerificationToken(id);
        String link = "http://localhost:8080/verify?token="+token;
        System.out.println("generated link: " + link);


        senderService.sendSimpleEmail(recipient, "Confirmation mail", link);
    }

    @PostMapping("/register")
    public String postRegister(@RequestParam("email") String email, @RequestParam("password") String password,
                               @RequestParam("role") String role, @RequestParam("est_name") String name,
                               @RequestParam("address") String address, Model model){


        User userEntity = userRepository.findByEmail(email);

        //It means that user exists, we have to determine if they are active (confirmed via email)
        if(userEntity != null){
            if(userEntity.isActive()){
                System.out.println("User is already active");
                return "redirect:/register?error=Account with this email is already active";
            }
            //User exists but has not been confirmed
            else{
                sendConfirmationMail(email,userEntity.getId());

                //We notify a user that mail was sent
                return "redirect:/register?mailSent=Email has been sent.";

            }

        }
        //User with given email is not in database
        else{
            //We need to create a new record and send email
            User newUserEntity = new User();
            newUserEntity.setEmail(email);
            newUserEntity.setRole(role);
            String rawPassword = password;
            int strength = 10;
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(strength);
            String hashedPassword = encoder.encode(rawPassword);

            newUserEntity.setPassword(hashedPassword);
            userRepository.save(newUserEntity);
            sendConfirmationMail(email,newUserEntity.getId());

            // Create record in either the store or wholesaler table
            // create record in either the wholesaler or store table
            if (role.equals("store")) {
                Store newStoreEntity = new Store();
                newStoreEntity.setUser(newUserEntity);
                newStoreEntity.setAddress(address);
                newStoreEntity.setName(name);

                storeRepository.save(newStoreEntity);
            } else if (role.equals("wholesaler")) {
                Wholesaler newWholesalerEntity = new Wholesaler();
                newWholesalerEntity.setUser(newUserEntity);
                newWholesalerEntity.setAddress(address);
                newWholesalerEntity.setName(name);

                wholesalerRepository.save(newWholesalerEntity);
            }

            return "redirect:/register?mailSent=Email has been sent.";
        }
    }

    @GetMapping("/verify")
    public String verifyAccount(@RequestParam String token){
        try{
            //If token is valid we assign it, if it's invalid we throw an exception
            int id = jwtService.handleVerification(token);

            //If no error was thrown it's all cool and we can update the database

            User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

            user.setIs_active(true);   // <---- update field
            userRepository.save(user); // <---- save to DB

            System.out.println("User activated: " + id);

            System.out.println("poprawny token dla id: " + id);




            System.out.println("token: " + token);
            return "redirect:/login?successMessage=Your account has been verified.";
        }
        catch (ExpiredJwtException e){
            return "redirect:/register?failMessage=Your link has expired, register again.";
        }
        catch (JwtException e){
            return "redirect:/register?failMessage=Your link is invalid, register again.";
        }
    }

    @PostMapping("/account/offer/add")
    public String addOffer(@ModelAttribute("offerForm") OfferFormDTO form) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email);
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

        return "redirect:/account";
    }

}
