package com.nefentus.api.repositories;

import com.nefentus.api.entities.Clicks;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;

public interface ClickRepository extends JpaRepository<Clicks, Long> {
    Long countByCreatedAtAfter(Timestamp timestamp);
}
