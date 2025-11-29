package com.infy.pinterest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.infy.pinterest.dto.ApiResponse;
import com.infy.pinterest.dto.BoardCreationDTO;
import com.infy.pinterest.dto.BoardResponseDTO;
import com.infy.pinterest.dto.BoardUpdateDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.service.BoardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/boards")
@Tag(name = "Boards", description = "Board management APIs")
@SecurityRequirement(name = "JWT")
@Slf4j
public class BoardController {
    @Autowired
    private BoardService boardService;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "Create a new board")
    public ResponseEntity<ApiResponse<BoardResponseDTO>> createBoard(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam("visibility") String visibility,
            @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
            @RequestParam(value = "bannerImageUrl", required = false) String bannerImageUrl) {
        log.info("POST /boards - Creating board for user: {}", userId);

        BoardCreationDTO boardDTO = new BoardCreationDTO();
        boardDTO.setName(name);
        boardDTO.setDescription(description);
        boardDTO.setCategory(category);
        boardDTO.setVisibility(visibility);
        boardDTO.setBannerImageUrl(bannerImageUrl);

        BoardResponseDTO response = boardService.createBoard(userId, boardDTO, bannerImage);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Board created successfully", response));
    }

    @PutMapping("/{boardId}")
    @Operation(summary = "Update a board")
    public ResponseEntity<ApiResponse<BoardResponseDTO>> updateBoard(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String boardId,
            @Valid @RequestBody BoardUpdateDTO updateDTO) {
        log.info("PUT /boards/{} - Updating board", boardId);

        BoardResponseDTO response = boardService.updateBoard(userId, boardId, updateDTO);
        return ResponseEntity
                .ok(ApiResponse.success("Board updated successfully", response));
    }

    @DeleteMapping("/{boardId}")
    @Operation(summary = "Delete a board")
    public ResponseEntity<ApiResponse<Object>> deleteBoard(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String boardId) {log.info("DELETE /boards/{} - Deleting board", boardId);

        boardService.deleteBoard(userId, boardId);
        return ResponseEntity
                .ok(ApiResponse.success("Board deleted successfully", null));
    }

    @GetMapping("/{boardId}")
    @Operation(summary = "Get board by ID")
    public ResponseEntity<ApiResponse<BoardResponseDTO>> getBoardById(
            @PathVariable String boardId) {
        log.info("GET /boards/{} - Fetching board details", boardId);

        BoardResponseDTO response = boardService.getBoardById(boardId);
        return ResponseEntity
                .ok(ApiResponse.success("Board retrieved successfully", response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's boards")
    public ResponseEntity<ApiResponse<PaginatedResponse<BoardResponseDTO>>> getUserBoards(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        log.info("GET /boards/user/{} - Fetching user boards", userId);

        PaginatedResponse<BoardResponseDTO> response = boardService.getUserBoards(userId, page,
                size, sort);
        return ResponseEntity
                .ok(ApiResponse.success("Boards retrieved successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all public boards for home page")
    public ResponseEntity<ApiResponse<PaginatedResponse<BoardResponseDTO>>> getAllPublicBoards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort) {
        log.info("GET /boards - Fetching all public boards");

        PaginatedResponse<BoardResponseDTO> response = boardService.getAllPublicBoards(page, size, sort);
        return ResponseEntity
                .ok(ApiResponse.success("Public boards retrieved successfully", response));
    }

    @GetMapping("/collaborative")
    @Operation(summary = "Get boards where user is a collaborator")
    public ResponseEntity<ApiResponse<PaginatedResponse<BoardResponseDTO>>> getCollaborativeBoards(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        log.info("GET /boards/collaborative - Fetching collaborative boards for user: {}", userId);

        PaginatedResponse<BoardResponseDTO> response = boardService.getCollaborativeBoards(userId, page, size, sort);
        return ResponseEntity
                .ok(ApiResponse.success("Collaborative boards retrieved successfully", response));
    }

}