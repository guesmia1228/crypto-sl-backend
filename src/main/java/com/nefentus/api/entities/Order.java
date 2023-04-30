package com.nefentus.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ord_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ord_id")
    private Integer id;

    @Column(name = "ord_quantity")
    private Integer quantity;

    @Column(name = "ord_earnings")
    private Float earnings;

    @Column(name = "ord_date")
    private LocalDate date;

    @Column(name = "ord_time")
    private LocalTime time;

    @Column(name = "ord_billing_email")
    private String billingEmail;

    @Column(name = "ord_transaction_id")
    private Long transactionId;

    @Column(name = "ord_created_at")
    private Timestamp createdAt;

    @Column(name = "ord_updated_at")
    private Timestamp updatedAt;

    @ManyToOne
    @JoinColumn(name = "ord_product_id", referencedColumnName = "prd_id", insertable = false, updatable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "ord_transaction_id", referencedColumnName = "tra_id", insertable = false, updatable = false)
    private Transaction transaction;
}