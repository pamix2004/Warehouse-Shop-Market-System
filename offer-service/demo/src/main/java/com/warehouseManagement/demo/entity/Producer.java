package com.warehouseManagement.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "producer")
public class Producer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String producer_name;
    private String headquarter_address;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setHeadquarter_address(String headquarter_address) {
        this.headquarter_address = headquarter_address;
    }
    public String getHeadquarter_address() {
        return headquarter_address;
    }

    public void setProducer_name(String producer_name) {
        this.producer_name = producer_name;
    }

    public String getProducer_name() {
        return producer_name;
    }

    // ===== toString() =====
    @Override
    public String toString() {
        return "Producer{" +
                "id=" + id +
                ", producer_name='" + producer_name + '\'' +
                ", headquarter_address='" + headquarter_address + '\'' +
                '}';
    }
}
