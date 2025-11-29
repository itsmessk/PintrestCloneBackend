package com.infy.pinterest.repository;

import com.infy.pinterest.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board,String> {

    Page<Board> findByUserId(String userId, Pageable pageable);

    List<Board> findByUserId(String userId);
    
    Page<Board> findByVisibility(Board.Visibility visibility, Pageable pageable);

    Optional<Board> findByBoardIdAndUserId(String boardId, String userId);

    Long countByUserId(String userId);

    @Query("SELECT b FROM Board b WHERE " +
            "(LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND b.visibility = 'PUBLIC'")
    Page<Board> searchBoards(@Param("keyword") String keyword, Pageable pageable);

    // Search boards by category
    @Query("SELECT b FROM Board b WHERE " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND b.category = :category " +
            "AND b.visibility = 'PUBLIC'")
    Page<Board> searchBoardsByCategory(@Param("keyword") String keyword,
                                       @Param("category") String category,
                                       Pageable pageable);

    // Get board name suggestions
    @Query("SELECT DISTINCT b.name FROM Board b WHERE " +
            "LOWER(b.name) LIKE LOWER(CONCAT(:keyword, '%')) " +"AND b.visibility = 'PUBLIC'")
    List<String> findBoardNameSuggestions(@Param("keyword") String keyword, Pageable pageable);


}
