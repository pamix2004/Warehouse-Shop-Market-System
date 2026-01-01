package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Integer> {
    Store findByUser_Id(int userId);
}
