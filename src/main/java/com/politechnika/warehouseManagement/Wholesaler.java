package com.politechnika.warehouseManagement;

import jakarta.persistence.*;

@Entity
@Table(name = "wholesaler")
public class Wholesaler {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int wholesaler_id;

    private String name;
    private String address;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    // ===== Getters and Setters =====


    public int getWholesaler_id() {
        return wholesaler_id;
    }

    public void setWholesaler_id(int wholesaler_id) {
        this.wholesaler_id = wholesaler_id;
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
                "id=" + wholesaler_id +
                ", user_id='" + user.getId() + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}

