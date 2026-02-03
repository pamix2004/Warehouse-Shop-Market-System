package com.warehouseManagement.demo.repo;

import com.warehouseManagement.demo.entity.Order;
import com.warehouseManagement.demo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByStore(Store store);
    Order findFirstByStoreAndStatus(Store store, String status);

    @Query(value = """
    SELECT IF(COUNT(*) > 0, 1, 0)
    FROM orders o
    JOIN order_offer oo ON o.order_id = oo.order_id
    JOIN offer offr ON offr.id = oo.offer_id
    WHERE o.order_id = :orderId
      AND offr.wholesaler_id = :wholesalerId
""", nativeQuery = true)
    long doesOrderBelongToWholesaler(@Param("orderId") int orderId,
                                    @Param("wholesalerId") int wholesalerId);

    boolean existsByorderIdAndStoreId(Integer orderId, Integer storeId);



}
