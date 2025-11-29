package com.infy.pinterest.repository;


import com.infy.pinterest.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, String> {

    Boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);

    Page<Follow> findByFollowingId(String followingId, Pageable pageable);

    Page<Follow> findByFollowerId(String followerId, Pageable pageable);

    Long countByFollowingId(String followingId);

    Long countByFollowerId(String followerId);

    void deleteByFollowerIdAndFollowingId(String followerId, String followingId);
}
