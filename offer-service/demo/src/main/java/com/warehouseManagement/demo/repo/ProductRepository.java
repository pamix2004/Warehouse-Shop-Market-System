package com.warehouseManagement.demo.repo;


import com.warehouseManagement.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Checks if a product with this name exists (returns boolean)
    boolean existsByName(String name);
}
