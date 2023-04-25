package com.nefentus.api.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aff_affiliate")
public class Affiliate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aff_id")
    private Integer id;

    @Column(name = "aff_affiliate_link")
    private String affiliateLink;

    @Column(name = "aff_commission_rate")
    private Float commissionRate;

    @Column(name = "aff_created_at")
    private Timestamp createdAt;

    @ManyToOne
    @JoinColumn(name = "aff_user_id", referencedColumnName = "id")
    private User user;
}
