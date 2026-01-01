package com.politechnika.warehouseManagement.service;

import com.politechnika.warehouseManagement.CustomUserDetails;
import com.politechnika.warehouseManagement.entity.User;
import com.politechnika.warehouseManagement.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException(
                    "user not found with name :" + username
            );
        }

        return new CustomUserDetails(user);
    }



}
