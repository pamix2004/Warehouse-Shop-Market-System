package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.entity.CartOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface CartOfferRepository extends JpaRepository<CartOffer, Integer> {
    List<CartOffer> findAllByCart_Store_IdAndCart_Wholesaler_Id(Integer storeId, Integer wholesalerId);
    Optional<CartOffer> findByCart_CartIdAndOffer_Id(Integer cartId, Integer offerId);
    List<CartOffer> findAllByCart_CartId(Integer cartId);
    @Modifying
    void deleteByOffer_Id(Integer offerId);
}

