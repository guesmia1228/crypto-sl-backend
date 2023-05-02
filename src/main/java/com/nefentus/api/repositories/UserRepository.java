package com.nefentus.api.repositories;

import com.nefentus.api.entities.ERole;
import com.nefentus.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByEmail(String email);

    Long countByCreatedAtAfter(Timestamp timestamp);

    Optional<User> findByToken(String token);

    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByAffiliateLink(String AffiliateLink);

    Long countAllByActive(boolean active);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") ERole roleName);


    @Query("SELECT COUNT(ac) FROM User ac WHERE ac.createdAt >= :startDate AND ac.createdAt <= :endDate")
    Long countClicksByDateRange(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    default double calculateAllClickPercentageInLast30Days() {
        Timestamp begin = Timestamp.valueOf(LocalDateTime.of(2022, 1, 1, 1, 1, 1));
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp startDate = new Timestamp(System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L);

        Long totalClicks = countClicksByDateRange(begin, startDate);
        Long totalClicksInLast30Days = countClicksByDateRange(startDate, now);
        double clickPercentage = 0.0;
        if (totalClicks == 0 && totalClicksInLast30Days > 0) {
            clickPercentage = totalClicksInLast30Days * 100;
        } else {
            clickPercentage = ((double) totalClicksInLast30Days - totalClicks) / totalClicks * 100.0;
        }

        return Math.round(clickPercentage * 100.0) / 100.0; // Round to two decimal places
    }
}
