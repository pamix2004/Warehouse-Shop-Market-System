package com.politechnika.warehouseManagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface WholesalerRepository extends JpaRepository<Wholesaler, Integer> {
    Wholesaler findByUser_Id(int user_id);
}
