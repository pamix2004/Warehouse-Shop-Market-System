package com.warehouseManagement.demo.repo;


import com.warehouseManagement.demo.entity.Offer;
import com.warehouseManagement.demo.entity.Wholesaler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Integer> {
    Offer findById(int storeId);
    List<Offer> findByWholesaler(Wholesaler wholesaler);
}
