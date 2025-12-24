package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {
}
