package com.warehouseManagement.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_offer")
public class OrderOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_offer_id")
    private Integer orderOfferId;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Column(nullable = false)
    private Integer quantity;

    // ===== getters =====

    public Integer getOrderOfferId() {
        return orderOfferId;
    }

    public Order getOrder() {
        return order;
    }

    public Offer getOffer() {
        return offer;
    }

    public Integer getQuantity() {
        return quantity;
    }

    // ===== setters =====

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
