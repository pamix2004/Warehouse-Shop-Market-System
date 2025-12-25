package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.Offer;
import com.politechnika.warehouseManagement.entity.Wholesaler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Integer> {
    Offer findById(int storeId);
    List<Offer> findByWholesaler(Wholesaler wholesaler);
}
