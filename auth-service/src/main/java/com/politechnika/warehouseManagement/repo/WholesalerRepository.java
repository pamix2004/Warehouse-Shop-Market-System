package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.Wholesaler;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WholesalerRepository extends JpaRepository<Wholesaler, Integer> {
    Wholesaler findByUser_Id(int user_id);
}
