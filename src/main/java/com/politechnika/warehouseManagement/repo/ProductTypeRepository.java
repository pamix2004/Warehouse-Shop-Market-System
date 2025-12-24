package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeRepository extends JpaRepository<ProductType, Integer> {
}
