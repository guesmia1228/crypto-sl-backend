package com.nefentus.api.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
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

    @Column(name = "s3_key")
    private String s3Key;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
