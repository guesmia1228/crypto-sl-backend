package com.nefentus.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Entity
@Table(name = "prd_product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prd_id")
    private Integer id;

    @Column(name = "prd_name")
    private String name;

    @Column(name = "prd_description")
    private String description;

    @Column(name = "prd_price")
    private Float price;

    @Column(name = "prd_picture_path")
    private String picturePath;

    @Column(name = "prd_stock")
    private Integer stock;

    @Column(name = "prd_created_at")
    private Timestamp createdAt;

    @Column(name = "prd_updated_at")
    private Timestamp updatedAt;
}