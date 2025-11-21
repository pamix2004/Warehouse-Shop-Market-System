package com.politechnika.warehouseManagement;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private EmailSenderService senderService;
    @Autowired
    private JwtService jwtService;

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // resolves to login.jsp or login.html
    }

    @GetMapping("/hello")
    public String hello(Model model){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User userEntity = userRepository.findByEmail(email);

        model.addAttribute("email",userEntity.getEmail());
        model.addAttribute("id",userEntity.getId());
        return "hello";
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
    public String postRegister(@RequestParam("email") String email, @RequestParam("password") String password,Model model){


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

            String rawPassword = password;
            int strength = 10;
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(strength);
            String hashedPassword = encoder.encode(rawPassword);

            newUserEntity.setPassword(hashedPassword);
            userRepository.save(newUserEntity);
            sendConfirmationMail(email,newUserEntity.getId());
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







}
