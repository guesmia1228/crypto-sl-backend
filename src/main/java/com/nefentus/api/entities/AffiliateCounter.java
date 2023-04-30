package com.nefentus.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AffiliateCounter {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    public Long id;

    @ManyToOne
    @JoinColumn(name = "aff_c_id", referencedColumnName = "id")
    public User user;

    public Timestamp timestamp;
}
