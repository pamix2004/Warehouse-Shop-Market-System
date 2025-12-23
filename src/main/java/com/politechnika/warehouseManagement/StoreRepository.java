package com.politechnika.warehouseManagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface StoreRepository extends JpaRepository<Store, Integer> {
    Store findByUser_Id(int userId);
}
