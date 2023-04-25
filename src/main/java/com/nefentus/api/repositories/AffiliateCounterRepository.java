package com.nefentus.api.repositories;

import com.nefentus.api.entities.AffiliateCounter;
import com.nefentus.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
public interface AffiliateCounterRepository extends JpaRepository<AffiliateCounter, Long> {
    Long countByTimestampAfter(Timestamp timestamp);
}
