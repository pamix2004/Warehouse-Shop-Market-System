package com.warehouseManagement.demo.repo;


import com.warehouseManagement.demo.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeRepository extends JpaRepository<ProductType, Integer> {
}
