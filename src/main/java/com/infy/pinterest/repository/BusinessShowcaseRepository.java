package com.infy.pinterest.repository;
import com.infy.pinterest.entity.BusinessShowcase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface BusinessShowcaseRepository extends JpaRepository<BusinessShowcase, String> {

    List<BusinessShowcase> findByBusinessIdAndIsActive(String businessId, Boolean isActive);

    Page<BusinessShowcase> findByIsActive(Boolean isActive, Pageable pageable);
}
