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

import com.infy.pinterest.dto.BoardCreationDTO;
import com.infy.pinterest.dto.BoardResponseDTO;
import com.infy.pinterest.dto.BoardSummaryDTO;
import com.infy.pinterest.dto.BoardUpdateDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PaginationDTO;
import com.infy.pinterest.dto.PinResponseDTO;
import com.infy.pinterest.dto.UserSummaryDTO;
import com.infy.pinterest.entity.Board;
import com.infy.pinterest.entity.BoardCollaborator;
import com.infy.pinterest.entity.Pin;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.exception.BoardNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
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
public class BoardService {
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private PinRepository pinRepository;
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
    private ModelMapper modelMapper;    /**
     * Create a new board
     */
    public BoardResponseDTO createBoard(String userId, BoardCreationDTO boardDTO, MultipartFile bannerImage) {
        log.info("Creating board for user: {}", userId);
        
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Handle banner image: either from URL or file upload
        String coverImageUrl = null;
        if (boardDTO.getBannerImageUrl() != null && !boardDTO.getBannerImageUrl().trim().isEmpty()) {
            // Use provided URL directly
            coverImageUrl = boardDTO.getBannerImageUrl();
            log.info("Using banner image URL: {}", coverImageUrl);
        } else if (bannerImage != null && !bannerImage.isEmpty()) {
            // Upload banner image file and get path
            coverImageUrl = fileUploadService.uploadImage(bannerImage);
            log.info("Banner image uploaded: {}", coverImageUrl);
        }
        // If both are null, coverImageUrl remains null (no banner)

        Board board = new Board();
        board.setUserId(userId);
        board.setName(boardDTO.getName());
        board.setDescription(boardDTO.getDescription());
        board.setCategory(boardDTO.getCategory());
        board.setCoverImageUrl(coverImageUrl);
        board.setVisibility(Board.Visibility.valueOf(boardDTO.getVisibility()));
        board.setIsCollaborative(false);
        board.setPinCount(0);
        
        Board savedBoard = boardRepository.save(board);
        log.info("Board created successfully with ID: {}", savedBoard.getBoardId());
        
        return modelMapper.map(savedBoard, BoardResponseDTO.class);
    }
     /**     * Update an existing board     */
     public BoardResponseDTO updateBoard(String userId, String boardId, BoardUpdateDTO updateDTO){
         log.info("Updating board {} for user: {}", boardId, userId);
         Board board = boardRepository.findByBoardIdAndUserId(boardId, userId)
         .orElseThrow(() -> new BoardNotFoundException("Board not found or you don't have permission to edit it"));
         //Update only provided fields using Optional pattern
         if (updateDTO.getName() != null && !updateDTO.getName().isEmpty()) {
         board.setName(updateDTO.getName());        }
         if (updateDTO.getDescription() != null) {
         board.setDescription(updateDTO.getDescription());        }
         if (updateDTO.getVisibility() != null) {
         board.setVisibility(Board.Visibility.valueOf(updateDTO.getVisibility()));        }
         Board updatedBoard = boardRepository.save(board);
         log.info("Board updated successfully: {}", boardId);
         return modelMapper.map(updatedBoard, BoardResponseDTO.class);
     }
    /** Delete a board     **/
    public void deleteBoard(String userId, String boardId) {
         log.info("Deleting board {} for user: {}", boardId, userId);
         Board board = boardRepository.findByBoardIdAndUserId(boardId, userId)
         .orElseThrow(() -> new BoardNotFoundException("Board not found or you don't have permission to delete it"));
         boardRepository.delete(board);
         log.info("Board deleted successfully: {}", boardId);
    }
    /** Get board by ID**/
     public BoardResponseDTO getBoardById(String boardId) {
         Board board = boardRepository.findById(boardId)
         .orElseThrow(() -> new BoardNotFoundException("Board not found with ID: " +boardId));
         return modelMapper.map(board, BoardResponseDTO.class);
     }
     /** Get user's boards with pagination **/
     public PaginatedResponse<BoardResponseDTO> getUserBoards(String userId, int page, int size, String sortBy) {
         log.info("Fetching boards for user: {}", userId);
         Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt");
         Pageable pageable = PageRequest.of(page, size, sort);
         Page<Board> boardPage = boardRepository.findByUserId(userId, pageable);
         List<BoardResponseDTO> boards = boardPage.getContent().stream()
         .map(board -> {
             BoardResponseDTO dto = modelMapper.map(board, BoardResponseDTO.class);
             Long count = pinRepository.countByBoardId(board.getBoardId());
             dto.setPinCount(count != null ? count.intValue() : 0);
             return dto;
         })
         .collect(Collectors.toList());
         PaginationDTO pagination = new PaginationDTO(
                 boardPage.getNumber(),
                 boardPage.getTotalPages(),
                 boardPage.getTotalElements(),
                 boardPage.getSize(),
                 boardPage.hasNext(),
                 boardPage.hasPrevious()
         );
         return new PaginatedResponse<>(boards, pagination);
     }
     
     /** Get collaborative boards where user is a collaborator **/
     public PaginatedResponse<BoardResponseDTO> getCollaborativeBoards(String userId, int page, int size, String sortBy) {
         log.info("Fetching collaborative boards for user: {}", userId);
         
         // Get all board IDs where user is a collaborator
         List<BoardCollaborator> collaborations = collaboratorRepository.findByUserId(userId);
         
         if (collaborations.isEmpty()) {
             return new PaginatedResponse<>(List.of(), new PaginationDTO(0, 0, 0L, size, false, false));
         }
         
         List<String> boardIds = collaborations.stream()
                 .map(BoardCollaborator::getBoardId)
                 .collect(Collectors.toList());
         
         // Fetch boards by IDs
         List<Board> boards = boardRepository.findAllById(boardIds);
         
         // Sort and paginate manually
         Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt");
         boards = boards.stream()
                 .sorted((b1, b2) -> {
                     if ("createdAt".equals(sortBy) || sortBy == null) {
                         return b2.getCreatedAt().compareTo(b1.getCreatedAt());
                     } else if ("name".equals(sortBy)) {
                         return b1.getName().compareTo(b2.getName());
                     }
                     return 0;
                 })
                 .collect(Collectors.toList());
         
         int start = page * size;
         int end = Math.min(start + size, boards.size());
         List<Board> paginatedBoards = start < boards.size() ? boards.subList(start, end) : List.of();
         
         List<BoardResponseDTO> boardDTOs = paginatedBoards.stream()
                 .map(board -> {
                     BoardResponseDTO dto = modelMapper.map(board, BoardResponseDTO.class);
                     Long count = pinRepository.countByBoardId(board.getBoardId());
                     dto.setPinCount(count != null ? count.intValue() : 0);
                     return dto;
                 })
                 .collect(Collectors.toList());
         
         int totalPages = (int) Math.ceil((double) boards.size() / size);
         PaginationDTO pagination = new PaginationDTO(
                 page,
                 totalPages,
                 (long) boards.size(),
                 size,
                 page < totalPages - 1,
                 page > 0
         );
         
         return new PaginatedResponse<>(boardDTOs, pagination);
     }
     
     /** Get all public boards for home page with pagination **/
     public PaginatedResponse<BoardResponseDTO> getAllPublicBoards(int page, int size, String sortBy) {
         log.info("Fetching all public boards");
         Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt");
         Pageable pageable = PageRequest.of(page, size, sort);
         Page<Board> boardPage = boardRepository.findByVisibility(Board.Visibility.PUBLIC, pageable);
         List<BoardResponseDTO> boards = boardPage.getContent().stream()
         .map(board -> {
             BoardResponseDTO dto = modelMapper.map(board, BoardResponseDTO.class);
             Long count = pinRepository.countByBoardId(board.getBoardId());
             dto.setPinCount(count != null ? count.intValue() : 0);
             return dto;
         })
         .collect(Collectors.toList());
         PaginationDTO pagination = new PaginationDTO(
                 boardPage.getNumber(),
                 boardPage.getTotalPages(),
                 boardPage.getTotalElements(),
                 boardPage.getSize(),
                 boardPage.hasNext(),
                 boardPage.hasPrevious()
         );
         return new PaginatedResponse<>(boards, pagination);
     }
     
     /** Get pins by board ID with pagination     */
     public PaginatedResponse<PinResponseDTO> getBoardPins(String boardId, int page, int size) {
         log.info("Fetching pins for board: {}", boardId);        // Verify board exists
         Board board = boardRepository.findById(boardId)                .orElseThrow(() -> new BoardNotFoundException("Board not found with ID: " +boardId));
         Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
         Pageable pageable = PageRequest.of(page, size, sort);
         Page<Pin> pinPage = pinRepository.findByBoardId(boardId, pageable);
     List<PinResponseDTO> pins = pinPage.getContent().stream()
     .map(pin -> {User user = userRepository.findById(pin.getUserId()).orElse(null);
           return buildPinResponse(pin, user, board);
     })
          .collect(Collectors.toList());
          PaginationDTO pagination = new PaginationDTO(pinPage.getNumber(),
                  pinPage.getTotalPages(),
                  pinPage.getTotalElements(),
                  pinPage.getSize(),
                  pinPage.hasNext(),
                  pinPage.hasPrevious()
          );
          return new PaginatedResponse<>(pins, pagination);
     }
     /**     * Get user's draft pins     */
     public PaginatedResponse<PinResponseDTO> getUserDrafts(String userId, int page, int size) {
     log.info("Fetching draft pins for user: {}", userId);
     Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
     Pageable pageable = PageRequest.of(page, size, sort);
     Page<Pin> pinPage = pinRepository.findByUserIdAndIsDraft(userId, true, pageable);
     List<PinResponseDTO> pins = pinPage.getContent().stream()
     .map(pin -> {
     User user = userRepository.findById(pin.getUserId()).orElse(null);
     Board board = boardRepository.findById(pin.getBoardId()).orElse(null);
     return buildPinResponse(pin, user, board);
     })
     .collect(Collectors.toList());
     PaginationDTO pagination = new PaginationDTO(pinPage.getNumber(),pinPage.getTotalPages(),pinPage.getTotalElements(),pinPage.getSize(),pinPage.hasNext(),pinPage.hasPrevious());
     return new PaginatedResponse<>(pins, pagination);
     }
     /**     * Helper method to build pin response with user and board info     */
     private PinResponseDTO buildPinResponse(Pin pin, User user, Board board) {
         return buildPinResponse(pin, user, board, null);
     }

     /**     * Helper method to build pin response with user, board info, and like/save status     */
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
