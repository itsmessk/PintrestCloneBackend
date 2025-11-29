package com.infy.pinterest.repository;


import com.infy.pinterest.entity.BoardCollaborator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardCollaboratorRepository extends JpaRepository<BoardCollaborator, String> {

    Boolean existsByBoardIdAndUserId(String boardId, String userId);

    Optional<BoardCollaborator> findByBoardIdAndUserId(String boardId, String userId);

    List<BoardCollaborator> findByBoardId(String boardId);

    List<BoardCollaborator> findByUserId(String userId);
}
