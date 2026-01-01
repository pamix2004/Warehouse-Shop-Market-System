package com.politechnika.warehouseManagement.service;

import com.politechnika.warehouseManagement.entity.User;
import com.politechnika.warehouseManagement.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    public void saveUser(User user){
        //We make sure to save the password using our encoder
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        repository.save(user);
        System.out.println("User has been saved");
    }

}
