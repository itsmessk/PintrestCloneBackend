package com.infy.pinterest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PinCreationDTO;
import com.infy.pinterest.dto.PinDraftDTO;
import com.infy.pinterest.dto.PinResponseDTO;
import com.infy.pinterest.dto.PinUpdateDTO;
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
import com.infy.pinterest.service.PinService;
import com.infy.pinterest.utility.FileUploadService;

@ExtendWith(MockitoExtension.class)
class PinServiceTest {

    @Mock
    private PinRepository pinRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoardCollaboratorRepository collaboratorRepository;

    @Mock
    private PinLikeRepository pinLikeRepository;

    @Mock
    private SavedPinRepository savedPinRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PinService pinService;

    private User testUser;
    private Board testBoard;
    private Pin testPin;
    private PinCreationDTO pinCreationDTO;
    private PinUpdateDTO pinUpdateDTO;
    private PinDraftDTO pinDraftDTO;
    private MultipartFile mockImage;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setUserId("user-123");
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setProfilePictureUrl("https://example.com/profile.jpg");
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

        // Setup test board
        testBoard = new Board();
        testBoard.setBoardId("board-123");
        testBoard.setUserId("user-123");
        testBoard.setName("Test Board");
        testBoard.setDescription("Test board description");
        testBoard.setVisibility(Board.Visibility.PUBLIC);
        testBoard.setPinCount(0);
        testBoard.setCreatedAt(LocalDateTime.now());

        // Setup test pin
        testPin = new Pin();
        testPin.setPinId("pin-123");
        testPin.setUserId("user-123");
        testPin.setBoardId("board-123");
        testPin.setTitle("Test Pin");
        testPin.setDescription("Test pin description");
        testPin.setImageUrl("https://example.com/image.jpg");
        testPin.setVisibility(Pin.Visibility.PUBLIC);
        testPin.setIsDraft(false);
        testPin.setIsSponsored(false);
        testPin.setSaveCount(0);
        testPin.setLikeCount(0);
        testPin.setCreatedAt(LocalDateTime.now());
        testPin.setUpdatedAt(LocalDateTime.now());

        // Setup DTOs
        pinCreationDTO = new PinCreationDTO();
        pinCreationDTO.setTitle("New Pin");
        pinCreationDTO.setDescription("New pin description");
        pinCreationDTO.setBoardId("board-123");
        pinCreationDTO.setVisibility("PUBLIC");
        pinCreationDTO.setSourceUrl("https://source.com");

        pinUpdateDTO = new PinUpdateDTO();
        pinUpdateDTO.setTitle("Updated Pin");
        pinUpdateDTO.setDescription("Updated description");

        pinDraftDTO = new PinDraftDTO();
        pinDraftDTO.setTitle("Draft Pin");
        pinDraftDTO.setDescription("Draft description");
        pinDraftDTO.setBoardId("board-123");

        // Mock image
        mockImage = mock(MultipartFile.class);
    }

    // ==================== CREATE PIN TESTS ====================

    @Test
    void testCreatePin_Success_AsOwner() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(fileUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://example.com/uploaded.jpg");
        when(pinRepository.save(any(Pin.class))).thenReturn(testPin);
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.createPin("user-123", pinCreationDTO, mockImage);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById("user-123");
        verify(boardRepository).findById("board-123");
        verify(fileUploadService).uploadImage(mockImage);
        verify(pinRepository).save(any(Pin.class));
    }

    @Test
    void testCreatePin_Success_AsCollaboratorWithEditPermission() {
        // Arrange
        Board collaborativeBoard = new Board();
        collaborativeBoard.setBoardId("board-456");
        collaborativeBoard.setUserId("owner-789");
        collaborativeBoard.setName("Collaborative Board");

        BoardCollaborator collaborator = new BoardCollaborator();
        collaborator.setBoardId("board-456");
        collaborator.setUserId("user-123");
        collaborator.setPermission(Invitation.Permission.EDIT);

        pinCreationDTO.setBoardId("board-456");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-456")).thenReturn(Optional.of(collaborativeBoard));
        when(collaboratorRepository.findByBoardIdAndUserId("board-456", "user-123"))
                .thenReturn(Optional.of(collaborator));
        when(fileUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://example.com/uploaded.jpg");
        when(pinRepository.save(any(Pin.class))).thenReturn(testPin);
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.createPin("user-123", pinCreationDTO, mockImage);

        // Assert
        assertNotNull(result);
        verify(collaboratorRepository).findByBoardIdAndUserId("board-456", "user-123");
    }

    @Test
    void testCreatePin_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            pinService.createPin("user-123", pinCreationDTO, mockImage);
        });

        verify(userRepository).findById("user-123");
        verify(boardRepository, never()).findById(anyString());
        verify(pinRepository, never()).save(any(Pin.class));
    }

    @Test
    void testCreatePin_BoardNotFound() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BoardNotFoundException.class, () -> {
            pinService.createPin("user-123", pinCreationDTO, mockImage);
        });

        verify(boardRepository).findById("board-123");
        verify(pinRepository, never()).save(any(Pin.class));
    }

    @Test
    void testCreatePin_UnauthorizedAccess_NotOwnerOrCollaborator() {
        // Arrange
        Board otherUserBoard = new Board();
        otherUserBoard.setBoardId("board-456");
        otherUserBoard.setUserId("other-user");

        pinCreationDTO.setBoardId("board-456");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-456")).thenReturn(Optional.of(otherUserBoard));
        when(collaboratorRepository.findByBoardIdAndUserId("board-456", "user-123"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> {
            pinService.createPin("user-123", pinCreationDTO, mockImage);
        });

        verify(pinRepository, never()).save(any(Pin.class));
    }

    @Test
    void testCreatePin_UnauthorizedAccess_CollaboratorWithViewPermission() {
        // Arrange
        Board collaborativeBoard = new Board();
        collaborativeBoard.setBoardId("board-456");
        collaborativeBoard.setUserId("owner-789");

        BoardCollaborator viewCollaborator = new BoardCollaborator();
        viewCollaborator.setBoardId("board-456");
        viewCollaborator.setUserId("user-123");
        viewCollaborator.setPermission(Invitation.Permission.VIEW);

        pinCreationDTO.setBoardId("board-456");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-456")).thenReturn(Optional.of(collaborativeBoard));
        when(collaboratorRepository.findByBoardIdAndUserId("board-456", "user-123"))
                .thenReturn(Optional.of(viewCollaborator));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> {
            pinService.createPin("user-123", pinCreationDTO, mockImage);
        });

        verify(pinRepository, never()).save(any(Pin.class));
    }

    // ==================== CREATE PIN DRAFT TESTS ====================

    @Test
    void testCreatePinDraft_Success() {
        // Arrange
        Pin draftPin = new Pin();
        draftPin.setPinId("pin-draft");
        draftPin.setUserId("user-123");
        draftPin.setBoardId("board-123");
        draftPin.setTitle("Draft Pin");
        draftPin.setIsDraft(true);

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(pinRepository.save(any(Pin.class))).thenReturn(draftPin);
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.createPinDraft("user-123", pinDraftDTO);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById("user-123");
        verify(boardRepository).findById("board-123");
        verify(pinRepository).save(any(Pin.class));
        verify(fileUploadService, never()).uploadImage(any(MultipartFile.class));
    }

    @Test
    void testCreatePinDraft_UserNotFound() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            pinService.createPinDraft("user-123", pinDraftDTO);
        });

        verify(pinRepository, never()).save(any(Pin.class));
    }

    @Test
    void testCreatePinDraft_BoardNotFound() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BoardNotFoundException.class, () -> {
            pinService.createPinDraft("user-123", pinDraftDTO);
        });

        verify(pinRepository, never()).save(any(Pin.class));
    }

    // ==================== UPDATE PIN TESTS ====================

    @Test
    void testUpdatePin_Success_AsOwner() {
        // Arrange
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(pinRepository.save(any(Pin.class))).thenReturn(testPin);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.updatePin("user-123", "pin-123", pinUpdateDTO);

        // Assert
        assertNotNull(result);
        verify(pinRepository).findById("pin-123");
        verify(pinRepository).save(any(Pin.class));
    }

    @Test
    void testUpdatePin_Success_UpdateAllFields() {
        // Arrange
        pinUpdateDTO.setTitle("Updated Title");
        pinUpdateDTO.setDescription("Updated Description");
        pinUpdateDTO.setBoardId("board-123");
        pinUpdateDTO.setVisibility("PRIVATE");

        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(pinRepository.save(any(Pin.class))).thenReturn(testPin);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.updatePin("user-123", "pin-123", pinUpdateDTO);

        // Assert
        assertNotNull(result);
        verify(pinRepository).save(any(Pin.class));
    }

    @Test
    void testUpdatePin_Success_AsCollaboratorWithEditPermission() {
        // Arrange
        Board collaborativeBoard = new Board();
        collaborativeBoard.setBoardId("board-456");
        collaborativeBoard.setUserId("owner-789");

        Pin collaborativePin = new Pin();
        collaborativePin.setPinId("pin-456");
        collaborativePin.setUserId("owner-789");
        collaborativePin.setBoardId("board-456");
        collaborativePin.setTitle("Pin in collaborative board");

        BoardCollaborator collaborator = new BoardCollaborator();
        collaborator.setBoardId("board-456");
        collaborator.setUserId("user-123");
        collaborator.setPermission(Invitation.Permission.EDIT);

        when(pinRepository.findById("pin-456")).thenReturn(Optional.of(collaborativePin));
        when(boardRepository.findById("board-456")).thenReturn(Optional.of(collaborativeBoard));
        when(collaboratorRepository.findByBoardIdAndUserId("board-456", "user-123"))
                .thenReturn(Optional.of(collaborator));
        when(pinRepository.save(any(Pin.class))).thenReturn(collaborativePin);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.updatePin("user-123", "pin-456", pinUpdateDTO);

        // Assert
        assertNotNull(result);
        verify(collaboratorRepository).findByBoardIdAndUserId("board-456", "user-123");
    }

    @Test
    void testUpdatePin_PinNotFound() {
        // Arrange
        when(pinRepository.findById("pin-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PinNotFoundException.class, () -> {
            pinService.updatePin("user-123", "pin-999", pinUpdateDTO);
        });

        verify(pinRepository, never()).save(any(Pin.class));
    }

    @Test
    void testUpdatePin_UnauthorizedAccess() {
        // Arrange
        Board otherBoard = new Board();
        otherBoard.setBoardId("board-456");
        otherBoard.setUserId("other-user");

        Pin otherPin = new Pin();
        otherPin.setPinId("pin-456");
        otherPin.setBoardId("board-456");

        when(pinRepository.findById("pin-456")).thenReturn(Optional.of(otherPin));
        when(boardRepository.findById("board-456")).thenReturn(Optional.of(otherBoard));
        when(collaboratorRepository.findByBoardIdAndUserId("board-456", "user-123"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> {
            pinService.updatePin("user-123", "pin-456", pinUpdateDTO);
        });

        verify(pinRepository, never()).save(any(Pin.class));
    }

    @Test
    void testUpdatePin_UpdateBoardId_NewBoardNotFound() {
        // Arrange
        pinUpdateDTO.setBoardId("board-999");

        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(boardRepository.findById("board-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BoardNotFoundException.class, () -> {
            pinService.updatePin("user-123", "pin-123", pinUpdateDTO);
        });

        verify(pinRepository, never()).save(any(Pin.class));
    }

    @Test
    void testUpdatePin_PartialUpdate_OnlyTitle() {
        // Arrange
        PinUpdateDTO partialUpdate = new PinUpdateDTO();
        partialUpdate.setTitle("Only Title Updated");

        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(pinRepository.save(any(Pin.class))).thenReturn(testPin);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.updatePin("user-123", "pin-123", partialUpdate);

        // Assert
        assertNotNull(result);
        verify(pinRepository).save(any(Pin.class));
    }

    @Test
    void testUpdatePin_EmptyTitleIgnored() {
        // Arrange
        PinUpdateDTO updateWithEmptyTitle = new PinUpdateDTO();
        updateWithEmptyTitle.setTitle("");
        updateWithEmptyTitle.setDescription("New Description");

        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(pinRepository.save(any(Pin.class))).thenReturn(testPin);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.updatePin("user-123", "pin-123", updateWithEmptyTitle);

        // Assert
        assertNotNull(result);
    }

    // ==================== DELETE PIN TESTS ====================

    @Test
    void testDeletePin_Success_AsOwner() {
        // Arrange
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        doNothing().when(pinRepository).delete(any(Pin.class));

        // Act
        pinService.deletePin("user-123", "pin-123");

        // Assert
        verify(pinRepository).findById("pin-123");
        verify(pinRepository).delete(testPin);
    }

    @Test
    void testDeletePin_Success_AsCollaboratorWithEditPermission() {
        // Arrange
        Board collaborativeBoard = new Board();
        collaborativeBoard.setBoardId("board-456");
        collaborativeBoard.setUserId("owner-789");

        Pin collaborativePin = new Pin();
        collaborativePin.setPinId("pin-456");
        collaborativePin.setBoardId("board-456");

        BoardCollaborator collaborator = new BoardCollaborator();
        collaborator.setBoardId("board-456");
        collaborator.setUserId("user-123");
        collaborator.setPermission(Invitation.Permission.EDIT);

        when(pinRepository.findById("pin-456")).thenReturn(Optional.of(collaborativePin));
        when(boardRepository.findById("board-456")).thenReturn(Optional.of(collaborativeBoard));
        when(collaboratorRepository.findByBoardIdAndUserId("board-456", "user-123"))
                .thenReturn(Optional.of(collaborator));
        doNothing().when(pinRepository).delete(any(Pin.class));

        // Act
        pinService.deletePin("user-123", "pin-456");

        // Assert
        verify(pinRepository).delete(collaborativePin);
    }

    @Test
    void testDeletePin_PinNotFound() {
        // Arrange
        when(pinRepository.findById("pin-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PinNotFoundException.class, () -> {
            pinService.deletePin("user-123", "pin-999");
        });

        verify(pinRepository, never()).delete(any(Pin.class));
    }

    @Test
    void testDeletePin_UnauthorizedAccess() {
        // Arrange
        Board otherBoard = new Board();
        otherBoard.setBoardId("board-456");
        otherBoard.setUserId("other-user");

        Pin otherPin = new Pin();
        otherPin.setPinId("pin-456");
        otherPin.setBoardId("board-456");

        when(pinRepository.findById("pin-456")).thenReturn(Optional.of(otherPin));
        when(boardRepository.findById("board-456")).thenReturn(Optional.of(otherBoard));
        when(collaboratorRepository.findByBoardIdAndUserId("board-456", "user-123"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> {
            pinService.deletePin("user-123", "pin-456");
        });

        verify(pinRepository, never()).delete(any(Pin.class));
    }

    // ==================== GET PIN BY ID TESTS ====================

    @Test
    void testGetPinById_Success_WithRequestingUser() {
        // Arrange
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId("pin-123", "user-123")).thenReturn(true);
        when(savedPinRepository.existsByPinIdAndUserId("pin-123", "user-123")).thenReturn(false);

        // Act
        PinResponseDTO result = pinService.getPinById("pin-123", "user-123");

        // Assert
        assertNotNull(result);
        verify(pinRepository).findById("pin-123");
        verify(pinLikeRepository).existsByPinIdAndUserId("pin-123", "user-123");
        verify(savedPinRepository).existsByPinIdAndUserId("pin-123", "user-123");
    }

    @Test
    void testGetPinById_Success_WithoutRequestingUser() {
        // Arrange
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.getPinById("pin-123", null);

        // Assert
        assertNotNull(result);
        verify(pinLikeRepository, never()).existsByPinIdAndUserId(anyString(), anyString());
        verify(savedPinRepository, never()).existsByPinIdAndUserId(anyString(), anyString());
    }

    @Test
    void testGetPinById_PinNotFound() {
        // Arrange
        when(pinRepository.findById("pin-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PinNotFoundException.class, () -> {
            pinService.getPinById("pin-999", "user-123");
        });
    }

    @Test
    void testGetPinById_UserNotFound() {
        // Arrange
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.getPinById("pin-123", "user-123");

        // Assert
        assertNotNull(result);
    }

    // ==================== GET USER PINS TESTS ====================

    @Test
    void testGetUserPins_Success() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 1);

        when(pinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getUserPins("user-123", "user-123", 0, 10, "createdAt");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertNotNull(result.getPagination());
        assertEquals(0, result.getPagination().getCurrentPage());
        verify(pinRepository).findByUserId(eq("user-123"), any(Pageable.class));
    }

    @Test
    void testGetUserPins_EmptyResults() {
        // Arrange
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(pinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getUserPins("user-123", "user-123", 0, 10, "createdAt");

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPagination().getTotalPages());
    }

    @Test
    void testGetUserPins_CustomSorting() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 1);

        when(pinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getUserPins("user-123", "user-123", 0, 10, "updatedAt");

        // Assert
        assertNotNull(result);
        verify(pinRepository).findByUserId(eq("user-123"), any(Pageable.class));
    }

    @Test
    void testGetUserPins_Pagination() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(2, 5), 20);

        when(pinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getUserPins("user-123", "user-123", 2, 5, "createdAt");

        // Assert
        assertEquals(2, result.getPagination().getCurrentPage());
        assertEquals(4, result.getPagination().getTotalPages());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    // ==================== GET BOARD PINS TESTS ====================

    @Test
    void testGetBoardPins_Success() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 1);

        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(pinRepository.findByBoardId(eq("board-123"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getBoardPins("board-123", "user-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(boardRepository).findById("board-123");
        verify(pinRepository).findByBoardId(eq("board-123"), any(Pageable.class));
    }

    @Test
    void testGetBoardPins_BoardNotFound() {
        // Arrange
        when(boardRepository.findById("board-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BoardNotFoundException.class, () -> {
            pinService.getBoardPins("board-999", "user-123", 0, 10);
        });

        verify(pinRepository, never()).findByBoardId(anyString(), any(Pageable.class));
    }

    @Test
    void testGetBoardPins_EmptyBoard() {
        // Arrange
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(pinRepository.findByBoardId(eq("board-123"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getBoardPins("board-123", "user-123", 0, 10);

        // Assert
        assertTrue(result.getData().isEmpty());
    }

    // ==================== GET USER DRAFTS TESTS ====================

    @Test
    void testGetUserDrafts_Success() {
        // Arrange
        Pin draftPin = new Pin();
        draftPin.setPinId("pin-draft");
        draftPin.setUserId("user-123");
        draftPin.setBoardId("board-123");
        draftPin.setTitle("Draft Pin");
        draftPin.setIsDraft(true);

        List<Pin> drafts = Arrays.asList(draftPin);
        Page<Pin> draftPage = new PageImpl<>(drafts, PageRequest.of(0, 10), 1);

        when(pinRepository.findByUserIdAndIsDraft(eq("user-123"), eq(true), any(Pageable.class)))
                .thenReturn(draftPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getUserDrafts("user-123", "user-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(pinRepository).findByUserIdAndIsDraft(eq("user-123"), eq(true), any(Pageable.class));
    }

    @Test
    void testGetUserDrafts_EmptyResults() {
        // Arrange
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(pinRepository.findByUserIdAndIsDraft(eq("user-123"), eq(true), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getUserDrafts("user-123", "user-123", 0, 10);

        // Assert
        assertTrue(result.getData().isEmpty());
    }

    // ==================== SEARCH PINS TESTS ====================

    @Test
    void testSearchPins_Success() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(eq("sunset"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.searchPins("sunset", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(pinRepository).searchPins(eq("sunset"), any(Pageable.class));
    }

    @Test
    void testSearchPins_NoResults() {
        // Arrange
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(pinRepository.searchPins(eq("nonexistent"), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.searchPins("nonexistent", 0, 20);

        // Assert
        assertTrue(result.getData().isEmpty());
    }

    // ==================== GET PUBLIC PINS TESTS ====================

    @Test
    void testGetPublicPins_WithAuthenticatedUser_WithCollaborations() {
        // Arrange
        BoardCollaborator collaborator = new BoardCollaborator();
        collaborator.setBoardId("board-collab");
        collaborator.setUserId("user-123");

        List<BoardCollaborator> collaborations = Arrays.asList(collaborator);
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(collaboratorRepository.findByUserId("user-123")).thenReturn(collaborations);
        when(pinRepository.findAccessiblePinsForUser(eq("user-123"), anyList(), any(Pageable.class)))
                .thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getPublicPins("user-123", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(collaboratorRepository).findByUserId("user-123");
        verify(pinRepository).findAccessiblePinsForUser(eq("user-123"), anyList(), any(Pageable.class));
    }

    @Test
    void testGetPublicPins_WithAuthenticatedUser_NoCollaborations() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(collaboratorRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());
        when(pinRepository.findAccessiblePinsForUser(eq("user-123"), anyList(), any(Pageable.class)))
                .thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getPublicPins("user-123", 0, 20);

        // Assert
        assertNotNull(result);
        verify(collaboratorRepository).findByUserId("user-123");
    }

    @Test
    void testGetPublicPins_UnauthenticatedUser() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.findAllPublicPins(any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getPublicPins(null, 0, 20);

        // Assert
        assertNotNull(result);
        verify(pinRepository).findAllPublicPins(any(Pageable.class));
        verify(collaboratorRepository, never()).findByUserId(anyString());
    }

    @Test
    void testGetPublicPins_EmptyUserId() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.findAllPublicPins(any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getPublicPins("", 0, 20);

        // Assert
        assertNotNull(result);
        verify(pinRepository).findAllPublicPins(any(Pageable.class));
    }

    @Test
    void testGetPublicPins_Pagination() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(1, 10), 30);

        when(pinRepository.findAllPublicPins(any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getPublicPins(null, 1, 10);

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(3, result.getPagination().getTotalPages());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    // ==================== EDGE CASES AND INTEGRATION TESTS ====================

    @Test
    void testCreatePin_WithAllOptionalFields() {
        // Arrange
        pinCreationDTO.setSourceUrl("https://source.example.com");
        pinCreationDTO.setDescription("Detailed description");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(fileUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://example.com/uploaded.jpg");
        when(pinRepository.save(any(Pin.class))).thenReturn(testPin);
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.createPin("user-123", pinCreationDTO, mockImage);

        // Assert
        assertNotNull(result);
        verify(pinRepository).save(any(Pin.class));
    }

    @Test
    void testGetPinById_WithLikedAndSaved() {
        // Arrange
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId("pin-123", "user-123")).thenReturn(true);
        when(savedPinRepository.existsByPinIdAndUserId("pin-123", "user-123")).thenReturn(true);

        // Act
        PinResponseDTO result = pinService.getPinById("pin-123", "user-123");

        // Assert
        assertNotNull(result);
        verify(pinLikeRepository).existsByPinIdAndUserId("pin-123", "user-123");
        verify(savedPinRepository).existsByPinIdAndUserId("pin-123", "user-123");
    }

    @Test
    void testGetUserPins_MultiplePins() {
        // Arrange
        Pin pin1 = new Pin();
        pin1.setPinId("pin-1");
        pin1.setUserId("user-123");
        pin1.setBoardId("board-123");
        pin1.setTitle("Pin 1");

        Pin pin2 = new Pin();
        pin2.setPinId("pin-2");
        pin2.setUserId("user-123");
        pin2.setBoardId("board-123");
        pin2.setTitle("Pin 2");

        List<Pin> pins = Arrays.asList(pin1, pin2);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 2);

        when(pinRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getUserPins("user-123", "user-123", 0, 10, "createdAt");

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getPagination().getTotalItems());
    }

    @Test
    void testUpdatePin_ChangeVisibilityToPrivate() {
        // Arrange
        pinUpdateDTO.setVisibility("PRIVATE");

        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(testPin));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(pinRepository.save(any(Pin.class))).thenReturn(testPin);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());

        // Act
        PinResponseDTO result = pinService.updatePin("user-123", "pin-123", pinUpdateDTO);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testGetPublicPins_MultipleCollaborativeBoards() {
        // Arrange
        BoardCollaborator collab1 = new BoardCollaborator();
        collab1.setBoardId("board-1");
        collab1.setUserId("user-123");

        BoardCollaborator collab2 = new BoardCollaborator();
        collab2.setBoardId("board-2");
        collab2.setUserId("user-123");

        List<BoardCollaborator> collaborations = Arrays.asList(collab1, collab2);
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(collaboratorRepository.findByUserId("user-123")).thenReturn(collaborations);
        when(pinRepository.findAccessiblePinsForUser(eq("user-123"), anyList(), any(Pageable.class)))
                .thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findById("board-123")).thenReturn(Optional.of(testBoard));
        when(modelMapper.map(any(Pin.class), eq(PinResponseDTO.class))).thenReturn(new PinResponseDTO());
        when(pinLikeRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(savedPinRepository.existsByPinIdAndUserId(anyString(), anyString())).thenReturn(false);

        // Act
        PaginatedResponse<PinResponseDTO> result = pinService.getPublicPins("user-123", 0, 20);

        // Assert
        assertNotNull(result);
        verify(collaboratorRepository).findByUserId("user-123");
    }
}
