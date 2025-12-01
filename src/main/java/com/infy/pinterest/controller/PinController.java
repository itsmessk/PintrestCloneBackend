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
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PinCreationDTO;
import com.infy.pinterest.dto.PinDraftDTO;
import com.infy.pinterest.dto.PinResponseDTO;
import com.infy.pinterest.dto.PinUpdateDTO;
import com.infy.pinterest.service.PinService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/pins")
@Tag(name = "Pins", description = "Pin management APIs")
@SecurityRequirement(name = "JWT")
@Slf4j
public class PinController {

    private final PinService pinService;

    @Autowired
    public PinController(PinService pinService) {
        this.pinService = pinService;
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new pin")
    public ResponseEntity<ApiResponse<PinResponseDTO>> createPin(
            @RequestHeader("X-User-Id") String userId, 
            @RequestParam("title") String title, 
            @RequestParam(value = "description", required = false) String description, 
            @RequestParam(value = "sourceUrl", required = false) String sourceUrl, 
            @RequestParam("boardId") String boardId, 
            @RequestParam("visibility") String visibility, 
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        log.info("POST /pins - Creating pin for user: {}", userId);
        PinCreationDTO pinDTO = new PinCreationDTO();
        pinDTO.setTitle(title);
        pinDTO.setDescription(description);
        pinDTO.setSourceUrl(sourceUrl);
        pinDTO.setBoardId(boardId);
        pinDTO.setVisibility(visibility);
        pinDTO.setImageUrl(imageUrl);
        PinResponseDTO response = pinService.createPin(userId, pinDTO, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pin created successfully", response));
    }
    @PostMapping("/draft")
    @Operation(summary = "Create a pin draft")
    public ResponseEntity<ApiResponse<PinResponseDTO>> createPinDraft(@RequestHeader("X-User-Id") String userId, @Valid @RequestBody PinDraftDTO draftDTO) {
        log.info("POST /pins/draft - Creating pin draft for user: {}", userId);
        PinResponseDTO response = pinService.createPinDraft(userId, draftDTO);
        return ResponseEntity .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Draft saved successfully", response));
    }
    @PutMapping("/{pinId}")
    @Operation(summary = "Update a pin")
    public ResponseEntity<ApiResponse<PinResponseDTO>> updatePin( @RequestHeader("X-User-Id") String userId, @PathVariable String pinId,@Valid @RequestBody PinUpdateDTO updateDTO) {
        log.info("PUT /pins/{} - Updating pin", pinId);
        PinResponseDTO response = pinService.updatePin(userId, pinId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("Pin updated successfully", response));
    }
    @DeleteMapping("/{pinId}")
    @Operation(summary = "Delete a pin")
    public ResponseEntity<ApiResponse<Object>> deletePin(@RequestHeader("X-User-Id") String userId, @PathVariable String pinId) {
        log.info("DELETE /pins/{} - Deleting pin", pinId);
        pinService.deletePin(userId, pinId);
        return ResponseEntity .ok(ApiResponse.success("Pin deleted successfully", null));
    }
    @GetMapping("/{pinId}")
    @Operation(summary = "Get pin by ID")
    public ResponseEntity<ApiResponse<PinResponseDTO>> getPinById(
            @PathVariable String pinId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("GET /pins/{} - Fetching pin details", pinId);
        PinResponseDTO response = pinService.getPinById(pinId, userId);
        return ResponseEntity                .ok(ApiResponse.success("Pin retrieved successfully", response));
    }
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's pins")
    public ResponseEntity<ApiResponse<PaginatedResponse<PinResponseDTO>>> getUserPins(
            @PathVariable String userId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        log.info("GET /pins/user/{} - Fetching user pins", userId);
        PaginatedResponse<PinResponseDTO> response = pinService.getUserPins(userId, requestingUserId, page, size,sort);
        return ResponseEntity                .ok(ApiResponse.success("Pins retrieved successfully", response));
    }
    @GetMapping("/board/{boardId}")
    @Operation(summary = "Get pins by board ID")
    public ResponseEntity<ApiResponse<PaginatedResponse<PinResponseDTO>>> getBoardPins(
            @PathVariable String boardId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /pins/board/{} - Fetching board pins", boardId);
        PaginatedResponse<PinResponseDTO> response = pinService.getBoardPins(boardId, requestingUserId, page,size);
        return ResponseEntity                .ok(ApiResponse.success("Board pins retrieved successfully", response));
    }
    @GetMapping("/drafts")
    @Operation(summary = "Get user's draft pins")
    public ResponseEntity<ApiResponse<PaginatedResponse<PinResponseDTO>>> getUserDrafts(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /pins/drafts - Fetching draft pins for user: {}", userId);
        PaginatedResponse<PinResponseDTO> response = pinService.getUserDrafts(userId, userId, page,size);
        return ResponseEntity                .ok(ApiResponse.success("Draft pins retrieved successfully", response));
    }

    @GetMapping("/public")
    public ResponseEntity<PaginatedResponse<PinResponseDTO>> getPublicPins(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PaginatedResponse<PinResponseDTO> response = pinService.getPublicPins(requestingUserId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to search pins by keyword with pagination.
     * Maps to the service method: searchPins(String keyword, int page, int size)
     * Example URL: GET /api/v1/pins/search?keyword=nature&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<PinResponseDTO>> searchPins(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (keyword == null || keyword.trim().isEmpty()) {
            // Handle cases where keyword is missing or empty if needed
            return ResponseEntity.badRequest().build();
        }

        PaginatedResponse<PinResponseDTO> response = pinService.searchPins(keyword, page, size);
        return ResponseEntity.ok(response);
    }
}
