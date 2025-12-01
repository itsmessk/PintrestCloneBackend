package com.infy.pinterest.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.pinterest.dto.ApiResponse;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PinLikeResponseDTO;
import com.infy.pinterest.dto.PinResponseDTO;
import com.infy.pinterest.dto.SavedPinResponseDTO;
import com.infy.pinterest.service.PinInteractionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/pins")
@Tag(name = "Pin Interactions", description = "Pin like and save APIs")
@SecurityRequirement(name = "JWT")
@Slf4j
public class PinInteractionController {

    private final PinInteractionService pinInteractionService;

    @Autowired
    public PinInteractionController(PinInteractionService pinInteractionService) {
        this.pinInteractionService = pinInteractionService;
    }

    // ==================== LIKE ENDPOINTS ====================

    @PostMapping("/{pinId}/like")
    @Operation(summary = "Like a pin")
    public ResponseEntity<ApiResponse<PinLikeResponseDTO>> likePin(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String pinId) {
        log.info("POST /pins/{}/like - User {} liking pin", pinId, userId);

        PinLikeResponseDTO response = pinInteractionService.likePin(userId, pinId);
        return ResponseEntity.ok(ApiResponse.success("Pin liked successfully", response));
    }

    @DeleteMapping("/{pinId}/like")
    @Operation(summary = "Unlike a pin")
    public ResponseEntity<ApiResponse<Object>> unlikePin(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String pinId) {
        log.info("DELETE /pins/{}/like - User {} unliking pin", pinId, userId);

        pinInteractionService.unlikePin(userId, pinId);
        return ResponseEntity.ok(ApiResponse.success("Pin unliked successfully", null));
    }

    @GetMapping("/{pinId}/is-liked")
    @Operation(summary = "Check if user liked a pin")
    public ResponseEntity<ApiResponse<Boolean>> isLiked(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String pinId) {
        log.info("GET /pins/{}/is-liked - Checking if user {} liked pin", pinId, userId);

        Boolean isLiked = pinInteractionService.isLiked(userId, pinId);
        return ResponseEntity.ok(ApiResponse.success("Like status retrieved", isLiked));
    }

    @GetMapping("/liked")
    @Operation(summary = "Get user's liked pins")
    public ResponseEntity<ApiResponse<PaginatedResponse<PinResponseDTO>>> getLikedPins(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /pins/liked - Fetching liked pins for user {}", userId);

        PaginatedResponse<PinResponseDTO> response = pinInteractionService.getLikedPins(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Liked pins retrieved successfully", response));
    }

    // ==================== SAVE ENDPOINTS ====================

    @PostMapping("/{pinId}/save")
    @Operation(summary = "Save a pin")
    public ResponseEntity<ApiResponse<SavedPinResponseDTO>> savePin(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String pinId,
            @RequestParam(required = false) String boardId) {
        log.info("POST /pins/{}/save - User {} saving pin to board {}", pinId, userId, boardId);

        SavedPinResponseDTO response = pinInteractionService.savePin(userId, pinId, boardId);
        return ResponseEntity.ok(ApiResponse.success("Pin saved successfully", response));
    }

    @DeleteMapping("/{pinId}/save")
    @Operation(summary = "Unsave a pin")
    public ResponseEntity<ApiResponse<Object>> unsavePin(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String pinId) {
        log.info("DELETE /pins/{}/save - User {} unsaving pin", pinId, userId);

        pinInteractionService.unsavePin(userId, pinId);
        return ResponseEntity.ok(ApiResponse.success("Pin unsaved successfully", null));
    }

    @GetMapping("/{pinId}/is-saved")
    @Operation(summary = "Check if user saved a pin")
    public ResponseEntity<ApiResponse<Boolean>> isSaved(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String pinId) {
        log.info("GET /pins/{}/is-saved - Checking if user {} saved pin", pinId, userId);

        Boolean isSaved = pinInteractionService.isSaved(userId, pinId);
        return ResponseEntity.ok(ApiResponse.success("Save status retrieved", isSaved));
    }

    @GetMapping("/saved")
    @Operation(summary = "Get user's saved pins")
    public ResponseEntity<ApiResponse<PaginatedResponse<PinResponseDTO>>> getSavedPins(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /pins/saved - Fetching saved pins for user {}", userId);

        PaginatedResponse<PinResponseDTO> response = pinInteractionService.getSavedPins(userId, boardId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Saved pins retrieved successfully", response));
    }
}
