package com.infy.pinterest.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.infy.pinterest.dto.BoardSummaryDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PaginationDTO;
import com.infy.pinterest.dto.PinCreationDTO;
import com.infy.pinterest.dto.PinDraftDTO;
import com.infy.pinterest.dto.PinResponseDTO;
import com.infy.pinterest.dto.PinUpdateDTO;
import com.infy.pinterest.dto.UserSummaryDTO;
import com.infy.pinterest.entity.Board;
import com.infy.pinterest.entity.BoardCollaborator;
import com.infy.pinterest.entity.Invitation;
import com.infy.pinterest.entity.Pin;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.exception.BoardNotFoundException;
import com.infy.pinterest.exception.PinNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.exception.UnauthorizedAccessException;
import com.infy.pinterest.repository.BoardCollaboratorRepository;
import com.infy.pinterest.repository.BoardRepository;
import com.infy.pinterest.repository.PinLikeRepository;
import com.infy.pinterest.repository.PinRepository;
import com.infy.pinterest.repository.SavedPinRepository;
import com.infy.pinterest.repository.UserRepository;
import com.infy.pinterest.utility.FileUploadService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PinService {
    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardCollaboratorRepository collaboratorRepository;

    @Autowired
    private PinLikeRepository pinLikeRepository;

    @Autowired
    private SavedPinRepository savedPinRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Create a new pin
     */
    public PinResponseDTO createPin(String userId, PinCreationDTO pinDTO, MultipartFile image) {
        log.info("Creating pin for user: {}", userId);

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Verify board exists and belongs to user
        Board board = boardRepository.findById(pinDTO.getBoardId())
                .orElseThrow(() -> new BoardNotFoundException("Board not found with ID: " + pinDTO.getBoardId()));

        // Check if user is board owner OR collaborator with EDIT permission
        boolean isOwner = board.getUserId().equals(userId);
        boolean hasEditPermission = false;
        
        if (!isOwner) {
            BoardCollaborator collaborator = collaboratorRepository
                    .findByBoardIdAndUserId(pinDTO.getBoardId(), userId)
                    .orElse(null);
            
            if (collaborator != null && collaborator.getPermission() == Invitation.Permission.EDIT) {
                hasEditPermission = true;
            }
        }
        
        if (!isOwner && !hasEditPermission) {
            throw new UnauthorizedAccessException("You don't have permission to add pins to this board");
        }

        // Handle image: either from URL or file upload
        String imageUrl;
        if (pinDTO.getImageUrl() != null && !pinDTO.getImageUrl().trim().isEmpty()) {
            // Use provided URL directly
            imageUrl = pinDTO.getImageUrl();
            log.info("Using image URL: {}", imageUrl);
        } else if (image != null && !image.isEmpty()) {
            // Upload image file and get path
            imageUrl = fileUploadService.uploadImage(image);
            log.info("Image uploaded: {}", imageUrl);
        } else {
            throw new IllegalArgumentException("Either image file or image URL must be provided");
        }

        Pin pin = new Pin();
        pin.setUserId(userId);
        pin.setBoardId(pinDTO.getBoardId());
        pin.setTitle(pinDTO.getTitle());
        pin.setDescription(pinDTO.getDescription());
        pin.setImageUrl(imageUrl);
        pin.setSourceUrl(pinDTO.getSourceUrl());
        pin.setVisibility(Pin.Visibility.valueOf(pinDTO.getVisibility()));
        pin.setIsDraft(false);
        pin.setIsSponsored(false);

        Pin savedPin = pinRepository.save(pin);
        log.info("Pin created successfully with ID: {}", savedPin.getPinId());

        return buildPinResponse(savedPin, user, board);
    }

    /**
     * Create a pin draft
     */
    public PinResponseDTO createPinDraft(String userId, PinDraftDTO draftDTO) {
        log.info("Creating pin draft for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " +
                        userId));

        Board board = boardRepository.findById(draftDTO.getBoardId())
                .orElseThrow(() -> new BoardNotFoundException("Board not found with ID: " +
                        draftDTO.getBoardId()));

        Pin pin = new Pin();
        pin.setUserId(userId);
        pin.setBoardId(draftDTO.getBoardId());
        pin.setTitle(draftDTO.getTitle());
        pin.setDescription(draftDTO.getDescription());
        pin.setImageUrl(""); // No image for draft
        pin.setVisibility(Pin.Visibility.PRIVATE);
        pin.setIsDraft(true);

        Pin savedPin = pinRepository.save(pin);
        log.info("Pin draft created successfully with ID: {}", savedPin.getPinId());

        return buildPinResponse(savedPin, user, board);
    }/**
     * Update a pin
     */
    public PinResponseDTO updatePin(String userId, String pinId, PinUpdateDTO updateDTO) {
        log.info("Updating pin {} for user: {}", pinId, userId);

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new PinNotFoundException("Pin not found with ID: " + pinId));

        // Check if user has permission to edit this pin (owner or collaborator with EDIT)
        if (!canEditBoard(userId, pin.getBoardId())) {
            throw new UnauthorizedAccessException("You don't have permission to edit this pin");
        }

        // Update only provided fields
        if (updateDTO.getTitle() != null && !updateDTO.getTitle().isEmpty()) {
            pin.setTitle(updateDTO.getTitle());
        }
        if (updateDTO.getDescription() != null) {
            pin.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getBoardId() != null) {
            Board newBoard = boardRepository.findById(updateDTO.getBoardId())
                    .orElseThrow(() -> new BoardNotFoundException("Board not found with ID: " + updateDTO.getBoardId()));
            pin.setBoardId(newBoard.getBoardId());
        }
        if (updateDTO.getVisibility() != null) {
            pin.setVisibility(Pin.Visibility.valueOf(updateDTO.getVisibility()));
        }

        Pin updatedPin = pinRepository.save(pin);
        log.info("Pin updated successfully: {}", pinId);

        User user = userRepository.findById(userId).orElse(null);
        Board board = boardRepository.findById(updatedPin.getBoardId()).orElse(null);

        return buildPinResponse(updatedPin, user, board);
    }

    /**
     * Delete a pin
     */
    public void deletePin(String userId, String pinId) {
        log.info("Deleting pin {} for user: {}", pinId, userId);

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new PinNotFoundException("Pin not found with ID: " + pinId));

        // Check if user has permission to delete this pin (owner or collaborator with EDIT)
        if (!canEditBoard(userId, pin.getBoardId())) {
            throw new UnauthorizedAccessException("You don't have permission to delete this pin");
        }

        pinRepository.delete(pin);
        log.info("Pin deleted successfully: {}", pinId);
    }
        /**
         * Get pin by ID
         */
        public PinResponseDTO getPinById(String pinId, String requestingUserId) {
            Pin pin = pinRepository.findById(pinId)
                    .orElseThrow(() -> new PinNotFoundException("Pin not found with ID: " + pinId));

            User user = userRepository.findById(pin.getUserId()).orElse(null);
            Board board = boardRepository.findById(pin.getBoardId()).orElse(null);

            return buildPinResponse(pin, user, board, requestingUserId);
        }

        /**
         * Get user's pins with pagination
         * */
        public PaginatedResponse<PinResponseDTO> getUserPins(String userId, String requestingUserId, int page, int size,
                                                             String sortBy) {
            log.info("Fetching pins for user: {}", userId);
            Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt");
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // If viewing own profile, show all pins including drafts
            // If viewing other user's profile, show only published pins (isDraft=false)
            Page<Pin> pinPage;
            boolean isOwnProfile = userId.equals(requestingUserId);
            if (isOwnProfile) {
                pinPage = pinRepository.findByUserId(userId, pageable);
            } else {
                pinPage = pinRepository.findByUserIdAndIsDraft(userId, false, pageable);
            }
            
            List<PinResponseDTO> pins = pinPage.getContent().stream()
                    .map(pin -> {
                        User user = userRepository.findById(pin.getUserId()).orElse(null);
                        Board board = boardRepository.findById(pin.getBoardId()).orElse(null);
                        return buildPinResponse(pin, user, board, requestingUserId);
                    })
                    .collect(Collectors.toList());
            PaginationDTO pagination = new PaginationDTO(
                    pinPage.getNumber(),
                    pinPage.getTotalPages(),
                    pinPage.getTotalElements(),
                    pinPage.getSize(),
                    pinPage.hasNext(),
                    pinPage.hasPrevious()
            );
            return new PaginatedResponse<>(pins, pagination);
        }
    /**
     * Get pins by board ID with pagination
     * */
    public PaginatedResponse<PinResponseDTO> getBoardPins(String boardId, String requestingUserId, int page, int size) {
     log.info("Fetching pins for board: {}", boardId);
     // Verify board exists
     Board board = boardRepository.findById(boardId)
     .orElseThrow(() -> new BoardNotFoundException("Board not found with ID: " + boardId));
     Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
     Pageable pageable = PageRequest.of(page, size, sort);
     Page<Pin> pinPage = pinRepository.findByBoardId(boardId, pageable);
     List<PinResponseDTO> pins = pinPage.getContent().stream()
     .map(pin -> {
     User user = userRepository.findById(pin.getUserId()).orElse(null);
     return buildPinResponse(pin, user, board, requestingUserId);
     })
     .collect(Collectors.toList());
     PaginationDTO pagination = new PaginationDTO(
     pinPage.getNumber(),
     pinPage.getTotalPages(),
     pinPage.getTotalElements(),
     pinPage.getSize(),
     pinPage.hasNext(),
     pinPage.hasPrevious()
     );
     return new PaginatedResponse<>(pins, pagination);
     }
     /**
     * Get user's draft pins
     */
    public PaginatedResponse<PinResponseDTO> getUserDrafts(String userId, String requestingUserId, int page, int size) {
        log.info("Fetching draft pins for user: {}", userId);
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Pin> pinPage = pinRepository.findByUserIdAndIsDraft(userId, true, pageable);
        List<PinResponseDTO> pins = pinPage.getContent().stream()
                .map(pin -> {
                    User user = userRepository.findById(pin.getUserId()).orElse(null);
                    Board board = boardRepository.findById(pin.getBoardId()).orElse(null);
                    return buildPinResponse(pin, user, board, requestingUserId);
                })
                .collect(Collectors.toList());PaginationDTO pagination = new PaginationDTO(
                pinPage.getNumber(),
                pinPage.getTotalPages(),
                pinPage.getTotalElements(),
                pinPage.getSize(),
                pinPage.hasNext(),
                pinPage.hasPrevious()
        );
        return new PaginatedResponse<>(pins, pagination);
    }
    /**
     * Search pins by keyword
     */
    public PaginatedResponse<PinResponseDTO> searchPins(String keyword, int page, int size) {
        log.info("Searching pins with keyword: {}", keyword);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Pin> pinPage = pinRepository.searchPins(keyword, pageable);
        List<PinResponseDTO> pins = pinPage.getContent().stream()
                .map(pin -> {
                    User user = userRepository.findById(pin.getUserId()).orElse(null);
                    Board board = boardRepository.findById(pin.getBoardId()).orElse(null);
                    return buildPinResponse(pin, user, board);
                })
                .collect(Collectors.toList());
        PaginationDTO pagination = new PaginationDTO(
                pinPage.getNumber(),
                pinPage.getTotalPages(),
                pinPage.getTotalElements(),
                pinPage.getSize(),
                pinPage.hasNext(),
                pinPage.hasPrevious()
        );
        return new PaginatedResponse<>(pins, pagination);
    }
    /**
     * Get all accessible pins for the user (home feed)
     * Includes:
     * 1. All public pins from all users
     * 2. User's own pins (public and private)
     * 3. Pins from boards where user is a collaborator (with accepted invitation)
     */
    public PaginatedResponse<PinResponseDTO> getPublicPins(String requestingUserId, int page, int size) {
        log.info("Fetching accessible pins for user: {}", requestingUserId);

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Pin> pinPage;
        
        if (requestingUserId != null && !requestingUserId.isEmpty()) {
            // Get boards where user is a collaborator (accepted invitations only)
            List<BoardCollaborator> collaborations = collaboratorRepository.findByUserId(requestingUserId);
            List<String> collaborativeBoardIds = collaborations.stream()
                    .map(BoardCollaborator::getBoardId)
                    .collect(Collectors.toList());
            
            // If no collaborative boards, add empty string to avoid SQL error
            if (collaborativeBoardIds.isEmpty()) {
                collaborativeBoardIds.add("");
            }
            
            // Fetch pins: public pins + user's own pins + collaborative board pins
            pinPage = pinRepository.findAccessiblePinsForUser(requestingUserId, collaborativeBoardIds, pageable);
            log.info("Fetched {} pins accessible to user {} (including {} collaborative boards)", 
                    pinPage.getTotalElements(), requestingUserId, collaborativeBoardIds.size());
        } else {
            // If no user context, return only public pins
            pinPage = pinRepository.findAllPublicPins(pageable);
            log.info("Fetched {} public pins (no user context)", pinPage.getTotalElements());
        }

        List<PinResponseDTO> pins = pinPage.getContent().stream()
                .map(pin -> {
                    User user = userRepository.findById(pin.getUserId()).orElse(null);
                    Board board = boardRepository.findById(pin.getBoardId()).orElse(null);
                    return buildPinResponse(pin, user, board, requestingUserId);
                })
                .collect(Collectors.toList());

        PaginationDTO pagination = new PaginationDTO(
                pinPage.getNumber(),
                pinPage.getTotalPages(),
                pinPage.getTotalElements(),
                pinPage.getSize(),
                pinPage.hasNext(),
                pinPage.hasPrevious()
        );

        return new PaginatedResponse<>(pins, pagination);
    }


    /**
     * Helper method to check if user can edit pins in a board
     */
    private boolean canEditBoard(String userId, String boardId) {
        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) return false;
        
        // Check if user is board owner
        if (board.getUserId().equals(userId)) {
            return true;
        }
        
        // Check if user is collaborator with EDIT permission
        BoardCollaborator collaborator = collaboratorRepository
                .findByBoardIdAndUserId(boardId, userId)
                .orElse(null);
        
        return collaborator != null && collaborator.getPermission() == Invitation.Permission.EDIT;
    }

    /**
     * Helper method to build pin response with user and board info
     */
    private PinResponseDTO buildPinResponse(Pin pin, User user, Board board) {
        return buildPinResponse(pin, user, board, null);
    }

    /**
     * Helper method to build pin response with user, board info, and like/save status
     */
    private PinResponseDTO buildPinResponse(Pin pin, User user, Board board, String requestingUserId) {
        PinResponseDTO response = modelMapper.map(pin, PinResponseDTO.class);
        
        // Set like and save status if requesting user is provided
        if (requestingUserId != null) {
            response.setIsLiked(pinLikeRepository.existsByPinIdAndUserId(pin.getPinId(), requestingUserId));
            response.setIsSaved(savedPinRepository.existsByPinIdAndUserId(pin.getPinId(), requestingUserId));
        } else {
            response.setIsLiked(false);
            response.setIsSaved(false);
        }
        
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
    }


}
