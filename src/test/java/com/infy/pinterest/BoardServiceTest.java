package com.infy.pinterest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.infy.pinterest.dto.BoardCreationDTO;
import com.infy.pinterest.dto.BoardResponseDTO;
import com.infy.pinterest.dto.BoardUpdateDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PinResponseDTO;
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
import com.infy.pinterest.service.BoardService;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private PinRepository pinRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoardCollaboratorRepository collaboratorRepository;

    @Mock
    private PinLikeRepository pinLikeRepository;

    @Mock
    private SavedPinRepository savedPinRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private BoardService boardService;

    private User user;
    private Board board;
    private BoardCreationDTO boardCreationDTO;
    private BoardUpdateDTO boardUpdateDTO;
    private BoardResponseDTO boardResponseDTO;
    private Pin pin;

    @BeforeEach
    void setUp() {
        // Setup user
        user = new User();
        user.setUserId("user-123");
        user.setUsername("test_user");
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setProfilePictureUrl("https://example.com/user.jpg");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());

        // Setup board
        board = new Board();
        board.setBoardId("board-001");
        board.setUserId("user-123");
        board.setName("Test Board");
        board.setDescription("Test Description");
        board.setCategory("Travel");
        board.setVisibility(Board.Visibility.PUBLIC);
        board.setIsCollaborative(false);
        board.setPinCount(0);
        board.setCreatedAt(LocalDateTime.now());
        board.setUpdatedAt(LocalDateTime.now());

        // Setup DTOs
        boardCreationDTO = new BoardCreationDTO();
        boardCreationDTO.setName("New Board");
        boardCreationDTO.setDescription("New Description");
        boardCreationDTO.setCategory("Fashion");
        boardCreationDTO.setVisibility("PUBLIC");

        boardUpdateDTO = new BoardUpdateDTO();
        boardUpdateDTO.setName("Updated Board");
        boardUpdateDTO.setDescription("Updated Description");
        boardUpdateDTO.setVisibility("PRIVATE");

        boardResponseDTO = new BoardResponseDTO();
        boardResponseDTO.setBoardId("board-001");
        boardResponseDTO.setUserId("user-123");
        boardResponseDTO.setName("Test Board");
        boardResponseDTO.setDescription("Test Description");
        boardResponseDTO.setVisibility("PUBLIC");
        boardResponseDTO.setPinCount(0);

        // Setup pin
        pin = new Pin();
        pin.setPinId("pin-001");
        pin.setUserId("user-123");
        pin.setBoardId("board-001");
        pin.setTitle("Test Pin");
        pin.setDescription("Pin Description");
        pin.setImageUrl("https://example.com/pin.jpg");
        pin.setVisibility(Pin.Visibility.PUBLIC);
        pin.setIsDraft(false);
        pin.setCreatedAt(LocalDateTime.now());
    }

    // ==================== CREATE BOARD TESTS ====================

    @Test
    void testCreateBoard_Success() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        BoardResponseDTO result = boardService.createBoard("user-123", boardCreationDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals("board-001", result.getBoardId());
        assertEquals("Test Board", result.getName());
        verify(userRepository).findById("user-123");
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    void testCreateBoard_UserNotFound() {
        // Arrange
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            boardService.createBoard("non-existent", boardCreationDTO, null);
        });

        assertEquals("User not found with ID: non-existent", exception.getMessage());
        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void testCreateBoard_SetsDefaultValues() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> {
            Board savedBoard = invocation.getArgument(0);
            assertEquals("user-123", savedBoard.getUserId());
            assertEquals("New Board", savedBoard.getName());
            assertEquals("New Description", savedBoard.getDescription());
            assertEquals("Fashion", savedBoard.getCategory());
            assertEquals(Board.Visibility.PUBLIC, savedBoard.getVisibility());
            assertFalse(savedBoard.getIsCollaborative());
            assertEquals(0, savedBoard.getPinCount());
            return savedBoard;
        });
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        boardService.createBoard("user-123", boardCreationDTO, null);

        // Assert
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    void testCreateBoard_PrivateVisibility() {
        // Arrange
        boardCreationDTO.setVisibility("PRIVATE");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        BoardResponseDTO result = boardService.createBoard("user-123", boardCreationDTO, null);

        // Assert
        assertNotNull(result);
        verify(boardRepository).save(any(Board.class));
    }

    // ==================== UPDATE BOARD TESTS ====================

    @Test
    void testUpdateBoard_Success() {
        // Arrange
        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        BoardResponseDTO result = boardService.updateBoard("user-123", "board-001", boardUpdateDTO);

        // Assert
        assertNotNull(result);
        verify(boardRepository).findByBoardIdAndUserId("board-001", "user-123");
        verify(boardRepository).save(board);
    }

    @Test
    void testUpdateBoard_BoardNotFound() {
        // Arrange
        when(boardRepository.findByBoardIdAndUserId("non-existent", "user-123")).thenReturn(Optional.empty());

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class, () -> {
            boardService.updateBoard("user-123", "non-existent", boardUpdateDTO);
        });

        assertEquals("Board not found or you don't have permission to edit it", exception.getMessage());
        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void testUpdateBoard_NotOwner() {
        // Arrange
        when(boardRepository.findByBoardIdAndUserId("board-001", "other-user")).thenReturn(Optional.empty());

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class, () -> {
            boardService.updateBoard("other-user", "board-001", boardUpdateDTO);
        });

        assertEquals("Board not found or you don't have permission to edit it", exception.getMessage());
    }

    @Test
    void testUpdateBoard_UpdatesAllFields() {
        // Arrange
        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        boardService.updateBoard("user-123", "board-001", boardUpdateDTO);

        // Assert
        assertEquals("Updated Board", board.getName());
        assertEquals("Updated Description", board.getDescription());
        assertEquals(Board.Visibility.PRIVATE, board.getVisibility());
    }

    @Test
    void testUpdateBoard_PartialUpdate_OnlyName() {
        // Arrange
        BoardUpdateDTO partialUpdate = new BoardUpdateDTO();
        partialUpdate.setName("Only Name Updated");
        String originalDescription = board.getDescription();
        Board.Visibility originalVisibility = board.getVisibility();

        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        boardService.updateBoard("user-123", "board-001", partialUpdate);

        // Assert
        assertEquals("Only Name Updated", board.getName());
        assertEquals(originalDescription, board.getDescription());
        assertEquals(originalVisibility, board.getVisibility());
    }

    @Test
    void testUpdateBoard_PartialUpdate_OnlyDescription() {
        // Arrange
        BoardUpdateDTO partialUpdate = new BoardUpdateDTO();
        partialUpdate.setDescription("Only Description Updated");
        String originalName = board.getName();

        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        boardService.updateBoard("user-123", "board-001", partialUpdate);

        // Assert
        assertEquals(originalName, board.getName());
        assertEquals("Only Description Updated", board.getDescription());
    }

    @Test
    void testUpdateBoard_PartialUpdate_OnlyVisibility() {
        // Arrange
        BoardUpdateDTO partialUpdate = new BoardUpdateDTO();
        partialUpdate.setVisibility("PRIVATE");
        String originalName = board.getName();

        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        boardService.updateBoard("user-123", "board-001", partialUpdate);

        // Assert
        assertEquals(originalName, board.getName());
        assertEquals(Board.Visibility.PRIVATE, board.getVisibility());
    }

    @Test
    void testUpdateBoard_EmptyNameIgnored() {
        // Arrange
        BoardUpdateDTO partialUpdate = new BoardUpdateDTO();
        partialUpdate.setName("");
        String originalName = board.getName();

        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        boardService.updateBoard("user-123", "board-001", partialUpdate);

        // Assert
        assertEquals(originalName, board.getName()); // Name should not change
    }

    // ==================== DELETE BOARD TESTS ====================

    @Test
    void testDeleteBoard_Success() {
        // Arrange
        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));
        doNothing().when(boardRepository).delete(board);

        // Act
        boardService.deleteBoard("user-123", "board-001");

        // Assert
        verify(boardRepository).findByBoardIdAndUserId("board-001", "user-123");
        verify(boardRepository).delete(board);
    }

    @Test
    void testDeleteBoard_BoardNotFound() {
        // Arrange
        when(boardRepository.findByBoardIdAndUserId("non-existent", "user-123")).thenReturn(Optional.empty());

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class, () -> {
            boardService.deleteBoard("user-123", "non-existent");
        });

        assertEquals("Board not found or you don't have permission to delete it", exception.getMessage());
        verify(boardRepository, never()).delete(any(Board.class));
    }

    @Test
    void testDeleteBoard_NotOwner() {
        // Arrange
        when(boardRepository.findByBoardIdAndUserId("board-001", "other-user")).thenReturn(Optional.empty());

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class, () -> {
            boardService.deleteBoard("other-user", "board-001");
        });

        assertEquals("Board not found or you don't have permission to delete it", exception.getMessage());
    }

    // ==================== GET BOARD BY ID TESTS ====================

    @Test
    void testGetBoardById_Success() {
        // Arrange
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        BoardResponseDTO result = boardService.getBoardById("board-001");

        // Assert
        assertNotNull(result);
        assertEquals("board-001", result.getBoardId());
        verify(boardRepository).findById("board-001");
    }

    @Test
    void testGetBoardById_NotFound() {
        // Arrange
        when(boardRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class, () -> {
            boardService.getBoardById("non-existent");
        });

        assertEquals("Board not found with ID: non-existent", exception.getMessage());
    }

    // ==================== GET USER BOARDS TESTS ====================

    @Test
    void testGetUserBoards_Success() {
        // Arrange
        List<Board> boards = Arrays.asList(board);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 10), 1);

        when(boardRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId("board-001")).thenReturn(5L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getUserBoards("user-123", 0, 10, "createdAt");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(5, result.getData().get(0).getPinCount());
        assertEquals(0, result.getPagination().getCurrentPage());
    }

    @Test
    void testGetUserBoards_EmptyList() {
        // Arrange
        Page<Board> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(boardRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getUserBoards("user-123", 0, 10, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetUserBoards_WithPagination() {
        // Arrange
        List<Board> boards = Arrays.asList(board);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(1, 5), 20);

        when(boardRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId("board-001")).thenReturn(5L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getUserBoards("user-123", 1, 5, "createdAt");

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(4, result.getPagination().getTotalPages());
        assertEquals(20L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetUserBoards_DefaultSortByCreatedAt() {
        // Arrange
        List<Board> boards = Arrays.asList(board);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 10), 1);

        when(boardRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId("board-001")).thenReturn(0L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        boardService.getUserBoards("user-123", 0, 10, null);

        // Assert
        verify(boardRepository).findByUserId(eq("user-123"), any(Pageable.class));
    }

    @Test
    void testGetUserBoards_NullPinCount() {
        // Arrange
        List<Board> boards = Arrays.asList(board);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 10), 1);

        when(boardRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId("board-001")).thenReturn(null);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getUserBoards("user-123", 0, 10, "createdAt");

        // Assert
        assertEquals(0, result.getData().get(0).getPinCount());
    }

    // ==================== GET COLLABORATIVE BOARDS TESTS ====================

    @Test
    void testGetCollaborativeBoards_Success() {
        // Arrange
        BoardCollaborator collaborator = new BoardCollaborator();
        collaborator.setCollaboratorId("collab-001");
        collaborator.setBoardId("board-001");
        collaborator.setUserId("user-123");

        when(collaboratorRepository.findByUserId("user-123")).thenReturn(Arrays.asList(collaborator));
        when(boardRepository.findAllById(any(List.class))).thenReturn(Arrays.asList(board));
        when(pinRepository.countByBoardId("board-001")).thenReturn(3L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getCollaborativeBoards("user-123", 0, 10, "createdAt");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(3, result.getData().get(0).getPinCount());
    }

    @Test
    void testGetCollaborativeBoards_NoCollaborations() {
        // Arrange
        when(collaboratorRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getCollaborativeBoards("user-123", 0, 10, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPagination().getTotalItems());
    }

    @Test
    void testGetCollaborativeBoards_WithPagination() {
        // Arrange
        List<BoardCollaborator> collaborators = new ArrayList<>();
        List<Board> boards = new ArrayList<>();
        
        for (int i = 0; i < 15; i++) {
            BoardCollaborator collab = new BoardCollaborator();
            collab.setCollaboratorId("collab-" + i);
            collab.setBoardId("board-" + i);
            collab.setUserId("user-123");
            collaborators.add(collab);

            Board b = new Board();
            b.setBoardId("board-" + i);
            b.setName("Board " + i);
            b.setUserId("owner-" + i);
            b.setCreatedAt(LocalDateTime.now().minusDays(i));
            boards.add(b);
        }

        when(collaboratorRepository.findByUserId("user-123")).thenReturn(collaborators);
        when(boardRepository.findAllById(any(List.class))).thenReturn(boards);
        when(pinRepository.countByBoardId(anyString())).thenReturn(0L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getCollaborativeBoards("user-123", 1, 10, "createdAt");

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(5, result.getData().size()); // Second page has 5 items
        assertEquals(2, result.getPagination().getTotalPages());
    }

    @Test
    void testGetCollaborativeBoards_SortByName() {
        // Arrange
        BoardCollaborator collaborator = new BoardCollaborator();
        collaborator.setBoardId("board-001");
        collaborator.setUserId("user-123");

        when(collaboratorRepository.findByUserId("user-123")).thenReturn(Arrays.asList(collaborator));
        when(boardRepository.findAllById(any(List.class))).thenReturn(Arrays.asList(board));
        when(pinRepository.countByBoardId("board-001")).thenReturn(0L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getCollaborativeBoards("user-123", 0, 10, "name");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
    }

    // ==================== GET ALL PUBLIC BOARDS TESTS ====================

    @Test
    void testGetAllPublicBoards_Success() {
        // Arrange
        List<Board> boards = Arrays.asList(board);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 10), 1);

        when(boardRepository.findByVisibility(eq(Board.Visibility.PUBLIC), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId("board-001")).thenReturn(8L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getAllPublicBoards(0, 10, "createdAt");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(8, result.getData().get(0).getPinCount());
    }

    @Test
    void testGetAllPublicBoards_EmptyList() {
        // Arrange
        Page<Board> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(boardRepository.findByVisibility(eq(Board.Visibility.PUBLIC), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getAllPublicBoards(0, 10, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetAllPublicBoards_WithPagination() {
        // Arrange
        List<Board> boards = Arrays.asList(board);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(2, 10), 50);

        when(boardRepository.findByVisibility(eq(Board.Visibility.PUBLIC), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId("board-001")).thenReturn(0L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getAllPublicBoards(2, 10, "createdAt");

        // Assert
        assertEquals(2, result.getPagination().getCurrentPage());
        assertEquals(5, result.getPagination().getTotalPages());
        assertEquals(50L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetAllPublicBoards_DefaultSort() {
        // Arrange
        List<Board> boards = Arrays.asList(board);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 10), 1);

        when(boardRepository.findByVisibility(eq(Board.Visibility.PUBLIC), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId("board-001")).thenReturn(0L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        boardService.getAllPublicBoards(0, 10, null);

        // Assert
        verify(boardRepository).findByVisibility(eq(Board.Visibility.PUBLIC), any(Pageable.class));
    }

    // ==================== GET BOARD PINS TESTS ====================

    @Test
    void testGetBoardPins_Success() {
        // Arrange
        List<Pin> pins = Arrays.asList(pin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 1);

        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(pinRepository.findByBoardId(eq("board-001"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        pinResponse.setPinId("pin-001");
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getBoardPins("board-001", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("pin-001", result.getData().get(0).getPinId());
    }

    @Test
    void testGetBoardPins_BoardNotFound() {
        // Arrange
        when(boardRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class, () -> {
            boardService.getBoardPins("non-existent", 0, 10);
        });

        assertEquals("Board not found with ID: non-existent", exception.getMessage());
    }

    @Test
    void testGetBoardPins_EmptyList() {
        // Arrange
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(pinRepository.findByBoardId(eq("board-001"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getBoardPins("board-001", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetBoardPins_WithPagination() {
        // Arrange
        List<Pin> pins = Arrays.asList(pin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(1, 5), 15);

        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(pinRepository.findByBoardId(eq("board-001"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getBoardPins("board-001", 1, 5);

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(3, result.getPagination().getTotalPages());
        assertEquals(15L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetBoardPins_WithUserAndBoardDetails() {
        // Arrange
        List<Pin> pins = Arrays.asList(pin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 1);

        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(pinRepository.findByBoardId(eq("board-001"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getBoardPins("board-001", 0, 10);

        // Assert
        assertNotNull(result.getData().get(0).getCreatedBy());
        assertNotNull(result.getData().get(0).getBoard());
    }

    @Test
    void testGetBoardPins_UserNotFound() {
        // Arrange
        List<Pin> pins = Arrays.asList(pin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 1);

        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(pinRepository.findByBoardId(eq("board-001"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getBoardPins("board-001", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
    }

    // ==================== GET USER DRAFTS TESTS ====================

    @Test
    void testGetUserDrafts_Success() {
        // Arrange
        pin.setIsDraft(true);
        List<Pin> drafts = Arrays.asList(pin);
        Page<Pin> draftPage = new PageImpl<>(drafts, PageRequest.of(0, 10), 1);

        when(pinRepository.findByUserIdAndIsDraft(eq("user-123"), eq(true), any(Pageable.class))).thenReturn(draftPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        pinResponse.setPinId("pin-001");
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getUserDrafts("user-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("pin-001", result.getData().get(0).getPinId());
    }

    @Test
    void testGetUserDrafts_EmptyList() {
        // Arrange
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(pinRepository.findByUserIdAndIsDraft(eq("user-123"), eq(true), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getUserDrafts("user-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetUserDrafts_WithPagination() {
        // Arrange
        pin.setIsDraft(true);
        List<Pin> drafts = Arrays.asList(pin);
        Page<Pin> draftPage = new PageImpl<>(drafts, PageRequest.of(2, 5), 30);

        when(pinRepository.findByUserIdAndIsDraft(eq("user-123"), eq(true), any(Pageable.class))).thenReturn(draftPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getUserDrafts("user-123", 2, 5);

        // Assert
        assertEquals(2, result.getPagination().getCurrentPage());
        assertEquals(6, result.getPagination().getTotalPages());
        assertEquals(30L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetUserDrafts_SortedByUpdatedAt() {
        // Arrange
        pin.setIsDraft(true);
        List<Pin> drafts = Arrays.asList(pin);
        Page<Pin> draftPage = new PageImpl<>(drafts, PageRequest.of(0, 10), 1);

        when(pinRepository.findByUserIdAndIsDraft(eq("user-123"), eq(true), any(Pageable.class))).thenReturn(draftPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        boardService.getUserDrafts("user-123", 0, 10);

        // Assert
        verify(pinRepository).findByUserIdAndIsDraft(eq("user-123"), eq(true), any(Pageable.class));
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    void testCreateThenUpdateBoard() {
        // Arrange - Create
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act - Create
        BoardResponseDTO created = boardService.createBoard("user-123", boardCreationDTO, null);

        // Arrange - Update
        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));

        // Act - Update
        BoardResponseDTO updated = boardService.updateBoard("user-123", "board-001", boardUpdateDTO);

        // Assert
        assertNotNull(created);
        assertNotNull(updated);
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    void testCreateThenDeleteBoard() {
        // Arrange - Create
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act - Create
        boardService.createBoard("user-123", boardCreationDTO, null);

        // Arrange - Delete
        when(boardRepository.findByBoardIdAndUserId("board-001", "user-123")).thenReturn(Optional.of(board));
        doNothing().when(boardRepository).delete(board);

        // Act - Delete
        boardService.deleteBoard("user-123", "board-001");

        // Assert
        verify(boardRepository).save(any(Board.class));
        verify(boardRepository).delete(board);
    }

    @Test
    void testGetUserBoards_MultipleBoards() {
        // Arrange
        Board board2 = new Board();
        board2.setBoardId("board-002");
        board2.setName("Second Board");
        board2.setUserId("user-123");

        List<Board> boards = Arrays.asList(board, board2);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 10), 2);

        when(boardRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId(anyString())).thenReturn(0L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getUserBoards("user-123", 0, 10, "createdAt");

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetBoardPins_MultiplePins() {
        // Arrange
        Pin pin2 = new Pin();
        pin2.setPinId("pin-002");
        pin2.setUserId("user-123");
        pin2.setBoardId("board-001");

        List<Pin> pins = Arrays.asList(pin, pin2);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 2);

        when(boardRepository.findById("board-001")).thenReturn(Optional.of(board));
        when(pinRepository.findByBoardId(eq("board-001"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = boardService.getBoardPins("board-001", 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetAllPublicBoards_OnlyPublicReturned() {
        // Arrange
        List<Board> publicBoards = Arrays.asList(board);
        Page<Board> boardPage = new PageImpl<>(publicBoards, PageRequest.of(0, 10), 1);

        when(boardRepository.findByVisibility(eq(Board.Visibility.PUBLIC), any(Pageable.class))).thenReturn(boardPage);
        when(pinRepository.countByBoardId("board-001")).thenReturn(0L);
        when(modelMapper.map(any(Board.class), eq(BoardResponseDTO.class))).thenReturn(boardResponseDTO);

        // Act
        PaginatedResponse<BoardResponseDTO> result = boardService.getAllPublicBoards(0, 10, "createdAt");

        // Assert
        assertEquals(1, result.getData().size());
        verify(boardRepository).findByVisibility(eq(Board.Visibility.PUBLIC), any(Pageable.class));
    }
}
