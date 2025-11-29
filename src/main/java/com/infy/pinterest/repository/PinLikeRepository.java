package com.infy.pinterest.repository;

import com.infy.pinterest.entity.PinLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PinLikeRepository extends JpaRepository<PinLike, String> {
    
    Optional<PinLike> findByPinIdAndUserId(String pinId, String userId);
    
    Boolean existsByPinIdAndUserId(String pinId, String userId);
    
    Long countByPinId(String pinId);
    
    Page<PinLike> findByUserId(String userId, Pageable pageable);
    
    @Query("SELECT pl.pinId FROM PinLike pl WHERE pl.userId = :userId")
    Page<String> findPinIdsByUserId(String userId, Pageable pageable);
}
