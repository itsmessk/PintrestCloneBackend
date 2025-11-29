package com.infy.pinterest.repository;

import com.infy.pinterest.entity.ShowcasePin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowcasePinRepository extends JpaRepository<ShowcasePin, String> {

    List<ShowcasePin> findByShowcaseIdOrderByDisplayOrderAsc(String showcaseId);

    Boolean existsByShowcaseIdAndPinId(String showcaseId, String pinId);

    Optional<ShowcasePin> findByShowcaseIdAndPinId(String showcaseId, String pinId);

    Integer countByShowcaseId(String showcaseId);
}
