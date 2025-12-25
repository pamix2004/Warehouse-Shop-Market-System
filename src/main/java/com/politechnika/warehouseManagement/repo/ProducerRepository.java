package com.politechnika.warehouseManagement.repo;

import com.politechnika.warehouseManagement.entity.Producer;
import com.politechnika.warehouseManagement.entity.Store;
import com.politechnika.warehouseManagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducerRepository extends JpaRepository<Producer, Integer> {
    Producer findById(int producerId);
}
