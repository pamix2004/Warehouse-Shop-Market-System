package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.entity.Cart;
import com.warehouseManagement.demo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    List<Cart> findByStore(Store store);
    // List<Cart> findByWholesaler(Wholesaler wholesaler);
    List<Cart> findByStore_IdAndWholesaler_Id(Integer storeId, Integer wholesalerId);
    // Checks if given cart Id belongs to the storeId
    boolean existsByCartIdAndStore_Id(Integer cartId, Integer storeId);
}
