package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}
