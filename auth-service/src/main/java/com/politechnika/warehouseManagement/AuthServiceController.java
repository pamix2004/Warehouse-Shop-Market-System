package com.politechnika.warehouseManagement;


import com.politechnika.warehouseManagement.dto.AuthRequest;
import com.politechnika.warehouseManagement.dto.TokenValidationResponse;
import com.politechnika.warehouseManagement.entity.Store;
import com.politechnika.warehouseManagement.entity.User;
import com.politechnika.warehouseManagement.entity.Wholesaler;
import com.politechnika.warehouseManagement.repo.StoreRepository;
import com.politechnika.warehouseManagement.repo.UserRepository;
import com.politechnika.warehouseManagement.repo.WholesalerRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.hibernate.cfg.JdbcSettings.URL;


@Controller
@RequestMapping("/auth")
public class AuthServiceController {

    @Autowired
    private UserRepository userRepository; // Spring injects it

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private WholesalerRepository wholesalerRepository;



    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private DiscoveryClient discoveryClient;

    public String getJWTServiceUrl() {
        // Get all instances of "jwt-service"
        List<ServiceInstance> instances = discoveryClient.getInstances("jwt-service");
        if (instances != null && !instances.isEmpty()) {
            // Return the URI of the first instance
            return instances.get(0).getUri().toString(); // e.g., http://192.168.1.10:8080
        }
        return null;
    }

    public String getEmailServiceUrl() {
        // Get all instances of "email-service"
        List<ServiceInstance> instances = discoveryClient.getInstances("email-service");
        if (instances != null && !instances.isEmpty()) {
            // Return the URI of the first instance
            return instances.get(0).getUri().toString(); // e.g., http://192.168.1.10:8080
        }
        return null;
    }



    @GetMapping("/login")
    public String loginPage(@CookieValue(value = "auth_token", required = false) String authToken, Model model) {
        model.addAttribute("isAuthenticated", authToken != null);
        return "login"; // resolves to login.jsp or login.html
    }


    @PostMapping("/login")
    public String login(@RequestParam("email") String email,
                                   @RequestParam("password") String password,
                                   HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

             CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();


            RestTemplate restTemplate = new RestTemplate();
            String token = restTemplate.getForObject(
                    getJWTServiceUrl()+"/jwt/createJWTToken?id={id}",
                    String.class,
                    cud.getID()
            );

            //  set cookie ONLY after successful auth + token generation
            ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge((Duration.ofHours(1)))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return "redirect:/offer/account";

        } catch (org.springframework.security.authentication.DisabledException e) {
            return "redirect:/auth/login?error=Account not verified/active.";
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return "redirect:/auth/login?error=Invalid username or password.";
        } catch (org.springframework.security.core.AuthenticationException e) {
            return "redirect:/auth/login?error=Authentication failed.";
        }
    }

    /**
     * Function that removes the auth_token and redirects to /auth/login
     * */
    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {

        ResponseCookie deleteCookie = ResponseCookie.from("auth_token", "")
                .httpOnly(true)
                .path("/")      // MUST match original
                .maxAge(0)      // DELETE
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return "redirect:/auth/login";
    }

    /**
     * This validates a given token
     * @return JSON: { "valid": true/false, "userId": id/-1 }
     */
    @GetMapping("/validateToken")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestParam("token") String token) {

        try {
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Integer> resp = restTemplate.postForEntity(
                    getJWTServiceUrl()+"/jwt/verifyJWTToken?token={token}",
                    null,
                    Integer.class,
                    token
            );

            int userId = resp.getBody();

            System.out.println("Valid token for userId=" + userId);

            return ResponseEntity.ok(
                    new TokenValidationResponse(true, userId)
            );

        } catch (Exception e) {
            System.out.println("Invalid token");

            return ResponseEntity.ok(
                    new TokenValidationResponse(false, -1)
            );
        }
    }

    /**Its only for debugging, you can remove it, if I forgot*/
    @GetMapping("/cookieTest")
    @ResponseBody
    public ResponseEntity<?> testCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("access_token", "mojTokenJWT")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().body("Logged in");
    }


    @GetMapping("/register")
    public String getRegister(@CookieValue(value = "auth_token", required = false) String authToken, Model model){
        model.addAttribute("isAuthenticated", authToken != null);
        return  "register";
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
                return "redirect:/auth/register?error=Account with this email is already active";
            }
            //User exists but has not been confirmed
            else{
                sendConfirmationMail(email,userEntity.getId());



                //We notify a user that mail was sent
                return "redirect:/auth/register?mailSent=Email has been sent.";

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
            System.out.println("Raw password is: " + rawPassword);

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

            return "redirect:/auth/register?mailSent=Email has been sent.";
        }
    }

    @GetMapping("/verify")
    /**
     * Method made for veryfing the JWT token. If it's valid it will be take action to change is_active in users table inside database. If it's invalid
     * user will be redirected and shown a message.
     * */
    public String verifyAccount(@RequestParam String token){
        //We validate JWT token using jwt-service (microservice). If it's correct it will return userId, if it's invalid it will throw HTTP status of 504 or 401.
        try{
            //User jwt-service
            RestTemplate restTemplate = new RestTemplate();
            System.out.println("Testing rest template");
            ResponseEntity<Integer> resp = restTemplate.postForEntity(getJWTServiceUrl()+"/jwt/verifyJWTToken?token={token}",null,Integer.class,token);
            int id = Integer.parseInt((String.valueOf(resp.getBody())));
            int statusCode = resp.getStatusCode().value();
            System.out.println("status=" + statusCode + " body=" + id);


            //If no error was thrown it's all cool and we can update the database

            User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

            user.setIs_active(true);   // <---- update field
            userRepository.save(user); // <---- save to DB

            System.out.println("User activated: " + id);

            System.out.println("poprawny token dla id: " + id);

            System.out.println("token: " + token);
            return "redirect:/auth/login?successMessage=Your account has been verified.";




        }
        //Error 504 - expired JWT
        catch (org.springframework.web.client.HttpServerErrorException.GatewayTimeout e) {
            return "redirect:/auth/register?failMessage=Your link has expired, register again.";
        }

        //Error 401 - not authorized
        catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            return "redirect:/auth/register?failMessage=Your link is invalid, register again.";
        }

        catch (Exception e) {
            return "redirect:/auth/register?failMessage=Your link is corrupted!";
        }


    }

    private void sendConfirmationMail(String recipient,int id){
        //String token = jwtService.createVerificationToken(id);
        RestTemplate restTemplate = new RestTemplate();
        String token = restTemplate.getForObject(
                getJWTServiceUrl()+"/jwt/createJWTToken?id={id}",
                String.class,
                id
        );

        String link = "http://localhost:8085/auth/verify?token="+token;
        System.out.println("generated link: " + link);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("toEmail",recipient);
        params.add("subject", "Verification Confirmation");
        params.add("body",link);

        String URL = getEmailServiceUrl()+"/email/sendEmail";
        //Calling the other service (email-service)
        String result = new RestTemplate().postForObject(URL, params, String.class);
    }







}
