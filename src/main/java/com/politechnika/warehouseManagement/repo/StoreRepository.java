package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Integer> {
    Store findByUser_Id(int userId);
}
