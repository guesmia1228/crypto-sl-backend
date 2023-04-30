package com.nefentus.api.repositories;

import com.nefentus.api.entities.AffiliateCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public interface AffiliateCounterRepository extends JpaRepository<AffiliateCounter, Long> {
    Long countByTimestampAfter(Timestamp timestamp);
}
