package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.entity.Cart;
import com.warehouseManagement.demo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = "SELECT c.cart_id FROM payment p " +
            "INNER JOIN payment_order po ON p.payment_id = po.payment_id " +
            "INNER JOIN orders o ON o.order_id = po.order_id " +
            "INNER JOIN cart c ON c.store_id = o.store_id AND c.wholesaler_id = o.wholesaler_id " +
            "WHERE p.payment_id = :paymentId",
            nativeQuery = true)
    List<Integer> findCartIdsNative(@Param("paymentId") int paymentId);



    @Query(value = "SELECT EXISTS (" +
            "  SELECT 1 FROM users u " +
            "  JOIN store s ON s.user_id = u.id " +
            "  JOIN cart c ON c.store_id = s.id " +
            "  JOIN cart_offer co ON co.cart_id = c.cart_id " +
            "  WHERE u.id = :userId" +
            ")", nativeQuery = true)
    long hasActiveBasket(@Param("userId") Integer userId);

}
