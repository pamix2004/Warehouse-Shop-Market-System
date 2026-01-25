package com.warehouseManagement.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "wholesaler")
public class Wholesaler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String address;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;


    // ===== Getters and Setters =====


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // ===== toString() =====
    @Override
    public String toString() {
        return "Store{" +
                "id=" + id +
                ", user_id=" + user.getId() +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}

