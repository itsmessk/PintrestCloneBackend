package com.infy.pinterest.service;

import com.infy.pinterest.dto.*;
import com.infy.pinterest.entity.*;
import com.infy.pinterest.exception.PinNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PinInteractionService {

    @Autowired
    private PinLikeRepository pinLikeRepository;

    @Autowired
    private SavedPinRepository savedPinRepository;

    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ModelMapper modelMapper;

    // ==================== LIKE OPERATIONS ====================

    @Transactional
    public PinLikeResponseDTO likePin(String userId, String pinId) {
        log.info("User {} liking pin {}", userId, pinId);

        // Verify pin exists
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new PinNotFoundException("Pin not found with ID: " + pinId));

        // Check if already liked
        if (pinLikeRepository.existsByPinIdAndUserId(pinId, userId)) {
            log.warn("User {} already liked pin {}", userId, pinId);
            throw new IllegalStateException("Pin already liked");
        }

        // Create like
        PinLike pinLike = new PinLike();
        pinLike.setPinId(pinId);
        pinLike.setUserId(userId);
        PinLike savedLike = pinLikeRepository.save(pinLike);

        // Update like count
        pin.setLikeCount(pin.getLikeCount() + 1);
        pinRepository.save(pin);

        // Create notification for pin owner (if not liking own pin)
        if (!userId.equals(pin.getUserId())) {
            try {
                notificationService.createNotification(
                        pin.getUserId(),
                        userId,
                        Notification.NotificationType.PIN_LIKED,
                        "liked your pin",
                        pinId,
                        "pin"
                );
            } catch (Exception e) {
                log.error("Failed to create notification for pin like", e);
            }
        }

        log.info("Pin {} liked successfully by user {}", pinId, userId);

        PinLikeResponseDTO response = modelMapper.map(savedLike, PinLikeResponseDTO.class);
        response.setIsLiked(true);
        return response;
    }

    @Transactional
    public void unlikePin(String userId, String pinId) {
        log.info("User {} unliking pin {}", userId, pinId);

        // Verify pin exists
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new PinNotFoundException("Pin not found with ID: " + pinId));

        // Find and delete like
        PinLike pinLike = pinLikeRepository.findByPinIdAndUserId(pinId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));

        pinLikeRepository.delete(pinLike);

        // Update like count
        pin.setLikeCount(Math.max(0, pin.getLikeCount() - 1));
        pinRepository.save(pin);

        log.info("Pin {} unliked successfully by user {}", pinId, userId);
    }

    public Boolean isLiked(String userId, String pinId) {
        return pinLikeRepository.existsByPinIdAndUserId(pinId, userId);
    }

    public PaginatedResponse<PinResponseDTO> getLikedPins(String userId, int page, int size) {
        log.info("Fetching liked pins for user: {}", userId);

        Sort sort = Sort.by(Sort.Direction.DESC, "likedAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PinLike> likePage = pinLikeRepository.findByUserId(userId, pageable);

        List<PinResponseDTO> pins = likePage.getContent().stream()
                .map(like -> {
                    Pin pin = pinRepository.findById(like.getPinId()).orElse(null);
                    if (pin == null) return null;

                    User user = userRepository.findById(pin.getUserId()).orElse(null);
                    Board board = boardRepository.findById(pin.getBoardId()).orElse(null);

                    PinResponseDTO response = modelMapper.map(pin, PinResponseDTO.class);
                    response.setIsLiked(true);
                    response.setIsSaved(savedPinRepository.existsByPinIdAndUserId(pin.getPinId(), userId));

                    if (user != null) {
                        UserSummaryDTO userSummary = new UserSummaryDTO();
                        userSummary.setUserId(user.getUserId());
                        userSummary.setUsername(user.getUsername());
                        userSummary.setProfilePictureUrl(user.getProfilePictureUrl());
                        response.setCreatedBy(userSummary);
                    }

                    if (board != null) {
                        BoardSummaryDTO boardSummary = new BoardSummaryDTO();
                        boardSummary.setBoardId(board.getBoardId());
                        boardSummary.setBoardName(board.getName());
                        response.setBoard(boardSummary);
                    }

                    return response;
                })
                .filter(pin -> pin != null)
                .collect(Collectors.toList());

        PaginationDTO pagination = new PaginationDTO(
                likePage.getNumber(),
                likePage.getTotalPages(),
                likePage.getTotalElements(),
                likePage.getSize(),
                likePage.hasNext(),
                likePage.hasPrevious()
        );

        return new PaginatedResponse<>(pins, pagination);
    }

    // ==================== SAVE OPERATIONS ====================

    @Transactional
    public SavedPinResponseDTO savePin(String userId, String pinId, String boardId) {
        log.info("User {} saving pin {} to board {}", userId, pinId, boardId);

        // Board ID is required for saving pins
        if (boardId == null || boardId.isEmpty()) {
            throw new IllegalArgumentException("Board ID is required to save a pin");
        }

        // Verify original pin exists
        Pin originalPin = pinRepository.findById(pinId)
                .orElseThrow(() -> new PinNotFoundException("Pin not found with ID: " + pinId));

        // Verify board exists and user owns it
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with ID: " + boardId));
        
        if (!board.getUserId().equals(userId)) {
            throw new IllegalStateException("You can only save pins to your own boards");
        }

        // Check if already saved to this specific board
        if (savedPinRepository.existsByPinIdAndUserIdAndBoardId(pinId, userId, boardId)) {
            log.warn("User {} already saved pin {} to board {}", userId, pinId, boardId);
            throw new IllegalStateException("Pin already saved to this board");
        }

        // Create a NEW pin (copy) in the user's board
        Pin newPin = new Pin();
        newPin.setUserId(userId);
        newPin.setBoardId(boardId);
        newPin.setTitle(originalPin.getTitle());
        newPin.setDescription(originalPin.getDescription());
        newPin.setImageUrl(originalPin.getImageUrl());
        newPin.setSourceUrl(originalPin.getSourceUrl());
        newPin.setVisibility(originalPin.getVisibility());
        newPin.setIsDraft(false);
        newPin.setIsSponsored(false);
        newPin.setSaveCount(0);
        newPin.setLikeCount(0);
        
        Pin savedNewPin = pinRepository.save(newPin);

        // Create save record linking original pin to the new pin
        SavedPin savedPin = new SavedPin();
        savedPin.setPinId(pinId); // Original pin ID for reference
        savedPin.setCopiedPinId(savedNewPin.getPinId()); // New copied pin ID
        savedPin.setUserId(userId);
        savedPin.setBoardId(boardId);
        SavedPin saved = savedPinRepository.save(savedPin);

        // Update save count on ORIGINAL pin
        originalPin.setSaveCount(originalPin.getSaveCount() + 1);
        pinRepository.save(originalPin);

        // Create notification for original pin owner (if not saving own pin)
        if (!userId.equals(originalPin.getUserId())) {
            try {
                notificationService.createNotification(
                        originalPin.getUserId(),
                        userId,
                        Notification.NotificationType.PIN_SAVED,
                        "saved your pin",
                        pinId,
                        "pin"
                );
            } catch (Exception e) {
                log.error("Failed to create notification for pin save", e);
            }
        }

        log.info("Pin {} saved successfully to board {} as new pin {}", pinId, boardId, savedNewPin.getPinId());

        SavedPinResponseDTO response = modelMapper.map(saved, SavedPinResponseDTO.class);
        response.setIsSaved(true);
        return response;
    }

    @Transactional
    public void unsavePin(String userId, String pinId) {
        log.info("User {} unsaving pin {}", userId, pinId);

        // Verify original pin exists
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new PinNotFoundException("Pin not found with ID: " + pinId));

        // Find and delete save record
        SavedPin savedPin = savedPinRepository.findByPinIdAndUserId(pinId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved pin not found"));

        // Delete the copied pin if it exists
        if (savedPin.getCopiedPinId() != null) {
            pinRepository.findById(savedPin.getCopiedPinId()).ifPresent(copiedPin -> {
                pinRepository.delete(copiedPin);
                log.info("Deleted copied pin {}", savedPin.getCopiedPinId());
            });
        }

        savedPinRepository.delete(savedPin);

        // Update save count on original pin
        pin.setSaveCount(Math.max(0, pin.getSaveCount() - 1));
        pinRepository.save(pin);

        log.info("Pin {} unsaved successfully by user {}", pinId, userId);
    }

    public Boolean isSaved(String userId, String pinId) {
        return savedPinRepository.existsByPinIdAndUserId(pinId, userId);
    }

    public PaginatedResponse<PinResponseDTO> getSavedPins(String userId, String boardId, int page, int size) {
        log.info("Fetching saved pins for user: {}", userId);

        Sort sort = Sort.by(Sort.Direction.DESC, "savedAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SavedPin> savePage;
        if (boardId != null && !boardId.isEmpty()) {
            savePage = savedPinRepository.findByUserIdAndBoardId(userId, boardId, pageable);
        } else {
            savePage = savedPinRepository.findByUserId(userId, pageable);
        }

        List<PinResponseDTO> pins = savePage.getContent().stream()
                .map(save -> {
                    Pin pin = pinRepository.findById(save.getPinId()).orElse(null);
                    if (pin == null) return null;

                    User user = userRepository.findById(pin.getUserId()).orElse(null);
                    Board board = boardRepository.findById(pin.getBoardId()).orElse(null);

                    PinResponseDTO response = modelMapper.map(pin, PinResponseDTO.class);
                    response.setIsSaved(true);
                    response.setIsLiked(pinLikeRepository.existsByPinIdAndUserId(pin.getPinId(), userId));

                    if (user != null) {
                        UserSummaryDTO userSummary = new UserSummaryDTO();
                        userSummary.setUserId(user.getUserId());
                        userSummary.setUsername(user.getUsername());
                        userSummary.setProfilePictureUrl(user.getProfilePictureUrl());
                        response.setCreatedBy(userSummary);
                    }

                    if (board != null) {
                        BoardSummaryDTO boardSummary = new BoardSummaryDTO();
                        boardSummary.setBoardId(board.getBoardId());
                        boardSummary.setBoardName(board.getName());
                        response.setBoard(boardSummary);
                    }

                    return response;
                })
                .filter(pin -> pin != null)
                .collect(Collectors.toList());

        PaginationDTO pagination = new PaginationDTO(
                savePage.getNumber(),
                savePage.getTotalPages(),
                savePage.getTotalElements(),
                savePage.getSize(),
                savePage.hasNext(),
                savePage.hasPrevious()
        );

        return new PaginatedResponse<>(pins, pagination);
    }
}
