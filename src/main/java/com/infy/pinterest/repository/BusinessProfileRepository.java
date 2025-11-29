package com.infy.pinterest.repository;

import com.infy.pinterest.entity.BusinessProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, String> {

    Optional<BusinessProfile> findByUserId(String userId);

    Boolean existsByUserId(String userId);

    @Query("SELECT b FROM BusinessProfile b WHERE " +
            "LOWER(b.businessName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<BusinessProfile> searchBusinessProfiles(@Param("keyword") String keyword, Pageable
            pageable);

    Page<BusinessProfile> findByCategory(String category, Pageable pageable);
}
