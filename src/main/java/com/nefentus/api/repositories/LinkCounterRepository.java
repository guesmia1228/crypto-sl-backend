package com.nefentus.api.repositories;

import com.nefentus.api.entities.LinkCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;

public interface LinkCounterRepository extends JpaRepository<LinkCounter, Long> {

    Long countByUserEmail(String email);

    Long countByTimestampAfterAndUserEmail(Timestamp time, String email);

    Long countByTimestampAfter(Timestamp time);

}
