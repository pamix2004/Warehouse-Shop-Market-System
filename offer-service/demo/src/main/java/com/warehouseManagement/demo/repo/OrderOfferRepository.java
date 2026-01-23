package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.entity.Order;
import com.warehouseManagement.demo.entity.Offer;
import com.warehouseManagement.demo.entity.OrderOffer;
import com.warehouseManagement.demo.entity.Wholesaler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderOfferRepository extends JpaRepository<OrderOffer, Integer> {

    List<OrderOffer> findByOffer_Wholesaler(Wholesaler wholesaler);
    List<OrderOffer> findByOrder(Order order);
    OrderOffer findByOrderAndOffer(Order order, Offer offer);


}
