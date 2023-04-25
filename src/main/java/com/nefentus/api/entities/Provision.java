package com.nefentus.api.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;
@Data
@NoArgsConstructor
@Entity
@Table(name = "prv_provision")
public class Provision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prv_id")
    private Integer id;

    @Column(name = "prv_affiliate_id")
    private Integer affiliateId;

    @Column(name = "prv_transaction_id")
    private Long transactionId;

    @Column(name = "prv_commission_amount")
    private Float commissionAmount;

    @Column(name = "prv_created_at")
    private Timestamp createdAt;

    @ManyToOne
    @JoinColumn(name = "prv_affiliate_id", referencedColumnName = "aff_id", insertable = false, updatable = false)
    private Affiliate affiliate;

    @ManyToOne
    @JoinColumn(name = "prv_transaction_id", referencedColumnName = "tra_id", insertable = false, updatable = false)
    private Transaction transaction;
}