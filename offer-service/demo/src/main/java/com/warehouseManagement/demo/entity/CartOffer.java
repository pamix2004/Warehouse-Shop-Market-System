package com.warehouseManagement.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_offer")
public class CartOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_product_id") // matches PK in DB
    private Integer cartProductId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", referencedColumnName = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", referencedColumnName = "id", nullable = false)
    private Offer offer;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public CartOffer() {}

    public CartOffer(Cart cart, Offer offer, Integer quantity) {
        this.cart = cart;
        this.offer = offer;
        this.quantity = quantity;
    }



    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Offer getOffer() {
        return offer;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
