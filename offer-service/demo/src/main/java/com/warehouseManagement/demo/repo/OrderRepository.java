package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.entity.Order;
import com.warehouseManagement.demo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByStore(Store store);
    Order findFirstByStoreAndStatus(Store store, String status);


}
