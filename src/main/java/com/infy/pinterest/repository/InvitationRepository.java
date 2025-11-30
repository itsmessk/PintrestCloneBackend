package com.infy.pinterest.repository;


import com.infy.pinterest.entity.Invitation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, String> {

    Page<Invitation> findByToUserId(String toUserId, Pageable pageable);

    Page<Invitation> findByToUserIdAndStatus(String toUserId,
                                             Invitation.Status status,
                                             Pageable pageable);

    Page<Invitation> findByFromUserId(String fromUserId, Pageable pageable);

    Page<Invitation> findByFromUserIdAndStatus(String fromUserId,
                                               Invitation.Status status,
                                               Pageable pageable);

    Optional<Invitation> findByBoardIdAndToUserIdAndStatus(String boardId,
                                                           String toUserId,
                                                           Invitation.Status status);
}
