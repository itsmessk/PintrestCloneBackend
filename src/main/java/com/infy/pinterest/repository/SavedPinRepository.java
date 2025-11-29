package com.infy.pinterest.repository;

import com.infy.pinterest.entity.SavedPin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPinRepository extends JpaRepository<SavedPin, String> {
    
    Optional<SavedPin> findByPinIdAndUserId(String pinId, String userId);
    
    Optional<SavedPin> findByPinIdAndUserIdAndBoardId(String pinId, String userId, String boardId);
    
    Boolean existsByPinIdAndUserId(String pinId, String userId);
    
    Boolean existsByPinIdAndUserIdAndBoardId(String pinId, String userId, String boardId);
    
    Long countByPinId(String pinId);
    
    Page<SavedPin> findByUserId(String userId, Pageable pageable);
    
    Page<SavedPin> findByUserIdAndBoardId(String userId, String boardId, Pageable pageable);
    
    @Query("SELECT sp.pinId FROM SavedPin sp WHERE sp.userId = :userId")
    Page<String> findPinIdsByUserId(String userId, Pageable pageable);
}
