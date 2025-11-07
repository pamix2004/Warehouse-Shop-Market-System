package com.politechnika.warehouseManagement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Controller
public class MainController {

    @Autowired
    private UserRepository userRepository; // Spring injects it



    @GetMapping("/hello")
    public String hello(Model model){


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User userEntity = userRepository.findByEmail(email);

        model.addAttribute("email",userEntity.getEmail());
        model.addAttribute("id",userEntity.getId());


        return "hello";
    }

}
