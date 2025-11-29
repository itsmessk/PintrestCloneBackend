package com.infy.pinterest.repository;

import com.infy.pinterest.entity.Pin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PinRepository extends JpaRepository<Pin, String>  {
    Page<Pin> findByUserId(String userId, Pageable pageable);
    List<Pin> findByUserId(String userId);

    Page<Pin> findByBoardId(String boardId, Pageable pageable);

    Page<Pin> findByUserIdAndIsDraft(String userId, Boolean isDraft, Pageable pageable);

    Optional<Pin> findByPinIdAndUserId(String pinId, String userId);

    Long countByUserId(String userId);

    Long countByBoardId(String boardId);

    @Query("SELECT p FROM Pin p WHERE p.visibility = 'PUBLIC' AND p.isDraft = false")
    Page<Pin> findAllPublicPins(Pageable pageable);

//    @Query("SELECT p FROM Pin p WHERE (p.title LIKE %:keyword% OR p.description LIKE %:keyword%)" + "AND p.visibility = 'PUBLIC' AND p.isDraft = false")
//    Page<Pin> searchPins(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Pin p WHERE " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND p.visibility = 'PUBLIC' AND p.isDraft = false")
    Page<Pin> searchPins(@Param("keyword") String keyword, Pageable pageable);

    // Search with category filter
    @Query("SELECT p FROM Pin p JOIN Board b ON p.boardId = b.boardId WHERE " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND b.category = :category " +
            "AND p.visibility = 'PUBLIC' AND p.isDraft = false")
    Page<Pin> searchPinsByCategory(@Param("keyword") String keyword,
                                   @Param("category") String category,
                                   Pageable pageable);
    // Get popular pins (by saves and likes)
    @Query("SELECT p FROM Pin p WHERE p.visibility = 'PUBLIC' AND p.isDraft = false " +
            "ORDER BY (p.saveCount + p.likeCount) DESC")
    Page<Pin> findPopularPins(Pageable pageable);

    // Search suggestions based on keywords
    @Query("SELECT DISTINCT p.title FROM Pin p WHERE " +
            "LOWER(p.title) LIKE LOWER(CONCAT(:keyword, '%')) " +
            "AND p.visibility = 'PUBLIC' AND p.isDraft = false")
    List<String> findTitleSuggestions(@Param("keyword") String keyword, Pageable pageable);

    // Get all accessible pins for a user (public pins + user's own non-draft pins + collaborative board non-draft pins)
    @Query("SELECT DISTINCT p FROM Pin p WHERE " +
            "((p.visibility = 'PUBLIC' AND p.isDraft = false) OR " +
            "(p.userId = :userId AND p.isDraft = false) OR " +
            "(p.boardId IN :collaborativeBoardIds AND p.isDraft = false))")
    Page<Pin> findAccessiblePinsForUser(@Param("userId") String userId,
                                         @Param("collaborativeBoardIds") List<String> collaborativeBoardIds,
                                         Pageable pageable);

}
