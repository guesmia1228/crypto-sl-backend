package com.nefentus.api.repositories;

import com.nefentus.api.entities.Affiliate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public interface AffiliateRepository extends JpaRepository<Affiliate, Integer> {
    @Query("SELECT COUNT(a) FROM Affiliate a WHERE a.affiliateLink = :affiliateLink")
    Long countAffiliatesByLink(@Param("affiliateLink") String affiliateLink);

    @Query("SELECT COUNT(a) FROM Affiliate a WHERE a.affiliateLink = :affiliateLink AND a.createdAt >= :startDate AND a.createdAt <= :endDate")
    Long countAffiliatesByLinkSinceDate(@Param("affiliateLink") String affiliateLink, @Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    default double calculateNewAffiliatesPercentageInLast30Days(String affiliateLink) {
        Timestamp startDate = new Timestamp(System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L);
        Timestamp begin = Timestamp.valueOf(LocalDateTime.of(2022,1,1,1,1,1));
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Long totalAffiliates = countAffiliatesByLinkSinceDate(affiliateLink, begin, startDate);
        Long newAffiliates = countAffiliatesByLinkSinceDate(affiliateLink, startDate, now);
        double clickPercentage = 0.0;
        if(totalAffiliates == 0 && newAffiliates > 0){
            clickPercentage = newAffiliates * 100;
        }else{
            clickPercentage = ((double) newAffiliates - totalAffiliates) / totalAffiliates * 100.0;
        }
        return Math.round(clickPercentage * 100.0) / 100.0; // Round to two decimal places
    }

}
