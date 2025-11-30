package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.BoardController;
import com.infy.pinterest.dto.*;
import com.infy.pinterest.exception.BoardNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.service.BoardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

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
@WebMvcTest(BoardController.class)
@AutoConfigureMockMvc(addFilters = false)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private BoardService boardService;

    private static final String USER_ID = "user-123";
    private static final String BOARD_ID = "board-456";
    private static final String USER_ID_HEADER = "X-User-Id";

    private BoardResponseDTO boardResponseDTO;
    private BoardUpdateDTO boardUpdateDTO;
    private PaginatedResponse<BoardResponseDTO> paginatedResponse;

    @BeforeEach
    void setUp() {
        // Setup board response DTO
        boardResponseDTO = new BoardResponseDTO();
        boardResponseDTO.setBoardId(BOARD_ID);
        boardResponseDTO.setUserId(USER_ID);
        boardResponseDTO.setName("Test Board");
        boardResponseDTO.setDescription("Test Description");
        boardResponseDTO.setCategory("Travel");
        boardResponseDTO.setCoverImageUrl("https://example.com/cover.jpg");
        boardResponseDTO.setVisibility("PUBLIC");
        boardResponseDTO.setIsCollaborative(false);
        boardResponseDTO.setPinCount(10);
        boardResponseDTO.setCreatedAt(LocalDateTime.now());
        boardResponseDTO.setUpdatedAt(LocalDateTime.now());

        // Setup board update DTO
        boardUpdateDTO = new BoardUpdateDTO();
        boardUpdateDTO.setName("Updated Board");
        boardUpdateDTO.setDescription("Updated Description");
        boardUpdateDTO.setVisibility("PRIVATE");

        // Setup paginated response
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        paginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(boardResponseDTO),
                pagination
        );
    }

    // ==================== CREATE BOARD TESTS ====================

    @Test
    @DisplayName("POST /boards - Success - With Banner Image File")
    void testCreateBoard_WithBannerImageFile() throws Exception {
        // Arrange
        MockMultipartFile bannerImage = new MockMultipartFile(
                "bannerImage",
                "banner.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "banner content".getBytes()
        );

        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .file(bannerImage)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Test Board")
                        .param("description", "Test Description")
                        .param("category", "Travel")
                        .param("visibility", "PUBLIC"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Board created successfully"))
                .andExpect(jsonPath("$.data.boardId").value(BOARD_ID))
                .andExpect(jsonPath("$.data.name").value("Test Board"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("POST /boards - Success - With Banner Image URL")
    void testCreateBoard_WithBannerImageUrl() throws Exception {
        // Arrange
        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Test Board")
                        .param("description", "Test Description")
                        .param("category", "Travel")
                        .param("visibility", "PUBLIC")
                        .param("bannerImageUrl", "https://example.com/banner.jpg"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.coverImageUrl").value("https://example.com/cover.jpg"));

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("POST /boards - Success - Without Banner Image")
    void testCreateBoard_WithoutBannerImage() throws Exception {
        // Arrange
        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Test Board")
                        .param("visibility", "PUBLIC"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.boardId").value(BOARD_ID));

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("POST /boards - Success - Private Board")
    void testCreateBoard_PrivateVisibility() throws Exception {
        // Arrange
        BoardResponseDTO privateBoardResponse = new BoardResponseDTO();
        privateBoardResponse.setBoardId(BOARD_ID);
        privateBoardResponse.setVisibility("PRIVATE");
        
        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(privateBoardResponse);

        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Private Board")
                        .param("visibility", "PRIVATE"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("POST /boards - Failure - User Not Found")
    void testCreateBoard_UserNotFound() throws Exception {
        // Arrange
        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenThrow(new ResourceNotFoundException("User not found with ID: " + USER_ID));

        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Test Board")
                        .param("visibility", "PUBLIC"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User not found with ID: " + USER_ID));

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("POST /boards - Failure - Missing Required Name")
    void testCreateBoard_MissingName() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("visibility", "PUBLIC"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(boardService, never()).createBoard(anyString(), any(BoardCreationDTO.class), any(MultipartFile.class));
    }

    @Test
    @DisplayName("POST /boards - Failure - Missing Required Visibility")
    void testCreateBoard_MissingVisibility() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Test Board"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(boardService, never()).createBoard(anyString(), any(BoardCreationDTO.class), any(MultipartFile.class));
    }

    @Test
    @DisplayName("POST /boards - Failure - Missing User ID Header")
    void testCreateBoard_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .param("name", "Test Board")
                        .param("visibility", "PUBLIC"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(boardService, never()).createBoard(anyString(), any(BoardCreationDTO.class), any(MultipartFile.class));
    }

    @Test
    @DisplayName("POST /boards - Success - With All Optional Fields")
    void testCreateBoard_WithAllFields() throws Exception {
        // Arrange
        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Test Board")
                        .param("description", "Complete board description")
                        .param("category", "Travel")
                        .param("visibility", "PUBLIC")
                        .param("bannerImageUrl", "https://example.com/banner.jpg"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"));

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    // ==================== UPDATE BOARD TESTS ====================

    @Test
    @DisplayName("PUT /boards/{boardId} - Success")
    void testUpdateBoard_Success() throws Exception {
        // Arrange
        when(boardService.updateBoard(USER_ID, BOARD_ID, boardUpdateDTO))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardUpdateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Board updated successfully"))
                .andExpect(jsonPath("$.data.boardId").value(BOARD_ID))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(boardService, times(1)).updateBoard(USER_ID, BOARD_ID, boardUpdateDTO);
    }

    @Test
    @DisplayName("PUT /boards/{boardId} - Failure - Board Not Found")
    void testUpdateBoard_BoardNotFound() throws Exception {
        // Arrange
        when(boardService.updateBoard(USER_ID, BOARD_ID, boardUpdateDTO))
                .thenThrow(new BoardNotFoundException("Board not found or you don't have permission to edit it"));

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardUpdateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Board not found or you don't have permission to edit it"));

        verify(boardService, times(1)).updateBoard(USER_ID, BOARD_ID, boardUpdateDTO);
    }

    @Test
    @DisplayName("PUT /boards/{boardId} - Failure - Unauthorized User")
    void testUpdateBoard_UnauthorizedUser() throws Exception {
        // Arrange
        String unauthorizedUserId = "other-user";
        when(boardService.updateBoard(unauthorizedUserId, BOARD_ID, boardUpdateDTO))
                .thenThrow(new BoardNotFoundException("Board not found or you don't have permission to edit it"));

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, unauthorizedUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardUpdateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));

        verify(boardService, times(1)).updateBoard(unauthorizedUserId, BOARD_ID, boardUpdateDTO);
    }

    @Test
    @DisplayName("PUT /boards/{boardId} - Validation - Name Too Long")
    void testUpdateBoard_NameTooLong() throws Exception {
        // Arrange
        boardUpdateDTO.setName("a".repeat(101));

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(boardService, never()).updateBoard(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /boards/{boardId} - Validation - Description Too Long")
    void testUpdateBoard_DescriptionTooLong() throws Exception {
        // Arrange
        boardUpdateDTO.setDescription("a".repeat(201));

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(boardService, never()).updateBoard(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /boards/{boardId} - Validation - Invalid Visibility")
    void testUpdateBoard_InvalidVisibility() throws Exception {
        // Arrange
        boardUpdateDTO.setVisibility("INVALID");

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(boardService, never()).updateBoard(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /boards/{boardId} - Success - Partial Update (Only Name)")
    void testUpdateBoard_PartialUpdateName() throws Exception {
        // Arrange
        BoardUpdateDTO partialUpdate = new BoardUpdateDTO();
        partialUpdate.setName("Only Name Updated");
        
        when(boardService.updateBoard(USER_ID, BOARD_ID, partialUpdate))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdate)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(boardService, times(1)).updateBoard(USER_ID, BOARD_ID, partialUpdate);
    }

    @Test
    @DisplayName("PUT /boards/{boardId} - Success - Partial Update (Only Visibility)")
    void testUpdateBoard_PartialUpdateVisibility() throws Exception {
        // Arrange
        BoardUpdateDTO partialUpdate = new BoardUpdateDTO();
        partialUpdate.setVisibility("PRIVATE");
        
        when(boardService.updateBoard(USER_ID, BOARD_ID, partialUpdate))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdate)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(boardService, times(1)).updateBoard(USER_ID, BOARD_ID, partialUpdate);
    }

    // ==================== DELETE BOARD TESTS ====================

    @Test
    @DisplayName("DELETE /boards/{boardId} - Success")
    void testDeleteBoard_Success() throws Exception {
        // Arrange
        doNothing().when(boardService).deleteBoard(USER_ID, BOARD_ID);

        // Act & Assert
        mockMvc.perform(delete("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Board deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(boardService, times(1)).deleteBoard(USER_ID, BOARD_ID);
    }

    @Test
    @DisplayName("DELETE /boards/{boardId} - Failure - Board Not Found")
    void testDeleteBoard_BoardNotFound() throws Exception {
        // Arrange
        doThrow(new BoardNotFoundException("Board not found or you don't have permission to delete it"))
                .when(boardService).deleteBoard(USER_ID, BOARD_ID);

        // Act & Assert
        mockMvc.perform(delete("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Board not found or you don't have permission to delete it"));

        verify(boardService, times(1)).deleteBoard(USER_ID, BOARD_ID);
    }

    @Test
    @DisplayName("DELETE /boards/{boardId} - Failure - Unauthorized User")
    void testDeleteBoard_UnauthorizedUser() throws Exception {
        // Arrange
        String unauthorizedUserId = "other-user";
        doThrow(new BoardNotFoundException("Board not found or you don't have permission to delete it"))
                .when(boardService).deleteBoard(unauthorizedUserId, BOARD_ID);

        // Act & Assert
        mockMvc.perform(delete("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, unauthorizedUserId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));

        verify(boardService, times(1)).deleteBoard(unauthorizedUserId, BOARD_ID);
    }

    @Test
    @DisplayName("DELETE /boards/{boardId} - Failure - Missing User ID Header")
    void testDeleteBoard_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/boards/{boardId}", BOARD_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(boardService, never()).deleteBoard(anyString(), anyString());
    }

    // ==================== GET BOARD BY ID TESTS ====================

    @Test
    @DisplayName("GET /boards/{boardId} - Success")
    void testGetBoardById_Success() throws Exception {
        // Arrange
        when(boardService.getBoardById(BOARD_ID)).thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/boards/{boardId}", BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Board retrieved successfully"))
                .andExpect(jsonPath("$.data.boardId").value(BOARD_ID))
                .andExpect(jsonPath("$.data.name").value("Test Board"))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(boardService, times(1)).getBoardById(BOARD_ID);
    }

    @Test
    @DisplayName("GET /boards/{boardId} - Failure - Board Not Found")
    void testGetBoardById_BoardNotFound() throws Exception {
        // Arrange
        when(boardService.getBoardById(BOARD_ID))
                .thenThrow(new BoardNotFoundException("Board not found with ID: " + BOARD_ID));

        // Act & Assert
        mockMvc.perform(get("/boards/{boardId}", BOARD_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Board not found with ID: " + BOARD_ID));

        verify(boardService, times(1)).getBoardById(BOARD_ID);
    }

    @Test
    @DisplayName("GET /boards/{boardId} - Success - Public Board Without Authentication")
    void testGetBoardById_PublicBoardNoAuth() throws Exception {
        // Arrange
        when(boardService.getBoardById(BOARD_ID)).thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/boards/{boardId}", BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));

        verify(boardService, times(1)).getBoardById(BOARD_ID);
    }

    // ==================== GET USER BOARDS TESTS ====================

    @Test
    @DisplayName("GET /boards/user/{userId} - Success - Default Pagination")
    void testGetUserBoards_DefaultPagination() throws Exception {
        // Arrange
        when(boardService.getUserBoards(USER_ID, 0, 20, null))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/user/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Boards retrieved successfully"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(1))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(boardService, times(1)).getUserBoards(USER_ID, 0, 20, null);
    }

    @Test
    @DisplayName("GET /boards/user/{userId} - Success - Custom Pagination")
    void testGetUserBoards_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(2, 5, 50L, 10, true, true);
        PaginatedResponse<BoardResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(boardResponseDTO),
                customPagination
        );
        when(boardService.getUserBoards(USER_ID, 2, 10, "name"))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/user/{userId}", USER_ID)
                        .param("page", "2")
                        .param("size", "10")
                        .param("sort", "name"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(5))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(50));

        verify(boardService, times(1)).getUserBoards(USER_ID, 2, 10, "name");
    }

    @Test
    @DisplayName("GET /boards/user/{userId} - Success - Empty List")
    void testGetUserBoards_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<BoardResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(boardService.getUserBoards(USER_ID, 0, 20, null))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/user/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(0)))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(0));

        verify(boardService, times(1)).getUserBoards(USER_ID, 0, 20, null);
    }

    @Test
    @DisplayName("GET /boards/user/{userId} - Success - Multiple Boards")
    void testGetUserBoards_MultipleBoards() throws Exception {
        // Arrange
        BoardResponseDTO board2 = new BoardResponseDTO();
        board2.setBoardId("board-789");
        board2.setName("Second Board");

        List<BoardResponseDTO> boards = Arrays.asList(boardResponseDTO, board2);
        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<BoardResponseDTO> multipleResponse = new PaginatedResponse<>(boards, pagination);

        when(boardService.getUserBoards(USER_ID, 0, 20, null))
                .thenReturn(multipleResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/user/{userId}", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].boardId").value(BOARD_ID))
                .andExpect(jsonPath("$.data.data[1].boardId").value("board-789"));

        verify(boardService, times(1)).getUserBoards(USER_ID, 0, 20, null);
    }

    @Test
    @DisplayName("GET /boards/user/{userId} - Success - Sort By Created Date")
    void testGetUserBoards_SortByCreatedAt() throws Exception {
        // Arrange
        when(boardService.getUserBoards(USER_ID, 0, 20, "createdAt"))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/user/{userId}", USER_ID)
                        .param("sort", "createdAt"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(boardService, times(1)).getUserBoards(USER_ID, 0, 20, "createdAt");
    }

    // ==================== GET ALL PUBLIC BOARDS TESTS ====================

    @Test
    @DisplayName("GET /boards - Success - Default Pagination")
    void testGetAllPublicBoards_DefaultPagination() throws Exception {
        // Arrange
        when(boardService.getAllPublicBoards(0, 50, null))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/boards"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Public boards retrieved successfully"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(boardService, times(1)).getAllPublicBoards(0, 50, null);
    }

    @Test
    @DisplayName("GET /boards - Success - Custom Pagination")
    void testGetAllPublicBoards_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(1, 10, 100L, 10, true, true);
        PaginatedResponse<BoardResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(boardResponseDTO),
                customPagination
        );
        when(boardService.getAllPublicBoards(1, 10, "name"))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/boards")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "name"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.data.pagination.pageSize").value(10));

        verify(boardService, times(1)).getAllPublicBoards(1, 10, "name");
    }

    @Test
    @DisplayName("GET /boards - Success - Empty List")
    void testGetAllPublicBoards_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 50, false, false);
        PaginatedResponse<BoardResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(boardService.getAllPublicBoards(0, 50, null))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/boards"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(boardService, times(1)).getAllPublicBoards(0, 50, null);
    }

    @Test
    @DisplayName("GET /boards - Success - Large Page Size")
    void testGetAllPublicBoards_LargePageSize() throws Exception {
        // Arrange
        when(boardService.getAllPublicBoards(0, 100, null))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/boards")
                        .param("size", "100"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(boardService, times(1)).getAllPublicBoards(0, 100, null);
    }

    // ==================== GET COLLABORATIVE BOARDS TESTS ====================

    @Test
    @DisplayName("GET /boards/collaborative - Success - Default Pagination")
    void testGetCollaborativeBoards_DefaultPagination() throws Exception {
        // Arrange
        when(boardService.getCollaborativeBoards(USER_ID, 0, 20, null))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/collaborative")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Collaborative boards retrieved successfully"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(boardService, times(1)).getCollaborativeBoards(USER_ID, 0, 20, null);
    }

    @Test
    @DisplayName("GET /boards/collaborative - Success - Custom Pagination")
    void testGetCollaborativeBoards_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(1, 3, 25L, 10, true, true);
        PaginatedResponse<BoardResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(boardResponseDTO),
                customPagination
        );
        when(boardService.getCollaborativeBoards(USER_ID, 1, 10, "name"))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/collaborative")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "name"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(3));

        verify(boardService, times(1)).getCollaborativeBoards(USER_ID, 1, 10, "name");
    }

    @Test
    @DisplayName("GET /boards/collaborative - Success - Empty List (No Collaborations)")
    void testGetCollaborativeBoards_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<BoardResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(boardService.getCollaborativeBoards(USER_ID, 0, 20, null))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/collaborative")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(0)))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(0));

        verify(boardService, times(1)).getCollaborativeBoards(USER_ID, 0, 20, null);
    }

    @Test
    @DisplayName("GET /boards/collaborative - Failure - Missing User ID Header")
    void testGetCollaborativeBoards_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/boards/collaborative"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(boardService, never()).getCollaborativeBoards(anyString(), anyInt(), anyInt(), anyString());
    }

    @Test
    @DisplayName("GET /boards/collaborative - Success - Multiple Collaborative Boards")
    void testGetCollaborativeBoards_MultipleBoards() throws Exception {
        // Arrange
        BoardResponseDTO collabBoard1 = new BoardResponseDTO();
        collabBoard1.setBoardId("collab-001");
        collabBoard1.setName("Team Board 1");
        collabBoard1.setIsCollaborative(true);

        BoardResponseDTO collabBoard2 = new BoardResponseDTO();
        collabBoard2.setBoardId("collab-002");
        collabBoard2.setName("Team Board 2");
        collabBoard2.setIsCollaborative(true);

        List<BoardResponseDTO> boards = Arrays.asList(collabBoard1, collabBoard2);
        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<BoardResponseDTO> multipleResponse = new PaginatedResponse<>(boards, pagination);

        when(boardService.getCollaborativeBoards(USER_ID, 0, 20, null))
                .thenReturn(multipleResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/collaborative")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].isCollaborative").value(true))
                .andExpect(jsonPath("$.data.data[1].isCollaborative").value(true));

        verify(boardService, times(1)).getCollaborativeBoards(USER_ID, 0, 20, null);
    }

    // ==================== EDGE CASES & INTEGRATION TESTS ====================

    @Test
    @DisplayName("Board Lifecycle - Create, Update, Get, Delete")
    void testBoardLifecycle() throws Exception {
        // Arrange
        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(boardResponseDTO);
        when(boardService.updateBoard(eq(USER_ID), eq(BOARD_ID), any(BoardUpdateDTO.class)))
                .thenReturn(boardResponseDTO);
        when(boardService.getBoardById(BOARD_ID)).thenReturn(boardResponseDTO);
        doNothing().when(boardService).deleteBoard(USER_ID, BOARD_ID);

        // Create
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Test Board")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isCreated());

        // Update
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardUpdateDTO)))
                .andExpect(status().isOk());

        // Get
        mockMvc.perform(get("/boards/{boardId}", BOARD_ID))
                .andExpect(status().isOk());

        // Delete
        mockMvc.perform(delete("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
        verify(boardService, times(1)).updateBoard(eq(USER_ID), eq(BOARD_ID), any(BoardUpdateDTO.class));
        verify(boardService, times(1)).getBoardById(BOARD_ID);
        verify(boardService, times(1)).deleteBoard(USER_ID, BOARD_ID);
    }

    @Test
    @DisplayName("POST /boards - Success - With Category")
    void testCreateBoard_WithCategory() throws Exception {
        // Arrange
        BoardResponseDTO boardWithCategory = new BoardResponseDTO();
        boardWithCategory.setBoardId(BOARD_ID);
        boardWithCategory.setCategory("Fashion");
        
        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(boardWithCategory);

        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Fashion Board")
                        .param("category", "Fashion")
                        .param("visibility", "PUBLIC"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("Fashion"));

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("GET /boards/user/{userId} - Pagination - Has Next and Previous")
    void testGetUserBoards_PaginationNavigation() throws Exception {
        // Arrange
        PaginationDTO navPagination = new PaginationDTO(5, 10, 100L, 10, true, true);
        PaginatedResponse<BoardResponseDTO> navResponse = new PaginatedResponse<>(
                Collections.singletonList(boardResponseDTO),
                navPagination
        );
        when(boardService.getUserBoards(USER_ID, 5, 10, null))
                .thenReturn(navResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/user/{userId}", USER_ID)
                        .param("page", "5")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.hasNext").value(true))
                .andExpect(jsonPath("$.data.pagination.hasPrevious").value(true));

        verify(boardService, times(1)).getUserBoards(USER_ID, 5, 10, null);
    }

    @Test
    @DisplayName("GET /boards - First and Last Page Navigation")
    void testGetAllPublicBoards_FirstLastPage() throws Exception {
        // First page
        PaginationDTO firstPagePagination = new PaginationDTO(0, 5, 50L, 10, true, false);
        PaginatedResponse<BoardResponseDTO> firstPageResponse = new PaginatedResponse<>(
                Collections.singletonList(boardResponseDTO),
                firstPagePagination
        );
        when(boardService.getAllPublicBoards(0, 10, null))
                .thenReturn(firstPageResponse);

        mockMvc.perform(get("/boards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.hasPrevious").value(false))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(true));

        // Last page
        PaginationDTO lastPagePagination = new PaginationDTO(4, 5, 50L, 10, false, true);
        PaginatedResponse<BoardResponseDTO> lastPageResponse = new PaginatedResponse<>(
                Collections.singletonList(boardResponseDTO),
                lastPagePagination
        );
        when(boardService.getAllPublicBoards(4, 10, null))
                .thenReturn(lastPageResponse);

        mockMvc.perform(get("/boards")
                        .param("page", "4")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.hasPrevious").value(true))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false));
    }

    @Test
    @DisplayName("PUT /boards/{boardId} - Update All Fields")
    void testUpdateBoard_AllFields() throws Exception {
        // Arrange
        BoardUpdateDTO fullUpdate = new BoardUpdateDTO();
        fullUpdate.setName("Completely Updated Board");
        fullUpdate.setDescription("Completely new description");
        fullUpdate.setVisibility("PRIVATE");

        when(boardService.updateBoard(USER_ID, BOARD_ID, fullUpdate))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/boards/{boardId}", BOARD_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullUpdate)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(boardService, times(1)).updateBoard(USER_ID, BOARD_ID, fullUpdate);
    }

    @Test
    @DisplayName("Create Multiple Boards - Success")
    void testCreateMultipleBoards() throws Exception {
        // Arrange
        BoardResponseDTO board1 = new BoardResponseDTO();
        board1.setBoardId("board-001");
        board1.setName("Board 1");

        BoardResponseDTO board2 = new BoardResponseDTO();
        board2.setBoardId("board-002");
        board2.setName("Board 2");

        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(board1)
                .thenReturn(board2);

        // Create first board
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Board 1")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.boardId").value("board-001"));

        // Create second board
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Board 2")
                        .param("visibility", "PRIVATE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.boardId").value("board-002"));

        verify(boardService, times(2)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("GET /boards - Verify Pin Count Display")
    void testGetAllPublicBoards_WithPinCount() throws Exception {
        // Arrange
        BoardResponseDTO boardWithPins = new BoardResponseDTO();
        boardWithPins.setBoardId(BOARD_ID);
        boardWithPins.setPinCount(25);

        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 50, false, false);
        PaginatedResponse<BoardResponseDTO> response = new PaginatedResponse<>(
                Collections.singletonList(boardWithPins),
                pagination
        );

        when(boardService.getAllPublicBoards(0, 50, null))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/boards"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].pinCount").value(25));

        verify(boardService, times(1)).getAllPublicBoards(0, 50, null);
    }

    @Test
    @DisplayName("POST /boards - Success - Empty Optional Description")
    void testCreateBoard_EmptyDescription() throws Exception {
        // Arrange
        when(boardService.createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull()))
                .thenReturn(boardResponseDTO);

        // Act & Assert
        mockMvc.perform(multipart("/boards")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("name", "Board Without Description")
                        .param("description", "")
                        .param("visibility", "PUBLIC"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"));

        verify(boardService, times(1)).createBoard(eq(USER_ID), any(BoardCreationDTO.class), isNull());
    }

    @Test
    @DisplayName("GET /boards/collaborative - Sort By Name")
    void testGetCollaborativeBoards_SortByName() throws Exception {
        // Arrange
        when(boardService.getCollaborativeBoards(USER_ID, 0, 20, "name"))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/boards/collaborative")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("sort", "name"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(boardService, times(1)).getCollaborativeBoards(USER_ID, 0, 20, "name");
    }

    @Test
    @DisplayName("Multiple Users - Different Boards")
    void testMultipleUsers_DifferentBoards() throws Exception {
        // Arrange
        String user1 = "user-111";
        String user2 = "user-222";

        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        
        BoardResponseDTO user1Board = new BoardResponseDTO();
        user1Board.setUserId(user1);
        PaginatedResponse<BoardResponseDTO> user1Response = new PaginatedResponse<>(
                Collections.singletonList(user1Board),
                pagination
        );

        BoardResponseDTO user2Board = new BoardResponseDTO();
        user2Board.setUserId(user2);
        PaginatedResponse<BoardResponseDTO> user2Response = new PaginatedResponse<>(
                Collections.singletonList(user2Board),
                pagination
        );

        when(boardService.getUserBoards(user1, 0, 20, null)).thenReturn(user1Response);
        when(boardService.getUserBoards(user2, 0, 20, null)).thenReturn(user2Response);

        // Get user 1 boards
        mockMvc.perform(get("/boards/user/{userId}", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].userId").value(user1));

        // Get user 2 boards
        mockMvc.perform(get("/boards/user/{userId}", user2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].userId").value(user2));

        verify(boardService, times(1)).getUserBoards(user1, 0, 20, null);
        verify(boardService, times(1)).getUserBoards(user2, 0, 20, null);
    }
}
