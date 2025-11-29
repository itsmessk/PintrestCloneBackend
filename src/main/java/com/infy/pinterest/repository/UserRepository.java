package com.infy.pinterest.repository;


import com.infy.pinterest.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {


    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);

    // New: Search users by username or full name
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "u.isActive = true")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);


// Get username suggestions
    @Query("SELECT DISTINCT u.username FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT(:keyword, '%')) " +
            "AND u.isActive = true")
    List<String> findUsernameSuggestions(@Param("keyword") String keyword, Pageable pageable);

}
