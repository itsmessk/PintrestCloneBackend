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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PinLikeResponseDTO;
import com.infy.pinterest.dto.PinResponseDTO;
import com.infy.pinterest.dto.SavedPinResponseDTO;
import com.infy.pinterest.entity.Board;
import com.infy.pinterest.entity.Notification;
import com.infy.pinterest.entity.Pin;
import com.infy.pinterest.entity.PinLike;
import com.infy.pinterest.entity.SavedPin;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.exception.PinNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.repository.BoardRepository;
import com.infy.pinterest.repository.PinLikeRepository;
import com.infy.pinterest.repository.PinRepository;
import com.infy.pinterest.repository.SavedPinRepository;
import com.infy.pinterest.repository.UserRepository;
import com.infy.pinterest.service.NotificationService;
import com.infy.pinterest.service.PinInteractionService;

@ExtendWith(MockitoExtension.class)
class PinInteractionServiceTest {

    @Mock
    private PinLikeRepository pinLikeRepository;

    @Mock
    private SavedPinRepository savedPinRepository;

    @Mock
    private PinRepository pinRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PinInteractionService pinInteractionService;

    private User user;
    private User pinOwner;
    private Pin pin;
    private Board board;
    private PinLike pinLike;
    private SavedPin savedPin;

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

        // Setup pin owner
        pinOwner = new User();
        pinOwner.setUserId("owner-456");
        pinOwner.setUsername("pin_owner");
        pinOwner.setEmail("owner@example.com");
        pinOwner.setFullName("Pin Owner");
        pinOwner.setProfilePictureUrl("https://example.com/owner.jpg");
        pinOwner.setIsActive(true);
        pinOwner.setCreatedAt(LocalDateTime.now());

        // Setup board
        board = new Board();
        board.setBoardId("board-789");
        board.setUserId("user-123");
        board.setName("My Board");
        board.setDescription("Test Board");
        board.setVisibility(Board.Visibility.PUBLIC);
        board.setCreatedAt(LocalDateTime.now());

        // Setup pin
        pin = new Pin();
        pin.setPinId("pin-001");
        pin.setUserId("owner-456");
        pin.setBoardId("board-789");
        pin.setTitle("Test Pin");
        pin.setDescription("Test Description");
        pin.setImageUrl("https://example.com/image.jpg");
        pin.setSourceUrl("https://example.com");
        pin.setVisibility(Pin.Visibility.PUBLIC);
        pin.setIsDraft(false);
        pin.setIsSponsored(false);
        pin.setLikeCount(0);
        pin.setSaveCount(0);
        pin.setCreatedAt(LocalDateTime.now());

        // Setup pin like
        pinLike = new PinLike();
        pinLike.setLikeId("like-001");
        pinLike.setPinId("pin-001");
        pinLike.setUserId("user-123");
        pinLike.setLikedAt(LocalDateTime.now());

        // Setup saved pin
        savedPin = new SavedPin();
        savedPin.setSaveId("save-001");
        savedPin.setPinId("pin-001");
        savedPin.setUserId("user-123");
        savedPin.setBoardId("board-789");
        savedPin.setCopiedPinId("pin-002");
        savedPin.setSavedAt(LocalDateTime.now());
    }

    // ==================== LIKE PIN TESTS ====================

    @Test
    void testLikePin_Success() {
        // Arrange
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        when(pinLikeRepository.save(any(PinLike.class))).thenReturn(pinLike);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);
        
        PinLikeResponseDTO expectedResponse = new PinLikeResponseDTO();
        expectedResponse.setLikeId("like-001");
        expectedResponse.setIsLiked(true);
        when(modelMapper.map(any(PinLike.class), eq(PinLikeResponseDTO.class))).thenReturn(expectedResponse);

        // Act
        PinLikeResponseDTO result = pinInteractionService.likePin("user-123", "pin-001");

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsLiked());
        assertEquals("like-001", result.getLikeId());
        verify(pinLikeRepository).save(any(PinLike.class));
        verify(pinRepository).save(pin);
        verify(notificationService).createNotification(
            eq("owner-456"), eq("user-123"), 
            eq(Notification.NotificationType.PIN_LIKED),
            anyString(), eq("pin-001"), eq("pin")
        );
    }

    @Test
    void testLikePin_PinNotFound() {
        // Arrange
        when(pinRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        PinNotFoundException exception = assertThrows(PinNotFoundException.class, () -> {
            pinInteractionService.likePin("user-123", "non-existent");
        });

        assertEquals("Pin not found with ID: non-existent", exception.getMessage());
        verify(pinLikeRepository, never()).save(any(PinLike.class));
    }

    @Test
    void testLikePin_AlreadyLiked() {
        // Arrange
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            pinInteractionService.likePin("user-123", "pin-001");
        });

        assertEquals("Pin already liked", exception.getMessage());
        verify(pinLikeRepository, never()).save(any(PinLike.class));
    }

    @Test
    void testLikePin_IncreasesLikeCount() {
        // Arrange
        pin.setLikeCount(5);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        when(pinLikeRepository.save(any(PinLike.class))).thenReturn(pinLike);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);
        
        PinLikeResponseDTO expectedResponse = new PinLikeResponseDTO();
        when(modelMapper.map(any(PinLike.class), eq(PinLikeResponseDTO.class))).thenReturn(expectedResponse);

        // Act
        pinInteractionService.likePin("user-123", "pin-001");

        // Assert
        assertEquals(6, pin.getLikeCount());
        verify(pinRepository).save(pin);
    }

    @Test
    void testLikePin_OwnPin_NoNotification() {
        // Arrange
        pin.setUserId("user-123"); // User is liking their own pin
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        when(pinLikeRepository.save(any(PinLike.class))).thenReturn(pinLike);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);
        
        PinLikeResponseDTO expectedResponse = new PinLikeResponseDTO();
        when(modelMapper.map(any(PinLike.class), eq(PinLikeResponseDTO.class))).thenReturn(expectedResponse);

        // Act
        pinInteractionService.likePin("user-123", "pin-001");

        // Assert
        verify(notificationService, never()).createNotification(
            anyString(), anyString(), any(), anyString(), anyString(), anyString()
        );
    }

    @Test
    void testLikePin_NotificationFailure_DoesNotAffectLike() {
        // Arrange
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        when(pinLikeRepository.save(any(PinLike.class))).thenReturn(pinLike);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);
        
        PinLikeResponseDTO expectedResponse = new PinLikeResponseDTO();
        when(modelMapper.map(any(PinLike.class), eq(PinLikeResponseDTO.class))).thenReturn(expectedResponse);
        
        doNothing().when(notificationService).createNotification(
            anyString(), anyString(), any(), anyString(), anyString(), anyString()
        );

        // Act
        PinLikeResponseDTO result = pinInteractionService.likePin("user-123", "pin-001");

        // Assert
        assertNotNull(result);
    }

    // ==================== UNLIKE PIN TESTS ====================

    @Test
    void testUnlikePin_Success() {
        // Arrange
        pin.setLikeCount(5);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(pinLike));
        doNothing().when(pinLikeRepository).delete(pinLike);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        pinInteractionService.unlikePin("user-123", "pin-001");

        // Assert
        verify(pinLikeRepository).delete(pinLike);
        verify(pinRepository).save(pin);
        assertEquals(4, pin.getLikeCount());
    }

    @Test
    void testUnlikePin_PinNotFound() {
        // Arrange
        when(pinRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        PinNotFoundException exception = assertThrows(PinNotFoundException.class, () -> {
            pinInteractionService.unlikePin("user-123", "non-existent");
        });

        assertEquals("Pin not found with ID: non-existent", exception.getMessage());
        verify(pinLikeRepository, never()).delete(any(PinLike.class));
    }

    @Test
    void testUnlikePin_LikeNotFound() {
        // Arrange
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            pinInteractionService.unlikePin("user-123", "pin-001");
        });

        assertEquals("Like not found", exception.getMessage());
        verify(pinLikeRepository, never()).delete(any(PinLike.class));
    }

    @Test
    void testUnlikePin_DecreasesLikeCount() {
        // Arrange
        pin.setLikeCount(10);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(pinLike));
        doNothing().when(pinLikeRepository).delete(pinLike);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        pinInteractionService.unlikePin("user-123", "pin-001");

        // Assert
        assertEquals(9, pin.getLikeCount());
    }

    @Test
    void testUnlikePin_LikeCountNeverNegative() {
        // Arrange
        pin.setLikeCount(0); // Already at zero
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(pinLike));
        doNothing().when(pinLikeRepository).delete(pinLike);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        pinInteractionService.unlikePin("user-123", "pin-001");

        // Assert
        assertEquals(0, pin.getLikeCount());
    }

    // ==================== IS LIKED TESTS ====================

    @Test
    void testIsLiked_True() {
        // Arrange
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(true);

        // Act
        Boolean result = pinInteractionService.isLiked("user-123", "pin-001");

        // Assert
        assertTrue(result);
        verify(pinLikeRepository).existsByPinIdAndUserId("pin-001", "user-123");
    }

    @Test
    void testIsLiked_False() {
        // Arrange
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);

        // Act
        Boolean result = pinInteractionService.isLiked("user-123", "pin-001");

        // Assert
        assertFalse(result);
    }

    // ==================== GET LIKED PINS TESTS ====================

    @Test
    void testGetLikedPins_Success() {
        // Arrange
        List<PinLike> likes = Arrays.asList(pinLike);
        Page<PinLike> likePage = new PageImpl<>(likes, PageRequest.of(0, 10), 1);

        when(pinLikeRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(likePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        pinResponse.setPinId("pin-001");
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getLikedPins("user-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).getIsLiked());
        assertEquals(0, result.getPagination().getCurrentPage());
    }

    @Test
    void testGetLikedPins_EmptyList() {
        // Arrange
        Page<PinLike> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(pinLikeRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getLikedPins("user-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetLikedPins_WithPagination() {
        // Arrange
        List<PinLike> likes = Arrays.asList(pinLike);
        Page<PinLike> likePage = new PageImpl<>(likes, PageRequest.of(1, 5), 20);

        when(pinLikeRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(likePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getLikedPins("user-123", 1, 5);

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(4, result.getPagination().getTotalPages());
        assertEquals(20L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetLikedPins_PinDeleted_FilteredOut() {
        // Arrange
        List<PinLike> likes = Arrays.asList(pinLike);
        Page<PinLike> likePage = new PageImpl<>(likes, PageRequest.of(0, 10), 1);

        when(pinLikeRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(likePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.empty()); // Pin deleted

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getLikedPins("user-123", 0, 10);

        // Assert
        assertTrue(result.getData().isEmpty()); // Filtered out
    }

    @Test
    void testGetLikedPins_WithUserAndBoardDetails() {
        // Arrange
        List<PinLike> likes = Arrays.asList(pinLike);
        Page<PinLike> likePage = new PageImpl<>(likes, PageRequest.of(0, 10), 1);

        when(pinLikeRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(likePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(true);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getLikedPins("user-123", 0, 10);

        // Assert
        assertNotNull(result.getData().get(0).getCreatedBy());
        assertNotNull(result.getData().get(0).getBoard());
        assertTrue(result.getData().get(0).getIsSaved());
    }

    // ==================== SAVE PIN TESTS ====================

    @Test
    void testSavePin_Success() {
        // Arrange
        Pin newPin = new Pin();
        newPin.setPinId("pin-002");
        newPin.setUserId("user-123");
        newPin.setBoardId("board-789");

        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserIdAndBoardId("pin-001", "user-123", "board-789")).thenReturn(false);
        when(pinRepository.save(any(Pin.class))).thenReturn(newPin).thenReturn(pin);
        when(savedPinRepository.save(any(SavedPin.class))).thenReturn(savedPin);
        
        SavedPinResponseDTO expectedResponse = new SavedPinResponseDTO();
        expectedResponse.setSaveId("save-001");
        expectedResponse.setIsSaved(true);
        when(modelMapper.map(any(SavedPin.class), eq(SavedPinResponseDTO.class))).thenReturn(expectedResponse);

        // Act
        SavedPinResponseDTO result = pinInteractionService.savePin("user-123", "pin-001", "board-789");

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsSaved());
        assertEquals("save-001", result.getSaveId());
        verify(pinRepository, times(2)).save(any(Pin.class)); // New pin + update original
        verify(savedPinRepository).save(any(SavedPin.class));
        verify(notificationService).createNotification(
            eq("owner-456"), eq("user-123"), 
            eq(Notification.NotificationType.PIN_SAVED),
            anyString(), eq("pin-001"), eq("pin")
        );
    }

    @Test
    void testSavePin_NullBoardId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pinInteractionService.savePin("user-123", "pin-001", null);
        });

        assertEquals("Board ID is required to save a pin", exception.getMessage());
        verify(savedPinRepository, never()).save(any(SavedPin.class));
    }

    @Test
    void testSavePin_EmptyBoardId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pinInteractionService.savePin("user-123", "pin-001", "");
        });

        assertEquals("Board ID is required to save a pin", exception.getMessage());
    }

    @Test
    void testSavePin_PinNotFound() {
        // Arrange
        when(pinRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        PinNotFoundException exception = assertThrows(PinNotFoundException.class, () -> {
            pinInteractionService.savePin("user-123", "non-existent", "board-789");
        });

        assertEquals("Pin not found with ID: non-existent", exception.getMessage());
        verify(savedPinRepository, never()).save(any(SavedPin.class));
    }

    @Test
    void testSavePin_BoardNotFound() {
        // Arrange
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(boardRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            pinInteractionService.savePin("user-123", "pin-001", "non-existent");
        });

        assertEquals("Board not found with ID: non-existent", exception.getMessage());
    }

    @Test
    void testSavePin_NotOwnBoard() {
        // Arrange
        board.setUserId("other-user"); // Board belongs to someone else
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            pinInteractionService.savePin("user-123", "pin-001", "board-789");
        });

        assertEquals("You can only save pins to your own boards", exception.getMessage());
    }

    @Test
    void testSavePin_AlreadySavedToBoard() {
        // Arrange
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserIdAndBoardId("pin-001", "user-123", "board-789")).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            pinInteractionService.savePin("user-123", "pin-001", "board-789");
        });

        assertEquals("Pin already saved to this board", exception.getMessage());
        verify(savedPinRepository, never()).save(any(SavedPin.class));
    }

    @Test
    void testSavePin_IncreasesSaveCount() {
        // Arrange
        pin.setSaveCount(5);
        Pin newPin = new Pin();
        newPin.setPinId("pin-002");

        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserIdAndBoardId("pin-001", "user-123", "board-789")).thenReturn(false);
        when(pinRepository.save(any(Pin.class))).thenReturn(newPin).thenReturn(pin);
        when(savedPinRepository.save(any(SavedPin.class))).thenReturn(savedPin);
        
        SavedPinResponseDTO expectedResponse = new SavedPinResponseDTO();
        when(modelMapper.map(any(SavedPin.class), eq(SavedPinResponseDTO.class))).thenReturn(expectedResponse);

        // Act
        pinInteractionService.savePin("user-123", "pin-001", "board-789");

        // Assert
        assertEquals(6, pin.getSaveCount());
    }

    @Test
    void testSavePin_CreatesNewPin() {
        // Arrange
        Pin newPin = new Pin();
        newPin.setPinId("pin-002");

        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserIdAndBoardId("pin-001", "user-123", "board-789")).thenReturn(false);
        when(pinRepository.save(any(Pin.class))).thenReturn(newPin).thenReturn(pin);
        when(savedPinRepository.save(any(SavedPin.class))).thenReturn(savedPin);
        
        SavedPinResponseDTO expectedResponse = new SavedPinResponseDTO();
        when(modelMapper.map(any(SavedPin.class), eq(SavedPinResponseDTO.class))).thenReturn(expectedResponse);

        // Act
        pinInteractionService.savePin("user-123", "pin-001", "board-789");

        // Assert
        verify(pinRepository, times(2)).save(any(Pin.class)); // New pin created + original updated
    }

    @Test
    void testSavePin_OwnPin_NoNotification() {
        // Arrange
        pin.setUserId("user-123"); // Saving own pin
        Pin newPin = new Pin();
        newPin.setPinId("pin-002");

        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserIdAndBoardId("pin-001", "user-123", "board-789")).thenReturn(false);
        when(pinRepository.save(any(Pin.class))).thenReturn(newPin).thenReturn(pin);
        when(savedPinRepository.save(any(SavedPin.class))).thenReturn(savedPin);
        
        SavedPinResponseDTO expectedResponse = new SavedPinResponseDTO();
        when(modelMapper.map(any(SavedPin.class), eq(SavedPinResponseDTO.class))).thenReturn(expectedResponse);

        // Act
        pinInteractionService.savePin("user-123", "pin-001", "board-789");

        // Assert
        verify(notificationService, never()).createNotification(
            anyString(), anyString(), any(), anyString(), anyString(), anyString()
        );
    }

    // ==================== UNSAVE PIN TESTS ====================

    @Test
    void testUnsavePin_Success() {
        // Arrange
        pin.setSaveCount(5);
        Pin copiedPin = new Pin();
        copiedPin.setPinId("pin-002");

        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(savedPinRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(savedPin));
        when(pinRepository.findById("pin-002")).thenReturn(Optional.of(copiedPin));
        doNothing().when(pinRepository).delete(copiedPin);
        doNothing().when(savedPinRepository).delete(savedPin);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        pinInteractionService.unsavePin("user-123", "pin-001");

        // Assert
        verify(savedPinRepository).delete(savedPin);
        verify(pinRepository).delete(copiedPin);
        verify(pinRepository).save(pin);
        assertEquals(4, pin.getSaveCount());
    }

    @Test
    void testUnsavePin_PinNotFound() {
        // Arrange
        when(pinRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        PinNotFoundException exception = assertThrows(PinNotFoundException.class, () -> {
            pinInteractionService.unsavePin("user-123", "non-existent");
        });

        assertEquals("Pin not found with ID: non-existent", exception.getMessage());
        verify(savedPinRepository, never()).delete(any(SavedPin.class));
    }

    @Test
    void testUnsavePin_SavedPinNotFound() {
        // Arrange
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(savedPinRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            pinInteractionService.unsavePin("user-123", "pin-001");
        });

        assertEquals("Saved pin not found", exception.getMessage());
        verify(savedPinRepository, never()).delete(any(SavedPin.class));
    }

    @Test
    void testUnsavePin_DecreasesSaveCount() {
        // Arrange
        pin.setSaveCount(10);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(savedPinRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(savedPin));
        when(pinRepository.findById("pin-002")).thenReturn(Optional.empty());
        doNothing().when(savedPinRepository).delete(savedPin);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        pinInteractionService.unsavePin("user-123", "pin-001");

        // Assert
        assertEquals(9, pin.getSaveCount());
    }

    @Test
    void testUnsavePin_SaveCountNeverNegative() {
        // Arrange
        pin.setSaveCount(0);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(savedPinRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(savedPin));
        when(pinRepository.findById("pin-002")).thenReturn(Optional.empty());
        doNothing().when(savedPinRepository).delete(savedPin);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        pinInteractionService.unsavePin("user-123", "pin-001");

        // Assert
        assertEquals(0, pin.getSaveCount());
    }

    @Test
    void testUnsavePin_DeletesCopiedPin() {
        // Arrange
        Pin copiedPin = new Pin();
        copiedPin.setPinId("pin-002");

        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(savedPinRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(savedPin));
        when(pinRepository.findById("pin-002")).thenReturn(Optional.of(copiedPin));
        doNothing().when(pinRepository).delete(copiedPin);
        doNothing().when(savedPinRepository).delete(savedPin);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        pinInteractionService.unsavePin("user-123", "pin-001");

        // Assert
        verify(pinRepository).delete(copiedPin);
    }

    @Test
    void testUnsavePin_NoCopiedPin() {
        // Arrange
        savedPin.setCopiedPinId(null); // No copied pin
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(savedPinRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(savedPin));
        doNothing().when(savedPinRepository).delete(savedPin);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        pinInteractionService.unsavePin("user-123", "pin-001");

        // Assert
        verify(pinRepository, never()).delete(any(Pin.class));
        verify(savedPinRepository).delete(savedPin);
    }

    // ==================== IS SAVED TESTS ====================

    @Test
    void testIsSaved_True() {
        // Arrange
        when(savedPinRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(true);

        // Act
        Boolean result = pinInteractionService.isSaved("user-123", "pin-001");

        // Assert
        assertTrue(result);
        verify(savedPinRepository).existsByPinIdAndUserId("pin-001", "user-123");
    }

    @Test
    void testIsSaved_False() {
        // Arrange
        when(savedPinRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);

        // Act
        Boolean result = pinInteractionService.isSaved("user-123", "pin-001");

        // Assert
        assertFalse(result);
    }

    // ==================== GET SAVED PINS TESTS ====================

    @Test
    void testGetSavedPins_AllBoards() {
        // Arrange
        List<SavedPin> saves = Arrays.asList(savedPin);
        Page<SavedPin> savePage = new PageImpl<>(saves, PageRequest.of(0, 10), 1);

        when(savedPinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(savePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        pinResponse.setPinId("pin-001");
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getSavedPins("user-123", null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).getIsSaved());
        verify(savedPinRepository).findByUserId(eq("user-123"), any(Pageable.class));
    }

    @Test
    void testGetSavedPins_SpecificBoard() {
        // Arrange
        List<SavedPin> saves = Arrays.asList(savedPin);
        Page<SavedPin> savePage = new PageImpl<>(saves, PageRequest.of(0, 10), 1);

        when(savedPinRepository.findByUserIdAndBoardId(eq("user-123"), eq("board-789"), any(Pageable.class)))
                .thenReturn(savePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(true);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getSavedPins("user-123", "board-789", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).getIsLiked());
        verify(savedPinRepository).findByUserIdAndBoardId(eq("user-123"), eq("board-789"), any(Pageable.class));
    }

    @Test
    void testGetSavedPins_EmptyList() {
        // Arrange
        Page<SavedPin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(savedPinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getSavedPins("user-123", null, 0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetSavedPins_WithPagination() {
        // Arrange
        List<SavedPin> saves = Arrays.asList(savedPin);
        Page<SavedPin> savePage = new PageImpl<>(saves, PageRequest.of(2, 5), 30);

        when(savedPinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(savePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getSavedPins("user-123", null, 2, 5);

        // Assert
        assertEquals(2, result.getPagination().getCurrentPage());
        assertEquals(6, result.getPagination().getTotalPages());
        assertEquals(30L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetSavedPins_PinDeleted_FilteredOut() {
        // Arrange
        List<SavedPin> saves = Arrays.asList(savedPin);
        Page<SavedPin> savePage = new PageImpl<>(saves, PageRequest.of(0, 10), 1);

        when(savedPinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(savePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.empty()); // Pin deleted

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getSavedPins("user-123", null, 0, 10);

        // Assert
        assertTrue(result.getData().isEmpty()); // Filtered out
    }

    @Test
    void testGetSavedPins_WithUserAndBoardDetails() {
        // Arrange
        List<SavedPin> saves = Arrays.asList(savedPin);
        Page<SavedPin> savePage = new PageImpl<>(saves, PageRequest.of(0, 10), 1);

        when(savedPinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(savePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(true);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getSavedPins("user-123", null, 0, 10);

        // Assert
        assertNotNull(result.getData().get(0).getCreatedBy());
        assertNotNull(result.getData().get(0).getBoard());
        assertTrue(result.getData().get(0).getIsLiked());
    }

    @Test
    void testGetSavedPins_EmptyBoardId_AllBoards() {
        // Arrange
        List<SavedPin> saves = Arrays.asList(savedPin);
        Page<SavedPin> savePage = new PageImpl<>(saves, PageRequest.of(0, 10), 1);

        when(savedPinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(savePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getSavedPins("user-123", "", 0, 10);

        // Assert
        verify(savedPinRepository).findByUserId(eq("user-123"), any(Pageable.class));
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    void testLikeThenUnlike() {
        // Arrange - Like
        pin.setLikeCount(0);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123")).thenReturn(false);
        when(pinLikeRepository.save(any(PinLike.class))).thenReturn(pinLike);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);
        
        PinLikeResponseDTO expectedResponse = new PinLikeResponseDTO();
        when(modelMapper.map(any(PinLike.class), eq(PinLikeResponseDTO.class))).thenReturn(expectedResponse);

        // Act - Like
        pinInteractionService.likePin("user-123", "pin-001");

        // Arrange - Unlike
        when(pinLikeRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(pinLike));
        doNothing().when(pinLikeRepository).delete(pinLike);

        // Act - Unlike
        pinInteractionService.unlikePin("user-123", "pin-001");

        // Assert
        verify(pinLikeRepository).save(any(PinLike.class));
        verify(pinLikeRepository).delete(pinLike);
        assertEquals(0, pin.getLikeCount());
    }

    @Test
    void testSaveThenUnsave() {
        // Arrange - Save
        Pin newPin = new Pin();
        newPin.setPinId("pin-002");
        pin.setSaveCount(0);

        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(boardRepository.findById("board-789")).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserIdAndBoardId("pin-001", "user-123", "board-789")).thenReturn(false);
        when(pinRepository.save(any(Pin.class))).thenReturn(newPin).thenReturn(pin);
        when(savedPinRepository.save(any(SavedPin.class))).thenReturn(savedPin);
        
        SavedPinResponseDTO expectedResponse = new SavedPinResponseDTO();
        when(modelMapper.map(any(SavedPin.class), eq(SavedPinResponseDTO.class))).thenReturn(expectedResponse);

        // Act - Save
        pinInteractionService.savePin("user-123", "pin-001", "board-789");

        // Arrange - Unsave
        when(savedPinRepository.findByPinIdAndUserId("pin-001", "user-123")).thenReturn(Optional.of(savedPin));
        when(pinRepository.findById("pin-002")).thenReturn(Optional.of(newPin));
        doNothing().when(pinRepository).delete(newPin);
        doNothing().when(savedPinRepository).delete(savedPin);

        // Act - Unsave
        pinInteractionService.unsavePin("user-123", "pin-001");

        // Assert
        verify(savedPinRepository).save(any(SavedPin.class));
        verify(savedPinRepository).delete(savedPin);
        verify(pinRepository).delete(newPin);
        assertEquals(0, pin.getSaveCount());
    }

    @Test
    void testIsLiked_AfterLiking() {
        // Arrange
        when(pinLikeRepository.existsByPinIdAndUserId("pin-001", "user-123"))
                .thenReturn(false, true); // Before and after

        // Act
        Boolean beforeLike = pinInteractionService.isLiked("user-123", "pin-001");
        Boolean afterLike = pinInteractionService.isLiked("user-123", "pin-001");

        // Assert
        assertFalse(beforeLike);
        assertTrue(afterLike);
    }

    @Test
    void testIsSaved_AfterSaving() {
        // Arrange
        when(savedPinRepository.existsByPinIdAndUserId("pin-001", "user-123"))
                .thenReturn(false, true); // Before and after

        // Act
        Boolean beforeSave = pinInteractionService.isSaved("user-123", "pin-001");
        Boolean afterSave = pinInteractionService.isSaved("user-123", "pin-001");

        // Assert
        assertFalse(beforeSave);
        assertTrue(afterSave);
    }

    @Test
    void testGetLikedPins_MultiplePins() {
        // Arrange
        PinLike like2 = new PinLike();
        like2.setLikeId("like-002");
        like2.setPinId("pin-003");
        like2.setUserId("user-123");

        Pin pin2 = new Pin();
        pin2.setPinId("pin-003");
        pin2.setUserId("owner-456");

        List<PinLike> likes = Arrays.asList(pinLike, like2);
        Page<PinLike> likePage = new PageImpl<>(likes, PageRequest.of(0, 10), 2);

        when(pinLikeRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(likePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinRepository.findById("pin-003")).thenReturn(Optional.of(pin2));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById(anyString())).thenReturn(Optional.of(board));
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), eq("user-123"))).thenReturn(false);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getLikedPins("user-123", 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }

    @Test
    void testGetSavedPins_MultiplePins() {
        // Arrange
        SavedPin save2 = new SavedPin();
        save2.setSaveId("save-002");
        save2.setPinId("pin-003");
        save2.setUserId("user-123");

        Pin pin2 = new Pin();
        pin2.setPinId("pin-003");
        pin2.setUserId("owner-456");

        List<SavedPin> saves = Arrays.asList(savedPin, save2);
        Page<SavedPin> savePage = new PageImpl<>(saves, PageRequest.of(0, 10), 2);

        when(savedPinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(savePage);
        when(pinRepository.findById("pin-001")).thenReturn(Optional.of(pin));
        when(pinRepository.findById("pin-003")).thenReturn(Optional.of(pin2));
        when(userRepository.findById("owner-456")).thenReturn(Optional.of(pinOwner));
        when(boardRepository.findById(anyString())).thenReturn(Optional.of(board));
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), eq("user-123"))).thenReturn(false);
        
        PinResponseDTO pinResponse = new PinResponseDTO();
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(pinResponse);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinInteractionService.getSavedPins("user-123", null, 0, 10);

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }
}
