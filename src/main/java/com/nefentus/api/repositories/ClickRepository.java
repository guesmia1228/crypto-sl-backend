package com.nefentus.api.repositories;

import com.nefentus.api.entities.Clicks;
import jakarta.persistence.GeneratedValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface ClickRepository extends JpaRepository<Clicks, Long> {
    Long countByCreatedAtAfter(Timestamp timestamp);
}
