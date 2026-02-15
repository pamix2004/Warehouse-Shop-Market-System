package com.warehouseManagement.demo.entity;

import com.warehouseManagement.demo.OfferState;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "offer")
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int available_quantity;
    private int minimal_quantity;
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "wholesaler_id")
    private Wholesaler wholesaler;

    // ===== Getters and Setters =====


    public int getId() {
        return id;
    }

    public int getMinimal_quantity() {
        return minimal_quantity;
    }

    public Product getProduct() {
        return product;
    }

    public Wholesaler getWholesaler() {
        return wholesaler;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getAvailable_quantity() {
        return available_quantity;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAvailable_quantity(int available_quantity) {
        this.available_quantity = available_quantity;
    }

    public void setMinimal_quantity(int minimal_quantity) {
        this.minimal_quantity = minimal_quantity;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setWholesaler(Wholesaler wholesaler) {
        this.wholesaler = wholesaler;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private OfferState state;

    // Getter and Setter
    public OfferState getState() {
        return state;
    }

    public void setState(OfferState state) {
        this.state = state;
    }

    // ===== toString() =====
    @Override
    public String toString() {
        return "Offer{" +
                "id=" + id +
                ", available_quantity='" + available_quantity + '\'' +
                ", minimal_quantity='" + minimal_quantity + '\'' +
                ", price='" + price + '\'' +
                ", product_id='" + product.getId() + '\'' +
                ", wholesaler_id='" + wholesaler.getUser().getId() + '\'' +
                '}';
    }
}
