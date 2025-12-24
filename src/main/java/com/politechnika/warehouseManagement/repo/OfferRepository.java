package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Integer> {
    Offer findById(int storeId);
}
