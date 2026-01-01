package com.warehouseManagement.demo.repo;


import com.warehouseManagement.demo.entity.Producer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducerRepository extends JpaRepository<Producer, Integer> {
    Producer findById(int producerId);
}
