package com.politechnika.warehouseManagement;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BasicController {

    @GetMapping("/")
    public String index(@CookieValue(value = "auth_token", required = false) String authToken, Model model) {
        // Logic to check if user is logged in based on the cookie
        model.addAttribute("isAuthenticated", authToken != null);

        System.out.println("Index page accessed. Authenticated: " + (authToken != null));
        return "index";
    }
}