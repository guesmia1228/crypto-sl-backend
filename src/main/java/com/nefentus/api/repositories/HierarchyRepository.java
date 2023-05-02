package com.nefentus.api.repositories;


import com.nefentus.api.entities.Hierarchy;
import com.nefentus.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;

public interface HierarchyRepository extends JpaRepository<Hierarchy, Long> {
    @Query("SELECT h.child FROM Hierarchy h WHERE h.parent.email = :email")
    List<User> findChildByParentEmail(String email);

    List<Hierarchy> findAllByParent(User user);

    Long countByParentEmail(String email);

    Long countByCreatedAtAfterAndParentEmail(Timestamp timestamp, String email);
}
