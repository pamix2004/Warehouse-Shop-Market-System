package com.politechnika.warehouseManagement;

import jakarta.persistence.*;

@Entity
@Table(name = "store")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int store_id;

    private String name;
    private String address;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // ===== Getters and Setters =====

    public int getStore_id() {
        return store_id;
    }

    public void setStore_id(int store_id) {
        this.store_id = store_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
                "id=" + store_id +
                ", name='" + name + '\'' +
                ", user_id='" + user.getId() + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
