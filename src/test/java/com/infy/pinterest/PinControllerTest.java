package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.PinController;
import com.infy.pinterest.dto.*;
import com.infy.pinterest.exception.PinNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.service.PinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
@WebMvcTest(PinController.class)
@AutoConfigureMockMvc(addFilters = false)
class PinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PinService pinService;

    private static final String USER_ID = "user-123";
    private static final String PIN_ID = "pin-456";
    private static final String BOARD_ID = "board-789";
    private static final String USER_ID_HEADER = "X-User-Id";

    private PinResponseDTO pinResponseDTO;
    private PinUpdateDTO pinUpdateDTO;
    private PinDraftDTO pinDraftDTO;
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
        boardSummary.setBoardName("Travel Board");

        // Setup pin response DTO
        pinResponseDTO = new PinResponseDTO();
        pinResponseDTO.setPinId(PIN_ID);
        pinResponseDTO.setUserId(USER_ID);
        pinResponseDTO.setBoardId(BOARD_ID);
        pinResponseDTO.setTitle("Beautiful Sunset");
        pinResponseDTO.setDescription("Amazing sunset at the beach");
        pinResponseDTO.setImageUrl("https://example.com/sunset.jpg");
        pinResponseDTO.setSourceUrl("https://source.com/original");
        pinResponseDTO.setVisibility("PUBLIC");
        pinResponseDTO.setIsDraft(false);
        pinResponseDTO.setIsSponsored(false);
        pinResponseDTO.setSaveCount(10);
        pinResponseDTO.setLikeCount(25);
        pinResponseDTO.setIsLiked(false);
        pinResponseDTO.setIsSaved(false);
        pinResponseDTO.setCreatedBy(userSummary);
        pinResponseDTO.setBoard(boardSummary);
        pinResponseDTO.setCreatedAt(LocalDateTime.now());
        pinResponseDTO.setUpdatedAt(LocalDateTime.now());

        // Setup pin update DTO
        pinUpdateDTO = new PinUpdateDTO();
        pinUpdateDTO.setTitle("Updated Sunset");
        pinUpdateDTO.setDescription("Updated description");
        pinUpdateDTO.setVisibility("PRIVATE");

        // Setup pin draft DTO
        pinDraftDTO = new PinDraftDTO();
        pinDraftDTO.setTitle("Draft Pin");
        pinDraftDTO.setDescription("Draft description");
        pinDraftDTO.setBoardId(BOARD_ID);

        // Setup paginated response
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        paginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(pinResponseDTO),
                pagination
        );
    }

    // ==================== CREATE PIN TESTS ====================

    @Test
    @DisplayName("POST /pins - Success - With Image File")
    void testCreatePin_Success_WithImageFile() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "sunset.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image content".getBytes()
        );

        when(pinService.createPin(eq(USER_ID), any(PinCreationDTO.class), any(MockMultipartFile.class)))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/pins")
                        .file(imageFile)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("title", "Beautiful Sunset")
                        .param("description", "Amazing sunset at the beach")
                        .param("boardId", BOARD_ID)
                        .param("visibility", "PUBLIC"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin created successfully"))
                .andExpect(jsonPath("$.data.pinId").value(PIN_ID))
                .andExpect(jsonPath("$.data.title").value("Beautiful Sunset"))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));

        verify(pinService, times(1)).createPin(eq(USER_ID), any(PinCreationDTO.class), any());
    }

    @Test
    @DisplayName("POST /pins - Success - With Image URL")
    void testCreatePin_Success_WithImageUrl() throws Exception {
        // Arrange
        when(pinService.createPin(eq(USER_ID), any(PinCreationDTO.class), isNull()))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/pins")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("title", "Beautiful Sunset")
                        .param("description", "Amazing sunset at the beach")
                        .param("boardId", BOARD_ID)
                        .param("visibility", "PUBLIC")
                        .param("imageUrl", "https://example.com/sunset.jpg"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.pinId").value(PIN_ID));

        verify(pinService, times(1)).createPin(eq(USER_ID), any(PinCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("POST /pins - Success - With Source URL")
    void testCreatePin_Success_WithSourceUrl() throws Exception {
        // Arrange
        when(pinService.createPin(eq(USER_ID), any(PinCreationDTO.class), isNull()))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/pins")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("title", "Beautiful Sunset")
                        .param("boardId", BOARD_ID)
                        .param("visibility", "PUBLIC")
                        .param("imageUrl", "https://example.com/sunset.jpg")
                        .param("sourceUrl", "https://source.com/original"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sourceUrl").value("https://source.com/original"));

        verify(pinService, times(1)).createPin(eq(USER_ID), any(PinCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("POST /pins - Success - Private Pin")
    void testCreatePin_Success_PrivatePin() throws Exception {
        // Arrange
        pinResponseDTO.setVisibility("PRIVATE");
        when(pinService.createPin(eq(USER_ID), any(PinCreationDTO.class), isNull()))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/pins")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("title", "Private Sunset")
                        .param("boardId", BOARD_ID)
                        .param("visibility", "PRIVATE")
                        .param("imageUrl", "https://example.com/sunset.jpg"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));

        verify(pinService, times(1)).createPin(eq(USER_ID), any(PinCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("POST /pins - Failure - Missing User ID Header")
    void testCreatePin_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/pins")
                        .param("title", "Beautiful Sunset")
                        .param("boardId", BOARD_ID)
                        .param("visibility", "PUBLIC")
                        .param("imageUrl", "https://example.com/sunset.jpg"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).createPin(anyString(), any(), any());
    }

    @Test
    @DisplayName("POST /pins - Failure - Board Not Found")
    void testCreatePin_Failure_BoardNotFound() throws Exception {
        // Arrange
        when(pinService.createPin(eq(USER_ID), any(PinCreationDTO.class), isNull()))
                .thenThrow(new ResourceNotFoundException("Board not found"));

        // Act & Assert
        mockMvc.perform(multipart("/pins")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("title", "Beautiful Sunset")
                        .param("boardId", "invalid-board")
                        .param("visibility", "PUBLIC")
                        .param("imageUrl", "https://example.com/sunset.jpg"))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinService, times(1)).createPin(eq(USER_ID), any(PinCreationDTO.class), isNull());
    }

    // ==================== CREATE PIN DRAFT TESTS ====================

    @Test
    @DisplayName("POST /pins/draft - Success")
    void testCreatePinDraft_Success() throws Exception {
        // Arrange
        pinResponseDTO.setIsDraft(true);
        when(pinService.createPinDraft(eq(USER_ID), any(PinDraftDTO.class)))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/pins/draft")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinDraftDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Draft saved successfully"))
                .andExpect(jsonPath("$.data.isDraft").value(true))
                .andExpect(jsonPath("$.data.title").value("Beautiful Sunset"));

        verify(pinService, times(1)).createPinDraft(eq(USER_ID), any(PinDraftDTO.class));
    }

    @Test
    @DisplayName("POST /pins/draft - Validation - Title Too Long")
    void testCreatePinDraft_Validation_TitleTooLong() throws Exception {
        // Arrange
        pinDraftDTO.setTitle("a".repeat(201));

        // Act & Assert
        mockMvc.perform(post("/pins/draft")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinDraftDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).createPinDraft(anyString(), any());
    }

    @Test
    @DisplayName("POST /pins/draft - Validation - Empty Title")
    void testCreatePinDraft_Validation_EmptyTitle() throws Exception {
        // Arrange
        pinDraftDTO.setTitle("");

        // Act & Assert
        mockMvc.perform(post("/pins/draft")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinDraftDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).createPinDraft(anyString(), any());
    }

    @Test
    @DisplayName("POST /pins/draft - Validation - Description Too Long")
    void testCreatePinDraft_Validation_DescriptionTooLong() throws Exception {
        // Arrange
        pinDraftDTO.setDescription("a".repeat(501));

        // Act & Assert
        mockMvc.perform(post("/pins/draft")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinDraftDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).createPinDraft(anyString(), any());
    }

    @Test
    @DisplayName("POST /pins/draft - Validation - Empty Board ID")
    void testCreatePinDraft_Validation_EmptyBoardId() throws Exception {
        // Arrange
        pinDraftDTO.setBoardId("");

        // Act & Assert
        mockMvc.perform(post("/pins/draft")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinDraftDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).createPinDraft(anyString(), any());
    }

    // ==================== UPDATE PIN TESTS ====================

    @Test
    @DisplayName("PUT /pins/{pinId} - Success")
    void testUpdatePin_Success() throws Exception {
        // Arrange
        pinResponseDTO.setTitle("Updated Sunset");
        pinResponseDTO.setDescription("Updated description");
        when(pinService.updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class)))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinUpdateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated Sunset"));

        verify(pinService, times(1)).updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /pins/{pinId} - Success - Update Title Only")
    void testUpdatePin_Success_TitleOnly() throws Exception {
        // Arrange
        PinUpdateDTO titleOnlyDTO = new PinUpdateDTO();
        titleOnlyDTO.setTitle("New Title");
        when(pinService.updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class)))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(titleOnlyDTO)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /pins/{pinId} - Success - Change Visibility")
    void testUpdatePin_Success_ChangeVisibility() throws Exception {
        // Arrange
        PinUpdateDTO visibilityDTO = new PinUpdateDTO();
        visibilityDTO.setVisibility("PRIVATE");
        pinResponseDTO.setVisibility("PRIVATE");
        when(pinService.updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class)))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visibilityDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));

        verify(pinService, times(1)).updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /pins/{pinId} - Validation - Title Too Long")
    void testUpdatePin_Validation_TitleTooLong() throws Exception {
        // Arrange
        pinUpdateDTO.setTitle("a".repeat(201));

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).updatePin(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /pins/{pinId} - Validation - Description Too Long")
    void testUpdatePin_Validation_DescriptionTooLong() throws Exception {
        // Arrange
        pinUpdateDTO.setDescription("a".repeat(501));

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).updatePin(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /pins/{pinId} - Validation - Invalid Visibility")
    void testUpdatePin_Validation_InvalidVisibility() throws Exception {
        // Arrange
        pinUpdateDTO.setVisibility("INVALID");

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).updatePin(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /pins/{pinId} - Failure - Pin Not Found")
    void testUpdatePin_Failure_PinNotFound() throws Exception {
        // Arrange
        when(pinService.updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class)))
                .thenThrow(new PinNotFoundException("Pin not found"));

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinUpdateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinService, times(1)).updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /pins/{pinId} - Failure - Unauthorized User")
    void testUpdatePin_Failure_UnauthorizedUser() throws Exception {
        // Arrange
        when(pinService.updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class)))
                .thenThrow(new RuntimeException("Unauthorized to update this pin"));

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinUpdateDTO)))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(pinService, times(1)).updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class));
    }

    // ==================== DELETE PIN TESTS ====================

    @Test
    @DisplayName("DELETE /pins/{pinId} - Success")
    void testDeletePin_Success() throws Exception {
        // Arrange
        doNothing().when(pinService).deletePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin deleted successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(pinService, times(1)).deletePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId} - Failure - Pin Not Found")
    void testDeletePin_Failure_PinNotFound() throws Exception {
        // Arrange
        doThrow(new PinNotFoundException("Pin not found"))
                .when(pinService).deletePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinService, times(1)).deletePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId} - Failure - Unauthorized User")
    void testDeletePin_Failure_UnauthorizedUser() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Unauthorized to delete this pin"))
                .when(pinService).deletePin(USER_ID, PIN_ID);

        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(pinService, times(1)).deletePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("DELETE /pins/{pinId} - Failure - Missing User ID Header")
    void testDeletePin_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/pins/{pinId}", PIN_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).deletePin(anyString(), anyString());
    }

    // ==================== GET PIN BY ID TESTS ====================

    @Test
    @DisplayName("GET /pins/{pinId} - Success - With User ID")
    void testGetPinById_Success_WithUserId() throws Exception {
        // Arrange
        when(pinService.getPinById(PIN_ID, USER_ID))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin retrieved successfully"))
                .andExpect(jsonPath("$.data.pinId").value(PIN_ID))
                .andExpect(jsonPath("$.data.title").value("Beautiful Sunset"))
                .andExpect(jsonPath("$.data.createdBy.username").value("testuser"))
                .andExpect(jsonPath("$.data.board.name").value("Travel Board"));

        verify(pinService, times(1)).getPinById(PIN_ID, USER_ID);
    }

    @Test
    @DisplayName("GET /pins/{pinId} - Success - Without User ID (Anonymous)")
    void testGetPinById_Success_WithoutUserId() throws Exception {
        // Arrange
        when(pinService.getPinById(PIN_ID, null))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}", PIN_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.pinId").value(PIN_ID));

        verify(pinService, times(1)).getPinById(PIN_ID, null);
    }

    @Test
    @DisplayName("GET /pins/{pinId} - Failure - Pin Not Found")
    void testGetPinById_Failure_PinNotFound() throws Exception {
        // Arrange
        when(pinService.getPinById(PIN_ID, USER_ID))
                .thenThrow(new PinNotFoundException("Pin not found"));

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinService, times(1)).getPinById(PIN_ID, USER_ID);
    }

    @Test
    @DisplayName("GET /pins/{pinId} - Success - With Like and Save Status")
    void testGetPinById_Success_WithLikeAndSaveStatus() throws Exception {
        // Arrange
        pinResponseDTO.setIsLiked(true);
        pinResponseDTO.setIsSaved(true);
        when(pinService.getPinById(PIN_ID, USER_ID))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.isSaved").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(25))
                .andExpect(jsonPath("$.data.saveCount").value(10));

        verify(pinService, times(1)).getPinById(PIN_ID, USER_ID);
    }

    // ==================== GET USER PINS TESTS ====================

    @Test
    @DisplayName("GET /pins/user/{userId} - Success")
    void testGetUserPins_Success() throws Exception {
        // Arrange
        when(pinService.getUserPins(USER_ID, USER_ID, 0, 20, null))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/user/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pins retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].pinId").value(PIN_ID));

        verify(pinService, times(1)).getUserPins(USER_ID, USER_ID, 0, 20, null);
    }

    @Test
    @DisplayName("GET /pins/user/{userId} - Success - Custom Pagination")
    void testGetUserPins_Success_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(2, 5, 100L, 10, true, true);
        PaginatedResponse<PinResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(pinResponseDTO),
                customPagination
        );
        when(pinService.getUserPins(USER_ID, USER_ID, 2, 10, null))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/user/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "2")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(5));

        verify(pinService, times(1)).getUserPins(USER_ID, USER_ID, 2, 10, null);
    }

    @Test
    @DisplayName("GET /pins/user/{userId} - Success - With Sort Parameter")
    void testGetUserPins_Success_WithSort() throws Exception {
        // Arrange
        when(pinService.getUserPins(USER_ID, USER_ID, 0, 20, "createdAt"))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/user/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("sort", "createdAt"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).getUserPins(USER_ID, USER_ID, 0, 20, "createdAt");
    }

    @Test
    @DisplayName("GET /pins/user/{userId} - Success - Empty List")
    void testGetUserPins_Success_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<PinResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(pinService.getUserPins(USER_ID, USER_ID, 0, 20, null))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/user/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(pinService, times(1)).getUserPins(USER_ID, USER_ID, 0, 20, null);
    }

    @Test
    @DisplayName("GET /pins/user/{userId} - Success - Anonymous Request")
    void testGetUserPins_Success_AnonymousRequest() throws Exception {
        // Arrange
        when(pinService.getUserPins(USER_ID, null, 0, 20, null))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/user/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).getUserPins(USER_ID, null, 0, 20, null);
    }

    // ==================== GET BOARD PINS TESTS ====================

    @Test
    @DisplayName("GET /pins/board/{boardId} - Success")
    void testGetBoardPins_Success() throws Exception {
        // Arrange
        when(pinService.getBoardPins(BOARD_ID, USER_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/board/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Board pins retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)));

        verify(pinService, times(1)).getBoardPins(BOARD_ID, USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/board/{boardId} - Success - Custom Pagination")
    void testGetBoardPins_Success_CustomPagination() throws Exception {
        // Arrange
        when(pinService.getBoardPins(BOARD_ID, USER_ID, 1, 10))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/board/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).getBoardPins(BOARD_ID, USER_ID, 1, 10);
    }

    @Test
    @DisplayName("GET /pins/board/{boardId} - Success - Anonymous Request")
    void testGetBoardPins_Success_AnonymousRequest() throws Exception {
        // Arrange
        when(pinService.getBoardPins(BOARD_ID, null, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/board/{boardId}", BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).getBoardPins(BOARD_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/board/{boardId} - Failure - Board Not Found")
    void testGetBoardPins_Failure_BoardNotFound() throws Exception {
        // Arrange
        when(pinService.getBoardPins(BOARD_ID, USER_ID, 0, 20))
                .thenThrow(new ResourceNotFoundException("Board not found"));

        // Act & Assert
        mockMvc.perform(get("/pins/board/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(pinService, times(1)).getBoardPins(BOARD_ID, USER_ID, 0, 20);
    }

    // ==================== GET USER DRAFTS TESTS ====================

    @Test
    @DisplayName("GET /pins/drafts - Success")
    void testGetUserDrafts_Success() throws Exception {
        // Arrange
        pinResponseDTO.setIsDraft(true);
        when(pinService.getUserDrafts(USER_ID, USER_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/drafts")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Draft pins retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].isDraft").value(true));

        verify(pinService, times(1)).getUserDrafts(USER_ID, USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/drafts - Success - Custom Pagination")
    void testGetUserDrafts_Success_CustomPagination() throws Exception {
        // Arrange
        when(pinService.getUserDrafts(USER_ID, USER_ID, 1, 15))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/drafts")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "1")
                        .param("size", "15"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).getUserDrafts(USER_ID, USER_ID, 1, 15);
    }

    @Test
    @DisplayName("GET /pins/drafts - Success - Empty List")
    void testGetUserDrafts_Success_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<PinResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(pinService.getUserDrafts(USER_ID, USER_ID, 0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/drafts")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(pinService, times(1)).getUserDrafts(USER_ID, USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /pins/drafts - Failure - Missing User ID Header")
    void testGetUserDrafts_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pins/drafts"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).getUserDrafts(anyString(), anyString(), anyInt(), anyInt());
    }

    // ==================== GET PUBLIC PINS TESTS ====================

    @Test
    @DisplayName("GET /pins/public - Success")
    void testGetPublicPins_Success() throws Exception {
        // Arrange
        when(pinService.getPublicPins(USER_ID, 0, 10))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/public")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].pinId").value(PIN_ID));

        verify(pinService, times(1)).getPublicPins(USER_ID, 0, 10);
    }

    @Test
    @DisplayName("GET /pins/public - Success - Anonymous Request")
    void testGetPublicPins_Success_AnonymousRequest() throws Exception {
        // Arrange
        when(pinService.getPublicPins(null, 0, 10))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/public"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).getPublicPins(null, 0, 10);
    }

    @Test
    @DisplayName("GET /pins/public - Success - Custom Pagination")
    void testGetPublicPins_Success_CustomPagination() throws Exception {
        // Arrange
        when(pinService.getPublicPins(USER_ID, 2, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/public")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "2")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).getPublicPins(USER_ID, 2, 20);
    }

    @Test
    @DisplayName("GET /pins/public - Success - Empty List")
    void testGetPublicPins_Success_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 10, false, false);
        PaginatedResponse<PinResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(pinService.getPublicPins(null, 0, 10))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/public"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(pinService, times(1)).getPublicPins(null, 0, 10);
    }

    // ==================== SEARCH PINS TESTS ====================

    @Test
    @DisplayName("GET /pins/search - Success")
    void testSearchPins_Success() throws Exception {
        // Arrange
        when(pinService.searchPins("sunset", 0, 10))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/search")
                        .param("keyword", "sunset"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].pinId").value(PIN_ID));

        verify(pinService, times(1)).searchPins("sunset", 0, 10);
    }

    @Test
    @DisplayName("GET /pins/search - Success - Custom Pagination")
    void testSearchPins_Success_CustomPagination() throws Exception {
        // Arrange
        when(pinService.searchPins("beach", 1, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/search")
                        .param("keyword", "beach")
                        .param("page", "1")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).searchPins("beach", 1, 20);
    }

    @Test
    @DisplayName("GET /pins/search - Success - Empty Results")
    void testSearchPins_Success_EmptyResults() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 10, false, false);
        PaginatedResponse<PinResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(pinService.searchPins("nonexistent", 0, 10))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/search")
                        .param("keyword", "nonexistent"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(pinService, times(1)).searchPins("nonexistent", 0, 10);
    }

    @Test
    @DisplayName("GET /pins/search - Failure - Missing Keyword")
    void testSearchPins_Failure_MissingKeyword() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pins/search"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).searchPins(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /pins/search - Failure - Empty Keyword")
    void testSearchPins_Failure_EmptyKeyword() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pins/search")
                        .param("keyword", ""))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).searchPins(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /pins/search - Failure - Whitespace Only Keyword")
    void testSearchPins_Failure_WhitespaceKeyword() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pins/search")
                        .param("keyword", "   "))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(pinService, never()).searchPins(anyString(), anyInt(), anyInt());
    }

    // ==================== INTEGRATION & EDGE CASE TESTS ====================

    @Test
    @DisplayName("Complete Pin Lifecycle - Create, Update, Get, Delete")
    void testCompletePinLifecycle() throws Exception {
        // Arrange
        when(pinService.createPin(eq(USER_ID), any(PinCreationDTO.class), isNull()))
                .thenReturn(pinResponseDTO);
        when(pinService.getPinById(PIN_ID, USER_ID))
                .thenReturn(pinResponseDTO);
        when(pinService.updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class)))
                .thenReturn(pinResponseDTO);
        doNothing().when(pinService).deletePin(USER_ID, PIN_ID);

        // Create
        mockMvc.perform(multipart("/pins")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("title", "Beautiful Sunset")
                        .param("boardId", BOARD_ID)
                        .param("visibility", "PUBLIC")
                        .param("imageUrl", "https://example.com/sunset.jpg"))
                .andExpect(status().isCreated());

        // Get
        mockMvc.perform(get("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Update
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinUpdateDTO)))
                .andExpect(status().isOk());

        // Delete
        mockMvc.perform(delete("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        verify(pinService, times(1)).createPin(eq(USER_ID), any(PinCreationDTO.class), isNull());
        verify(pinService, times(1)).getPinById(PIN_ID, USER_ID);
        verify(pinService, times(1)).updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class));
        verify(pinService, times(1)).deletePin(USER_ID, PIN_ID);
    }

    @Test
    @DisplayName("Draft Workflow - Create Draft, Get Drafts")
    void testDraftWorkflow() throws Exception {
        // Arrange
        pinResponseDTO.setIsDraft(true);
        when(pinService.createPinDraft(eq(USER_ID), any(PinDraftDTO.class)))
                .thenReturn(pinResponseDTO);
        when(pinService.getUserDrafts(USER_ID, USER_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Create Draft
        mockMvc.perform(post("/pins/draft")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinDraftDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isDraft").value(true));

        // Get Drafts
        mockMvc.perform(get("/pins/drafts")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].isDraft").value(true));

        verify(pinService, times(1)).createPinDraft(eq(USER_ID), any(PinDraftDTO.class));
        verify(pinService, times(1)).getUserDrafts(USER_ID, USER_ID, 0, 20);
    }

    @Test
    @DisplayName("Multiple Pins - Get User Pins with Multiple Results")
    void testMultiplePins() throws Exception {
        // Arrange
        PinResponseDTO pin2 = new PinResponseDTO();
        pin2.setPinId("pin-999");
        pin2.setTitle("Mountain View");

        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<PinResponseDTO> multipleResponse = new PaginatedResponse<>(
                Arrays.asList(pinResponseDTO, pin2),
                pagination
        );
        when(pinService.getUserPins(USER_ID, USER_ID, 0, 20, null))
                .thenReturn(multipleResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/user/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].pinId").value(PIN_ID))
                .andExpect(jsonPath("$.data.data[1].pinId").value("pin-999"));

        verify(pinService, times(1)).getUserPins(USER_ID, USER_ID, 0, 20, null);
    }

    @Test
    @DisplayName("Pagination Boundary Test - Last Page")
    void testPaginationBoundary_LastPage() throws Exception {
        // Arrange
        PaginationDTO lastPagePagination = new PaginationDTO(4, 5, 100L, 20, false, true);
        PaginatedResponse<PinResponseDTO> lastPageResponse = new PaginatedResponse<>(
                Collections.singletonList(pinResponseDTO),
                lastPagePagination
        );
        when(pinService.getUserPins(USER_ID, USER_ID, 4, 20, null))
                .thenReturn(lastPageResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/user/{userId}", USER_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "4"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(4))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false))
                .andExpect(jsonPath("$.data.pagination.hasPrevious").value(true));

        verify(pinService, times(1)).getUserPins(USER_ID, USER_ID, 4, 20, null);
    }

    @Test
    @DisplayName("Edge Case - Search with Special Characters")
    void testSearchPins_SpecialCharacters() throws Exception {
        // Arrange
        when(pinService.searchPins("coffee & tea", 0, 10))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/pins/search")
                        .param("keyword", "coffee & tea"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).searchPins("coffee & tea", 0, 10);
    }

    @Test
    @DisplayName("Visibility Change - Public to Private")
    void testVisibilityChange_PublicToPrivate() throws Exception {
        // Arrange
        PinUpdateDTO visibilityUpdate = new PinUpdateDTO();
        visibilityUpdate.setVisibility("PRIVATE");
        pinResponseDTO.setVisibility("PRIVATE");
        
        when(pinService.updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class)))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visibilityUpdate)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));

        verify(pinService, times(1)).updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class));
    }

    @Test
    @DisplayName("Pin Engagement - Like and Save Counts")
    void testPinEngagement() throws Exception {
        // Arrange
        pinResponseDTO.setLikeCount(100);
        pinResponseDTO.setSaveCount(50);
        pinResponseDTO.setIsLiked(true);
        pinResponseDTO.setIsSaved(true);
        
        when(pinService.getPinById(PIN_ID, USER_ID))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(100))
                .andExpect(jsonPath("$.data.saveCount").value(50))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.isSaved").value(true));

        verify(pinService, times(1)).getPinById(PIN_ID, USER_ID);
    }

    @Test
    @DisplayName("Sponsored Pin - Is Sponsored Flag")
    void testSponsoredPin() throws Exception {
        // Arrange
        pinResponseDTO.setIsSponsored(true);
        when(pinService.getPinById(PIN_ID, USER_ID))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isSponsored").value(true));

        verify(pinService, times(1)).getPinById(PIN_ID, USER_ID);
    }

    @Test
    @DisplayName("Edge Case - Update with All Fields")
    void testUpdatePin_AllFields() throws Exception {
        // Arrange
        PinUpdateDTO completeUpdate = new PinUpdateDTO();
        completeUpdate.setTitle("New Title");
        completeUpdate.setDescription("New Description");
        completeUpdate.setBoardId("new-board-123");
        completeUpdate.setVisibility("PRIVATE");
        
        when(pinService.updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class)))
                .thenReturn(pinResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/pins/{pinId}", PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeUpdate)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pinService, times(1)).updatePin(eq(USER_ID), eq(PIN_ID), any(PinUpdateDTO.class));
    }
}
