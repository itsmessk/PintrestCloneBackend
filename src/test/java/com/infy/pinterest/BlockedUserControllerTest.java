package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.BlockedUserController;
import com.infy.pinterest.dto.BlockedUserDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PaginationDTO;
import com.infy.pinterest.service.BlockedUserService;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@WebMvcTest(BlockedUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class BlockedUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BlockedUserService blockedUserService;

    private static final String CURRENT_USER_ID = "user-123";
    private static final String TARGET_USER_ID = "user-456";
    private static final String USER_ID_HEADER = "X-User-Id";

    private BlockedUserDTO blockedUserDTO;
    private PaginatedResponse<BlockedUserDTO> paginatedResponse;

    @BeforeEach
    void setUp() {
        // Setup blocked user DTO
        blockedUserDTO = new BlockedUserDTO();
        blockedUserDTO.setBlockId("block-123");
        blockedUserDTO.setBlockedUserId("user-456");
        blockedUserDTO.setBlockedUsername("blockeduser");
        blockedUserDTO.setBlockedUserFullName("Blocked User");
        blockedUserDTO.setBlockedUserProfilePicture("https://example.com/profile.jpg");
        blockedUserDTO.setBlockedAt(LocalDateTime.now());

        // Setup paginated response
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        paginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(blockedUserDTO),
                pagination
        );
    }

    // ==================== BLOCK USER TESTS ====================

    @Test
    @DisplayName("POST /blocked-users/block/{targetUserId} - Success")
    void testBlockUser_Success() throws Exception {
        // Arrange
        doNothing().when(blockedUserService).blockUser(CURRENT_USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(post("/blocked-users/block/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User blocked successfully"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(blockedUserService, times(1)).blockUser(CURRENT_USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("POST /blocked-users/block/{targetUserId} - Failure - Missing User ID Header")
    void testBlockUser_MissingHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/blocked-users/block/{targetUserId}", TARGET_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(blockedUserService, never()).blockUser(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /blocked-users/block/{targetUserId} - Failure - Block Self")
    void testBlockUser_BlockSelf() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Cannot block yourself"))
                .when(blockedUserService).blockUser(CURRENT_USER_ID, CURRENT_USER_ID);

        // Act & Assert
        mockMvc.perform(post("/blocked-users/block/{targetUserId}", CURRENT_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Cannot block yourself"));

        verify(blockedUserService, times(1)).blockUser(CURRENT_USER_ID, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("POST /blocked-users/block/{targetUserId} - Failure - Already Blocked")
    void testBlockUser_AlreadyBlocked() throws Exception {
        // Arrange
        doThrow(new RuntimeException("User is already blocked"))
                .when(blockedUserService).blockUser(CURRENT_USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(post("/blocked-users/block/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User is already blocked"));

        verify(blockedUserService, times(1)).blockUser(CURRENT_USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("POST /blocked-users/block/{targetUserId} - Failure - Empty Target User ID")
    void testBlockUser_EmptyTargetUserId() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/blocked-users/block/ ")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(blockedUserService, never()).blockUser(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /blocked-users/block/{targetUserId} - Edge Case - Special Characters in User ID")
    void testBlockUser_SpecialCharactersInUserId() throws Exception {
        // Arrange
        String specialUserId = "user-@#$%";
        doNothing().when(blockedUserService).blockUser(CURRENT_USER_ID, specialUserId);

        // Act & Assert
        mockMvc.perform(post("/blocked-users/block/{targetUserId}", specialUserId)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(blockedUserService, times(1)).blockUser(CURRENT_USER_ID, specialUserId);
    }

    // ==================== UNBLOCK USER TESTS ====================

    @Test
    @DisplayName("DELETE /blocked-users/unblock/{targetUserId} - Success")
    void testUnblockUser_Success() throws Exception {
        // Arrange
        doNothing().when(blockedUserService).unblockUser(CURRENT_USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/blocked-users/unblock/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User unblocked successfully"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(blockedUserService, times(1)).unblockUser(CURRENT_USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("DELETE /blocked-users/unblock/{targetUserId} - Failure - User Not Blocked")
    void testUnblockUser_UserNotBlocked() throws Exception {
        // Arrange
        doThrow(new RuntimeException("User is not blocked"))
                .when(blockedUserService).unblockUser(CURRENT_USER_ID, TARGET_USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/blocked-users/unblock/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User is not blocked"));

        verify(blockedUserService, times(1)).unblockUser(CURRENT_USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("DELETE /blocked-users/unblock/{targetUserId} - Failure - Missing User ID Header")
    void testUnblockUser_MissingHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/blocked-users/unblock/{targetUserId}", TARGET_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(blockedUserService, never()).unblockUser(anyString(), anyString());
    }

    @Test
    @DisplayName("DELETE /blocked-users/unblock/{targetUserId} - Edge Case - Empty Target User ID")
    void testUnblockUser_EmptyTargetUserId() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/blocked-users/unblock/ ")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(blockedUserService, never()).unblockUser(anyString(), anyString());
    }

    // ==================== CHECK IF BLOCKED TESTS ====================

    @Test
    @DisplayName("GET /blocked-users/check/{targetUserId} - Success - User Is Blocked")
    void testCheckIfBlocked_UserIsBlocked() throws Exception {
        // Arrange
        when(blockedUserService.isBlockedInEitherDirection(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/blocked-users/check/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Block status retrieved"))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(blockedUserService, times(1)).isBlockedInEitherDirection(CURRENT_USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("GET /blocked-users/check/{targetUserId} - Success - User Is Not Blocked")
    void testCheckIfBlocked_UserIsNotBlocked() throws Exception {
        // Arrange
        when(blockedUserService.isBlockedInEitherDirection(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/blocked-users/check/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Block status retrieved"))
                .andExpect(jsonPath("$.data").value(false))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(blockedUserService, times(1)).isBlockedInEitherDirection(CURRENT_USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("GET /blocked-users/check/{targetUserId} - Failure - Missing User ID Header")
    void testCheckIfBlocked_MissingHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/blocked-users/check/{targetUserId}", TARGET_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(blockedUserService, never()).isBlockedInEitherDirection(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /blocked-users/check/{targetUserId} - Edge Case - Check Self")
    void testCheckIfBlocked_CheckSelf() throws Exception {
        // Arrange
        when(blockedUserService.isBlockedInEitherDirection(CURRENT_USER_ID, CURRENT_USER_ID))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/blocked-users/check/{targetUserId}", CURRENT_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        verify(blockedUserService, times(1)).isBlockedInEitherDirection(CURRENT_USER_ID, CURRENT_USER_ID);
    }

    // ==================== GET BLOCKED USERS TESTS ====================

    @Test
    @DisplayName("GET /blocked-users - Success - Default Pagination")
    void testGetBlockedUsers_DefaultPagination() throws Exception {
        // Arrange
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Blocked users retrieved successfully"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].blockId").value("block-123"))
                .andExpect(jsonPath("$.data.data[0].blockedUserId").value("user-456"))
                .andExpect(jsonPath("$.data.data[0].blockedUsername").value("blockeduser"))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(1))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(blockedUserService, times(1)).getBlockedUsers(CURRENT_USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /blocked-users - Success - Custom Pagination")
    void testGetBlockedUsers_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO pagination = new PaginationDTO(2, 5, 50L, 10, true, true);
        PaginatedResponse<BlockedUserDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(blockedUserDTO),
                pagination
        );
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, 2, 10))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .param("page", "2")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(5))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(50))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(true))
                .andExpect(jsonPath("$.data.pagination.hasPrevious").value(true));

        verify(blockedUserService, times(1)).getBlockedUsers(CURRENT_USER_ID, 2, 10);
    }

    @Test
    @DisplayName("GET /blocked-users - Success - Empty List")
    void testGetBlockedUsers_EmptyList() throws Exception {
        // Arrange
        PaginationDTO pagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<BlockedUserDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                pagination
        );
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, 0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(0)))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(0));

        verify(blockedUserService, times(1)).getBlockedUsers(CURRENT_USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /blocked-users - Success - Multiple Blocked Users")
    void testGetBlockedUsers_MultipleUsers() throws Exception {
        // Arrange
        BlockedUserDTO blockedUser2 = new BlockedUserDTO();
        blockedUser2.setBlockId("block-456");
        blockedUser2.setBlockedUserId("user-789");
        blockedUser2.setBlockedUsername("anotheruser");
        blockedUser2.setBlockedUserFullName("Another User");
        blockedUser2.setBlockedUserProfilePicture("https://example.com/profile2.jpg");
        blockedUser2.setBlockedAt(LocalDateTime.now());

        List<BlockedUserDTO> blockedUsers = Arrays.asList(blockedUserDTO, blockedUser2);
        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<BlockedUserDTO> multipleResponse = new PaginatedResponse<>(
                blockedUsers,
                pagination
        );
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, 0, 20))
                .thenReturn(multipleResponse);

        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].blockedUserId").value("user-456"))
                .andExpect(jsonPath("$.data.data[1].blockedUserId").value("user-789"))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(2));

        verify(blockedUserService, times(1)).getBlockedUsers(CURRENT_USER_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /blocked-users - Failure - Missing User ID Header")
    void testGetBlockedUsers_MissingHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(blockedUserService, never()).getBlockedUsers(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /blocked-users - Edge Case - Negative Page Number")
    void testGetBlockedUsers_NegativePageNumber() throws Exception {
        // Arrange
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, -1, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(blockedUserService, times(1)).getBlockedUsers(CURRENT_USER_ID, -1, 20);
    }

    @Test
    @DisplayName("GET /blocked-users - Edge Case - Large Page Size")
    void testGetBlockedUsers_LargePageSize() throws Exception {
        // Arrange
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, 0, 1000))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .param("size", "1000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(blockedUserService, times(1)).getBlockedUsers(CURRENT_USER_ID, 0, 1000);
    }

    @Test
    @DisplayName("GET /blocked-users - Edge Case - Zero Page Size")
    void testGetBlockedUsers_ZeroPageSize() throws Exception {
        // Arrange
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, 0, 0))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(blockedUserService, times(1)).getBlockedUsers(CURRENT_USER_ID, 0, 0);
    }

    // ==================== INTEGRATION & EDGE CASE TESTS ====================

    @Test
    @DisplayName("Block and Unblock Flow - Sequential Operations")
    void testBlockUnblockFlow() throws Exception {
        // Arrange
        doNothing().when(blockedUserService).blockUser(CURRENT_USER_ID, TARGET_USER_ID);
        doNothing().when(blockedUserService).unblockUser(CURRENT_USER_ID, TARGET_USER_ID);

        // Act & Assert - Block
        mockMvc.perform(post("/blocked-users/block/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User blocked successfully"));

        // Act & Assert - Unblock
        mockMvc.perform(delete("/blocked-users/unblock/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User unblocked successfully"));

        verify(blockedUserService, times(1)).blockUser(CURRENT_USER_ID, TARGET_USER_ID);
        verify(blockedUserService, times(1)).unblockUser(CURRENT_USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("Check Block Status After Blocking")
    void testCheckBlockStatusAfterBlocking() throws Exception {
        // Arrange
        doNothing().when(blockedUserService).blockUser(CURRENT_USER_ID, TARGET_USER_ID);
        when(blockedUserService.isBlockedInEitherDirection(CURRENT_USER_ID, TARGET_USER_ID))
                .thenReturn(true);

        // Act & Assert - Block
        mockMvc.perform(post("/blocked-users/block/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Act & Assert - Check Status
        mockMvc.perform(get("/blocked-users/check/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(blockedUserService, times(1)).blockUser(CURRENT_USER_ID, TARGET_USER_ID);
        verify(blockedUserService, times(1)).isBlockedInEitherDirection(CURRENT_USER_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("GET /blocked-users - Verify Complete DTO Structure")
    void testGetBlockedUsers_VerifyDTOStructure() throws Exception {
        // Arrange
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].blockId").exists())
                .andExpect(jsonPath("$.data.data[0].blockedUserId").exists())
                .andExpect(jsonPath("$.data.data[0].blockedUsername").exists())
                .andExpect(jsonPath("$.data.data[0].blockedUserFullName").exists())
                .andExpect(jsonPath("$.data.data[0].blockedUserProfilePicture").exists())
                .andExpect(jsonPath("$.data.data[0].blockedAt").exists());

        verify(blockedUserService, times(1)).getBlockedUsers(CURRENT_USER_ID, 0, 20);
    }

    @Test
    @DisplayName("POST /blocked-users/block - Header with Empty String")
    void testBlockUser_EmptyHeaderValue() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/blocked-users/block/{targetUserId}", TARGET_USER_ID)
                        .header(USER_ID_HEADER, "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(blockedUserService, never()).blockUser(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /blocked-users - Invalid Query Parameters")
    void testGetBlockedUsers_InvalidQueryParams() throws Exception {
        // Arrange
        when(blockedUserService.getBlockedUsers(CURRENT_USER_ID, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert - Non-numeric page parameter should default or error
        mockMvc.perform(get("/blocked-users")
                        .header(USER_ID_HEADER, CURRENT_USER_ID)
                        .param("page", "abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(blockedUserService, never()).getBlockedUsers(anyString(), anyInt(), anyInt());
    }
}
