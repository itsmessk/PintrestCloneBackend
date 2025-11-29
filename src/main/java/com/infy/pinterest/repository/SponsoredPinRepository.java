package com.infy.pinterest.repository;

import com.infy.pinterest.entity.SponsoredPin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface SponsoredPinRepository extends JpaRepository<SponsoredPin, String> {

    List<SponsoredPin> findByBusinessId(String businessId);

    Page<SponsoredPin> findByBusinessId(String businessId, Pageable pageable);

    @Query("SELECT s FROM SponsoredPin s WHERE s.status = 'ACTIVE' " +
            "AND s.startDate <= :currentDate AND s.endDate >= :currentDate")
    Page<SponsoredPin> findActiveSponsoredPins(@Param("currentDate") LocalDate currentDate,
                                               Pageable pageable);

    List<SponsoredPin> findByStatus(SponsoredPin.Status status);
}
