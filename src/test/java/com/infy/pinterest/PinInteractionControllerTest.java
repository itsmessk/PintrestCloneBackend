package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.PinInteractionController;
import com.infy.pinterest.dto.*;
import com.infy.pinterest.exception.PinNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.service.PinInteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@WebMvcTest(PinInteractionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PinInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PinInteractionService pinInteractionService;

    private static final String USER_ID = "user-123";
    private static final String PIN_ID = "pin-456";
    private static final String BOARD_ID = "board-789";
    private static final String LIKE_ID = "like-111";
    private static final String SAVE_ID = "save-222";
    private static final String USER_ID_HEADER = "X-User-Id";

    private PinLikeResponseDTO pinLikeResponseDTO;
    private SavedPinResponseDTO savedPinResponseDTO;
    private PinResponseDTO pinResponseDTO;
    private PaginatedResponse<PinResponseDTO> paginatedResponse;
    private UserSummaryDTO userSummary;
    private BoardSummaryDTO boardSummary;

    @BeforeEach
    void setUp() {
        // Setup user summary
        userSummary = new UserSummaryDTO();
        userSummary.setUserId(USER_ID);
        userSummary.setUsername("testuser");
        userSummary.setProfilePictureUrl("https://example.com/profile.jpg");

        // Setup board summary
        boardSummary = new BoardSummaryDTO();
        boardSummary.setBoardId(BOARD_ID);
        boardSummary.setBoardName("My Board");

        // Setup pin like response DTO
        pinLikeResponseDTO = new PinLikeResponseDTO();
        pinLikeResponseDTO.setLikeId(LIKE_ID);
        pinLikeResponseDTO.setPinId(PIN_ID);
        pinLikeResponseDTO.setUserId(USER_ID);
        pinLikeResponseDTO.setLikedAt(LocalDateTime.now().toString());
        pinLikeResponseDTO.setIsLiked(true);

        // Setup saved pin response DTO
        savedPinResponseDTO = new SavedPinResponseDTO();
        savedPinResponseDTO.setSaveId(SAVE_ID);
        savedPinResponseDTO.setPinId(PIN_ID);
        savedPinResponseDTO.setUserId(USER_ID);
        savedPinResponseDTO.setBoardId(BOARD_ID);
        savedPinResponseDTO.setSavedAt(LocalDateTime.now().toString());
        savedPinResponseDTO.setIsSaved(true);

        // Setup pin response DTO
        pinResponseDTO = new PinResponseDTO();
        pinResponseDTO.setPinId(PIN_ID);
        pinResponseDTO.setUserId(USER_ID);
        pinResponseDTO.setBoardId(BOARD_ID);
        pinResponseDTO.setTitle("Beautiful Sunset");
        pinResponseDTO.setDescription("Amazing sunset at the beach");
        pinResponseDTO.setImageUrl("https://example.com/sunset.jpg");
        pinResponseDTO.setVisibility("PUBLIC");
        pinResponseDTO.setIsDraft(false);
        pinResponseDTO.setIsSponsored(false);
        pinResponseDTO.setSaveCount(10);
        pinResponseDTO.setLikeCount(25);
        pinResponseDTO.setIsLiked(true);
        pinResponseDTO.setIsSaved(false);
        pinResponseDTO.setCreatedBy(userSummary);
        pinResponseDTO.setBoard(boardSummary);
        pinResponseDTO.setCreatedAt(LocalDateTime.now());
        pinResponseDTO.setUpdatedAt(LocalDateTime.now());

        // Setup paginated response
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        paginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(pinResponseDTO),
                pagination
        );
    }

    // ==================== LIKE PIN TESTS ====================

    @Test
    @DisplayName("POST /pins/{pinId}/like - Success")
    void testLikePin_Success() throws Exception {
        // Arrange
        when(pinInteractionService.likePin(USER_ID, PIN_ID))
                .thenReturn(pinLikeResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin liked successfully"))
                .andExpect(jsonPath("$.data.likeId").value(LIKE_ID))
                .andExpect(jsonPath("$.data.pinId").value(PIN_ID))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.isLiked").value(true));

        verify(pinInteractionService, times(1)).likePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/like - Failure - Pin Not Found")
    void testLikePin_Failure_PinNotFound() throws Exception {
        // Arrange
        when(pinInteractionService.likePin(USER_ID, PIN_ID))
                .thenThrow(new PinNotFoundException("Pin not found with ID: " + PIN_ID));

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinInteractionService, times(1)).likePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/like - Failure - Already Liked")
    void testLikePin_Failure_AlreadyLiked() throws Exception {
        // Arrange
        when(pinInteractionService.likePin(USER_ID, PIN_ID))
                .thenThrow(new IllegalStateException("Pin already liked"));

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(pinInteractionService, times(1)).likePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/like - Failure - Missing User ID Header")
    void testLikePin_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinInteractionService, never()).likePin(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /pins/{pinId}/like - Success - With Timestamp")
    void testLikePin_Success_WithTimestamp() throws Exception {
        // Arrange
        when(pinInteractionService.likePin(USER_ID, PIN_ID))
                .thenReturn(pinLikeResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedAt").exists());

        verify(pinInteractionService, times(1)).likePin(USER_ID, PIN_ID);
    }

    // ==================== UNLIKE PIN TESTS ====================

    @Test
    @DisplayName("DELETE /pins/{pinId}/like - Success")
    void testUnlikePin_Success() throws Exception {
        // Arrange
        doNothing().when(pinInteractionService).unlikePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin unliked successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(pinInteractionService, times(1)).unlikePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId}/like - Failure - Pin Not Found")
    void testUnlikePin_Failure_PinNotFound() throws Exception {
        // Arrange
        doThrow(new PinNotFoundException("Pin not found with ID: " + PIN_ID))
                .when(pinInteractionService).unlikePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinInteractionService, times(1)).unlikePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId}/like - Failure - Like Not Found")
    void testUnlikePin_Failure_LikeNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Like not found"))
                .when(pinInteractionService).unlikePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinInteractionService, times(1)).unlikePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId}/like - Failure - Missing User ID Header")
    void testUnlikePin_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}/like", PIN_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinInteractionService, never()).unlikePin(anyString(), anyString());
    }

    // ==================== IS LIKED TESTS ====================

    @Test
    @DisplayName("GET /pins/{pinId}/is-liked - Success - Liked")
    void testIsLiked_Success_Liked() throws Exception {
        // Arrange
        when(pinInteractionService.isLiked(USER_ID, PIN_ID))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}/is-liked", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Like status retrieved"))
                .andExpect(jsonPath("$.data").value(true));

        verify(pinInteractionService, times(1)).isLiked(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("GET /pins/{pinId}/is-liked - Success - Not Liked")
    void testIsLiked_Success_NotLiked() throws Exception {
        // Arrange
        when(pinInteractionService.isLiked(USER_ID, PIN_ID))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}/is-liked", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        verify(pinInteractionService, times(1)).isLiked(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("GET /pins/{pinId}/is-liked - Failure - Missing User ID Header")
    void testIsLiked_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}/is-liked", PIN_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinInteractionService, never()).isLiked(anyString(), anyString());
    }

    // ==================== GET LIKED PINS TESTS ====================

    @Test
    @DisplayName("GET /pins/liked - Success")
    void testGetLikedPins_Success() throws Exception {
        // Arrange
        when(pinInteractionService.getLikedPins(USER_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/liked")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Liked pins retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].pinId").value(PIN_ID))
                .andExpect(jsonPath("$.data.data[0].isLiked").value(true));

        verify(pinInteractionService, times(1)).getLikedPins(USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/liked - Success - Custom Pagination")
    void testGetLikedPins_Success_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(2, 5, 100L, 10, true, true);
        PaginatedResponse<PinResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(pinResponseDTO),
                customPagination
        );
        when(pinInteractionService.getLikedPins(USER_ID, 2, 10))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/liked")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "2")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(5));

        verify(pinInteractionService, times(1)).getLikedPins(USER_ID, 2, 10);
    }

    @Test
    @DisplayName("GET /pins/liked - Success - Empty List")
    void testGetLikedPins_Success_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<PinResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(pinInteractionService.getLikedPins(USER_ID, 0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/liked")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(0));

        verify(pinInteractionService, times(1)).getLikedPins(USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/liked - Success - Multiple Pins")
    void testGetLikedPins_Success_MultiplePins() throws Exception {
        // Arrange
        PinResponseDTO pin2 = new PinResponseDTO();
        pin2.setPinId("pin-999");
        pin2.setTitle("Mountain View");
        pin2.setIsLiked(true);

        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<PinResponseDTO> multipleResponse = new PaginatedResponse<>(
                Arrays.asList(pinResponseDTO, pin2),
                pagination
        );
        when(pinInteractionService.getLikedPins(USER_ID, 0, 20))
                .thenReturn(multipleResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/liked")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].pinId").value(PIN_ID))
                .andExpect(jsonPath("$.data.data[1].pinId").value("pin-999"));

        verify(pinInteractionService, times(1)).getLikedPins(USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/liked - Failure - Missing User ID Header")
    void testGetLikedPins_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pins/liked"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinInteractionService, never()).getLikedPins(anyString(), anyInt(), anyInt());
    }

    // ==================== SAVE PIN TESTS ====================

    @Test
    @DisplayName("POST /pins/{pinId}/save - Success - With Board ID")
    void testSavePin_Success_WithBoardId() throws Exception {
        // Arrange
        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenReturn(savedPinResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin saved successfully"))
                .andExpect(jsonPath("$.data.saveId").value(SAVE_ID))
                .andExpect(jsonPath("$.data.pinId").value(PIN_ID))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.boardId").value(BOARD_ID))
                .andExpect(jsonPath("$.data.isSaved").value(true));

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/save - Success - Without Board ID (null)")
    void testSavePin_Success_WithoutBoardId() throws Exception {
        // Arrange
        when(pinInteractionService.savePin(USER_ID, PIN_ID, null))
                .thenThrow(new IllegalArgumentException("Board ID is required to save a pin"));

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, null);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/save - Failure - Pin Not Found")
    void testSavePin_Failure_PinNotFound() throws Exception {
        // Arrange
        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenThrow(new PinNotFoundException("Pin not found with ID: " + PIN_ID));

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/save - Failure - Board Not Found")
    void testSavePin_Failure_BoardNotFound() throws Exception {
        // Arrange
        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenThrow(new ResourceNotFoundException("Board not found with ID: " + BOARD_ID));

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/save - Failure - Already Saved")
    void testSavePin_Failure_AlreadySaved() throws Exception {
        // Arrange
        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenThrow(new IllegalStateException("Pin already saved to this board"));

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/save - Failure - Not Own Board")
    void testSavePin_Failure_NotOwnBoard() throws Exception {
        // Arrange
        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenThrow(new IllegalStateException("You can only save pins to your own boards"));

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
    }

    @Test
    @DisplayName("POST /pins/{pinId}/save - Failure - Missing User ID Header")
    void testSavePin_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinInteractionService, never()).savePin(anyString(), anyString(), anyString());
    }

    // ==================== UNSAVE PIN TESTS ====================

    @Test
    @DisplayName("DELETE /pins/{pinId}/save - Success")
    void testUnsavePin_Success() throws Exception {
        // Arrange
        doNothing().when(pinInteractionService).unsavePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin unsaved successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(pinInteractionService, times(1)).unsavePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId}/save - Failure - Pin Not Found")
    void testUnsavePin_Failure_PinNotFound() throws Exception {
        // Arrange
        doThrow(new PinNotFoundException("Pin not found with ID: " + PIN_ID))
                .when(pinInteractionService).unsavePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinInteractionService, times(1)).unsavePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId}/save - Failure - Save Not Found")
    void testUnsavePin_Failure_SaveNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Saved pin not found"))
                .when(pinInteractionService).unsavePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinInteractionService, times(1)).unsavePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId}/save - Failure - Missing User ID Header")
    void testUnsavePin_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}/save", PIN_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinInteractionService, never()).unsavePin(anyString(), anyString());
    }

    // ==================== IS SAVED TESTS ====================

    @Test
    @DisplayName("GET /pins/{pinId}/is-saved - Success - Saved")
    void testIsSaved_Success_Saved() throws Exception {
        // Arrange
        when(pinInteractionService.isSaved(USER_ID, PIN_ID))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}/is-saved", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Save status retrieved"))
                .andExpect(jsonPath("$.data").value(true));

        verify(pinInteractionService, times(1)).isSaved(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("GET /pins/{pinId}/is-saved - Success - Not Saved")
    void testIsSaved_Success_NotSaved() throws Exception {
        // Arrange
        when(pinInteractionService.isSaved(USER_ID, PIN_ID))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}/is-saved", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        verify(pinInteractionService, times(1)).isSaved(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("GET /pins/{pinId}/is-saved - Failure - Missing User ID Header")
    void testIsSaved_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}/is-saved", PIN_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinInteractionService, never()).isSaved(anyString(), anyString());
    }

    // ==================== GET SAVED PINS TESTS ====================

    @Test
    @DisplayName("GET /pins/saved - Success - All Saved Pins")
    void testGetSavedPins_Success_AllSaved() throws Exception {
        // Arrange
        pinResponseDTO.setIsSaved(true);
        when(pinInteractionService.getSavedPins(USER_ID, null, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/saved")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Saved pins retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].pinId").value(PIN_ID))
                .andExpect(jsonPath("$.data.data[0].isSaved").value(true));

        verify(pinInteractionService, times(1)).getSavedPins(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/saved - Success - Filter By Board ID")
    void testGetSavedPins_Success_FilterByBoard() throws Exception {
        // Arrange
        pinResponseDTO.setIsSaved(true);
        when(pinInteractionService.getSavedPins(USER_ID, BOARD_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/saved")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(1)));

        verify(pinInteractionService, times(1)).getSavedPins(USER_ID, BOARD_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/saved - Success - Custom Pagination")
    void testGetSavedPins_Success_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(1, 3, 50L, 15, true, true);
        PaginatedResponse<PinResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(pinResponseDTO),
                customPagination
        );
        when(pinInteractionService.getSavedPins(USER_ID, null, 1, 15))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/saved")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "1")
                        .param("size", "15"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(3));

        verify(pinInteractionService, times(1)).getSavedPins(USER_ID, null, 1, 15);
    }

    @Test
    @DisplayName("GET /pins/saved - Success - Empty List")
    void testGetSavedPins_Success_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<PinResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(pinInteractionService.getSavedPins(USER_ID, null, 0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/saved")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(pinInteractionService, times(1)).getSavedPins(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/saved - Success - Multiple Saved Pins")
    void testGetSavedPins_Success_MultiplePins() throws Exception {
        // Arrange
        PinResponseDTO pin2 = new PinResponseDTO();
        pin2.setPinId("pin-888");
        pin2.setTitle("City Lights");
        pin2.setIsSaved(true);

        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<PinResponseDTO> multipleResponse = new PaginatedResponse<>(
                Arrays.asList(pinResponseDTO, pin2),
                pagination
        );
        when(pinInteractionService.getSavedPins(USER_ID, null, 0, 20))
                .thenReturn(multipleResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/saved")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].isSaved").value(true))
                .andExpect(jsonPath("$.data.data[1].isSaved").value(true));

        verify(pinInteractionService, times(1)).getSavedPins(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/saved - Failure - Missing User ID Header")
    void testGetSavedPins_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pins/saved"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinInteractionService, never()).getSavedPins(anyString(), anyString(), anyInt(), anyInt());
    }

    // ==================== INTEGRATION & WORKFLOW TESTS ====================

    @Test
    @DisplayName("Complete Like Workflow - Like, Check, Unlike")
    void testCompleteLikeWorkflow() throws Exception {
        // Arrange
        when(pinInteractionService.likePin(USER_ID, PIN_ID))
                .thenReturn(pinLikeResponseDTO);
        when(pinInteractionService.isLiked(USER_ID, PIN_ID))
                .thenReturn(true);
        doNothing().when(pinInteractionService).unlikePin(USER_ID, PIN_ID);
        when(pinInteractionService.isLiked(USER_ID, PIN_ID))
                .thenReturn(false);

        // Like pin
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(true));

        // Check if liked
        mockMvc.perform(get("/pins/{pinId}/is-liked", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // Unlike pin
        mockMvc.perform(delete("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Check if unliked
        mockMvc.perform(get("/pins/{pinId}/is-liked", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        verify(pinInteractionService, times(1)).likePin(USER_ID, PIN_ID);
        verify(pinInteractionService, times(2)).isLiked(USER_ID, PIN_ID);
        verify(pinInteractionService, times(1)).unlikePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("Complete Save Workflow - Save, Check, Unsave")
    void testCompleteSaveWorkflow() throws Exception {
        // Arrange
        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenReturn(savedPinResponseDTO);
        when(pinInteractionService.isSaved(USER_ID, PIN_ID))
                .thenReturn(true);
        doNothing().when(pinInteractionService).unsavePin(USER_ID, PIN_ID);
        when(pinInteractionService.isSaved(USER_ID, PIN_ID))
                .thenReturn(false);

        // Save pin
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isSaved").value(true));

        // Check if saved
        mockMvc.perform(get("/pins/{pinId}/is-saved", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // Unsave pin
        mockMvc.perform(delete("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Check if unsaved
        mockMvc.perform(get("/pins/{pinId}/is-saved", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
        verify(pinInteractionService, times(2)).isSaved(USER_ID, PIN_ID);
        verify(pinInteractionService, times(1)).unsavePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("Pagination Boundary Test - Last Page Liked Pins")
    void testPaginationBoundary_LastPageLikedPins() throws Exception {
        // Arrange
        PaginationDTO lastPagePagination = new PaginationDTO(4, 5, 100L, 20, false, true);
        PaginatedResponse<PinResponseDTO> lastPageResponse = new PaginatedResponse<>(
                Collections.singletonList(pinResponseDTO),
                lastPagePagination
        );
        when(pinInteractionService.getLikedPins(USER_ID, 4, 20))
                .thenReturn(lastPageResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/liked")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "4"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(4))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false))
                .andExpect(jsonPath("$.data.pagination.hasPrevious").value(true));

        verify(pinInteractionService, times(1)).getLikedPins(USER_ID, 4, 20);
    }

    @Test
    @DisplayName("Pagination Boundary Test - Last Page Saved Pins")
    void testPaginationBoundary_LastPageSavedPins() throws Exception {
        // Arrange
        PaginationDTO lastPagePagination = new PaginationDTO(3, 4, 80L, 20, false, true);
        PaginatedResponse<PinResponseDTO> lastPageResponse = new PaginatedResponse<>(
                Collections.singletonList(pinResponseDTO),
                lastPagePagination
        );
        when(pinInteractionService.getSavedPins(USER_ID, null, 3, 20))
                .thenReturn(lastPageResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/saved")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(3))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false));

        verify(pinInteractionService, times(1)).getSavedPins(USER_ID, null, 3, 20);
    }

    @Test
    @DisplayName("Edge Case - Like and Save Same Pin")
    void testEdgeCase_LikeAndSaveSamePin() throws Exception {
        // Arrange
        when(pinInteractionService.likePin(USER_ID, PIN_ID))
                .thenReturn(pinLikeResponseDTO);
        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenReturn(savedPinResponseDTO);

        // Like pin
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Save same pin
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andExpect(status().isOk());

        verify(pinInteractionService, times(1)).likePin(USER_ID, PIN_ID);
        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
    }

    @Test
    @DisplayName("Edge Case - Multiple Different Users Liking Same Pin")
    void testEdgeCase_MultipleUsersLikingSamePin() throws Exception {
        // Arrange
        String user2 = "user-222";
        PinLikeResponseDTO like2 = new PinLikeResponseDTO();
        like2.setUserId(user2);
        like2.setPinId(PIN_ID);
        like2.setIsLiked(true);

        when(pinInteractionService.likePin(USER_ID, PIN_ID))
                .thenReturn(pinLikeResponseDTO);
        when(pinInteractionService.likePin(user2, PIN_ID))
                .thenReturn(like2);

        // User 1 likes
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // User 2 likes
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, user2))
                .andExpect(status().isOk());

        verify(pinInteractionService, times(1)).likePin(USER_ID, PIN_ID);
        verify(pinInteractionService, times(1)).likePin(user2, PIN_ID);
    }

    @Test
    @DisplayName("Edge Case - Save Pin to Multiple Boards")
    void testEdgeCase_SavePinToMultipleBoards() throws Exception {
        // Arrange
        String board2 = "board-999";
        SavedPinResponseDTO save2 = new SavedPinResponseDTO();
        save2.setBoardId(board2);
        save2.setPinId(PIN_ID);
        save2.setIsSaved(true);

        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenReturn(savedPinResponseDTO);
        when(pinInteractionService.savePin(USER_ID, PIN_ID, board2))
                .thenReturn(save2);

        // Save to board 1
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andExpect(status().isOk());

        // Save to board 2
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", board2))
                .andExpect(status().isOk());

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, board2);
    }

    @Test
    @DisplayName("User Collection View - Get All Liked and Saved Pins")
    void testUserCollectionView() throws Exception {
        // Arrange
        when(pinInteractionService.getLikedPins(USER_ID, 0, 20))
                .thenReturn(paginatedResponse);
        when(pinInteractionService.getSavedPins(USER_ID, null, 0, 20))
                .thenReturn(paginatedResponse);

        // Get liked pins
        mockMvc.perform(get("/pins/liked")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(1)));

        // Get saved pins
        mockMvc.perform(get("/pins/saved")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(1)));

        verify(pinInteractionService, times(1)).getLikedPins(USER_ID, 0, 20);
        verify(pinInteractionService, times(1)).getSavedPins(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("Filter Saved Pins by Specific Board")
    void testFilterSavedPinsByBoard() throws Exception {
        // Arrange
        when(pinInteractionService.getSavedPins(USER_ID, BOARD_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/saved")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].boardId").value(BOARD_ID));

        verify(pinInteractionService, times(1)).getSavedPins(USER_ID, BOARD_ID, 0, 20);
    }

    @Test
    @DisplayName("Response DTO Validation - Like Response Has All Fields")
    void testResponseValidation_LikeResponse() throws Exception {
        // Arrange
        when(pinInteractionService.likePin(USER_ID, PIN_ID))
                .thenReturn(pinLikeResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/like", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeId").exists())
                .andExpect(jsonPath("$.data.pinId").exists())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.likedAt").exists())
                .andExpect(jsonPath("$.data.isLiked").exists());

        verify(pinInteractionService, times(1)).likePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("Response DTO Validation - Save Response Has All Fields")
    void testResponseValidation_SaveResponse() throws Exception {
        // Arrange
        when(pinInteractionService.savePin(USER_ID, PIN_ID, BOARD_ID))
                .thenReturn(savedPinResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/pins/{pinId}/save", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("boardId", BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saveId").exists())
                .andExpect(jsonPath("$.data.pinId").exists())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.boardId").exists())
                .andExpect(jsonPath("$.data.savedAt").exists())
                .andExpect(jsonPath("$.data.isSaved").exists());

        verify(pinInteractionService, times(1)).savePin(USER_ID, PIN_ID, BOARD_ID);
    }
}
