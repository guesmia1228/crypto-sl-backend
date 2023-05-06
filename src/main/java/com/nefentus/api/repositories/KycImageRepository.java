package com.nefentus.api.repositories;

import com.nefentus.api.entities.KycImage;
import com.nefentus.api.entities.KycImageType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycImageRepository extends JpaRepository<KycImage, Long> {

    KycImage findKycImageByTypeAndUser_Id(KycImageType type, Long userId);
}
