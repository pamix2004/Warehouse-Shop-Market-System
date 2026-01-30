package com.warehouseManagement.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cart")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Integer cartId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", referencedColumnName = "id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wholesaler_id", referencedColumnName = "id", nullable = false)
    private Wholesaler wholesaler;

    // Constructors
    public Cart() {
    }

    public Cart(Store store, Wholesaler wholesaler) {
        this.store = store;
        this.wholesaler = wholesaler;
    }

    // Getters and Setters
    public Integer getCartId() {
        return cartId;
    }

    public void setCartId(Integer cartId) {
        this.cartId = cartId;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Wholesaler getWholesaler() {
        return wholesaler;
    }

    public void setWholesaler(Wholesaler wholesaler) {
        this.wholesaler = wholesaler;
    }
}
