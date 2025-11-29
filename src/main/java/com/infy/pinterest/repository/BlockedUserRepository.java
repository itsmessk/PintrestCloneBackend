package com.infy.pinterest.repository;


import com.infy.pinterest.entity.BlockedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, String> {

    Boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

    Optional<BlockedUser> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    Page<BlockedUser> findByBlockerId(String blockerId, Pageable pageable);

    void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);
}
