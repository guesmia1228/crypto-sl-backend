package com.nefentus.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class KycImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private KycImageType type;

    private Boolean confirmed = false;

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
