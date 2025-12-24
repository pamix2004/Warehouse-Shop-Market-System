package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.Producer;
import com.politechnika.warehouseManagement.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducerRepository extends JpaRepository<Producer, Integer> {
}
