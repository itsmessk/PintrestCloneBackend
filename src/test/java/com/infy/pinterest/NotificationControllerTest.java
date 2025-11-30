package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.NotificationController;
import com.infy.pinterest.dto.NotificationResponseDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PaginationDTO;
import com.infy.pinterest.dto.UserSummaryDTO;
import com.infy.pinterest.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    private static final String USER_ID = "user-123";
    private static final String NOTIFICATION_ID = "notification-456";
    private static final String SENDER_ID = "user-789";
    private static final String USER_ID_HEADER = "X-User-Id";

    private NotificationResponseDTO notificationResponseDTO;
    private NotificationResponseDTO unreadNotificationDTO;
    private PaginatedResponse<NotificationResponseDTO> paginatedResponse;
    private UserSummaryDTO senderSummary;

    @BeforeEach
    void setUp() {
        // Setup sender summary
        senderSummary = new UserSummaryDTO();
        senderSummary.setUserId(SENDER_ID);
        senderSummary.setUsername("john_doe");
        senderSummary.setProfilePictureUrl("https://example.com/profile.jpg");

        // Setup read notification response DTO
        notificationResponseDTO = new NotificationResponseDTO();
        notificationResponseDTO.setNotificationId(NOTIFICATION_ID);
        notificationResponseDTO.setType("NEW_FOLLOWER");
        notificationResponseDTO.setMessage("john_doe started following you");
        notificationResponseDTO.setEntityId(SENDER_ID);
        notificationResponseDTO.setEntityType("USER");
        notificationResponseDTO.setIsRead(true);
        notificationResponseDTO.setCreatedAt(LocalDateTime.now().minusHours(2));
        notificationResponseDTO.setReadAt(LocalDateTime.now().minusHours(1));
        notificationResponseDTO.setSender(senderSummary);

        // Setup unread notification response DTO
        unreadNotificationDTO = new NotificationResponseDTO();
        unreadNotificationDTO.setNotificationId("notification-789");
        unreadNotificationDTO.setType("PIN_LIKED");
        unreadNotificationDTO.setMessage("john_doe liked your pin");
        unreadNotificationDTO.setEntityId("pin-123");
        unreadNotificationDTO.setEntityType("PIN");
        unreadNotificationDTO.setIsRead(false);
        unreadNotificationDTO.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        unreadNotificationDTO.setReadAt(null);
        unreadNotificationDTO.setSender(senderSummary);

        // Setup paginated response
        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        List<NotificationResponseDTO> notifications = Arrays.asList(unreadNotificationDTO, notificationResponseDTO);
        paginatedResponse = new PaginatedResponse<>(notifications, pagination);
    }

    // ==================== GET /notifications - Success Tests ====================

    @Test
    @DisplayName("GET /notifications - Success - All Notifications")
    void testGetNotifications_Success_AllNotifications() throws Exception {
        // Arrange
        when(notificationService.getNotifications(USER_ID, null, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Notifications retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].notificationId").value("notification-789"))
                .andExpect(jsonPath("$.data.data[0].type").value("PIN_LIKED"))
                .andExpect(jsonPath("$.data.data[0].isRead").value(false))
                .andExpect(jsonPath("$.data.data[1].notificationId").value(NOTIFICATION_ID))
                .andExpect(jsonPath("$.data.data[1].type").value("NEW_FOLLOWER"))
                .andExpect(jsonPath("$.data.data[1].isRead").value(true))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(2));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /notifications - Success - Filter Unread Only")
    void testGetNotifications_Success_FilterUnreadOnly() throws Exception {
        // Arrange
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        PaginatedResponse<NotificationResponseDTO> unreadResponse = new PaginatedResponse<>(
                Collections.singletonList(unreadNotificationDTO),
                pagination
        );
        when(notificationService.getNotifications(USER_ID, false, 0, 20))
                .thenReturn(unreadResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("isRead", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].isRead").value(false))
                .andExpect(jsonPath("$.data.data[0].type").value("PIN_LIKED"));

        verify(notificationService, times(1)).getNotifications(USER_ID, false, 0, 20);
    }

    @Test
    @DisplayName("GET /notifications - Success - Filter Read Only")
    void testGetNotifications_Success_FilterReadOnly() throws Exception {
        // Arrange
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        PaginatedResponse<NotificationResponseDTO> readResponse = new PaginatedResponse<>(
                Collections.singletonList(notificationResponseDTO),
                pagination
        );
        when(notificationService.getNotifications(USER_ID, true, 0, 20))
                .thenReturn(readResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("isRead", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].isRead").value(true))
                .andExpect(jsonPath("$.data.data[0].type").value("NEW_FOLLOWER"));

        verify(notificationService, times(1)).getNotifications(USER_ID, true, 0, 20);
    }

    @Test
    @DisplayName("GET /notifications - Success - Custom Pagination")
    void testGetNotifications_Success_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(2, 5, 100L, 10, true, true);
        PaginatedResponse<NotificationResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(notificationResponseDTO),
                customPagination
        );
        when(notificationService.getNotifications(USER_ID, null, 2, 10))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "2")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(5))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(100))
                .andExpect(jsonPath("$.data.pagination.pageSize").value(10));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 2, 10);
    }

    @Test
    @DisplayName("GET /notifications - Success - Empty List")
    void testGetNotifications_Success_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<NotificationResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(notificationService.getNotifications(USER_ID, null, 0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(0)))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(0));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /notifications - Success - With Sender Information")
    void testGetNotifications_Success_WithSenderInfo() throws Exception {
        // Arrange
        when(notificationService.getNotifications(USER_ID, null, 0, 20))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].sender.userId").value(SENDER_ID))
                .andExpect(jsonPath("$.data.data[0].sender.username").value("john_doe"))
                .andExpect(jsonPath("$.data.data[0].sender.profilePictureUrl").value("https://example.com/profile.jpg"));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /notifications - Success - Multiple Notification Types")
    void testGetNotifications_Success_MultipleTypes() throws Exception {
        // Arrange
        NotificationResponseDTO invitationNotification = new NotificationResponseDTO();
        invitationNotification.setNotificationId("notification-001");
        invitationNotification.setType("INVITATION_RECEIVED");
        invitationNotification.setMessage("You have been invited to collaborate");
        invitationNotification.setIsRead(false);
        invitationNotification.setCreatedAt(LocalDateTime.now());

        NotificationResponseDTO boardSharedNotification = new NotificationResponseDTO();
        boardSharedNotification.setNotificationId("notification-002");
        boardSharedNotification.setType("BOARD_SHARED");
        boardSharedNotification.setMessage("A board was shared with you");
        boardSharedNotification.setIsRead(false);
        boardSharedNotification.setCreatedAt(LocalDateTime.now());

        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<NotificationResponseDTO> multiTypeResponse = new PaginatedResponse<>(
                Arrays.asList(invitationNotification, boardSharedNotification),
                pagination
        );
        when(notificationService.getNotifications(USER_ID, null, 0, 20))
                .thenReturn(multiTypeResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].type").value("INVITATION_RECEIVED"))
                .andExpect(jsonPath("$.data.data[1].type").value("BOARD_SHARED"));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("GET /notifications - Failure - Missing User ID Header")
    void testGetNotifications_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/notifications"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getNotifications(anyString(), any(), anyInt(), anyInt());
    }

    // ==================== GET /notifications/unread-count - Success Tests ====================

    @Test
    @DisplayName("GET /notifications/unread-count - Success - Zero Count")
    void testGetUnreadCount_Success_ZeroCount() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Unread count retrieved successfully"))
                .andExpect(jsonPath("$.data").value(0));

        verify(notificationService, times(1)).getUnreadCount(USER_ID);
    }

    @Test
    @DisplayName("GET /notifications/unread-count - Success - Positive Count")
    void testGetUnreadCount_Success_PositiveCount() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(15L);

        // Act & Assert
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value(15));

        verify(notificationService, times(1)).getUnreadCount(USER_ID);
    }

    @Test
    @DisplayName("GET /notifications/unread-count - Success - Large Count")
    void testGetUnreadCount_Success_LargeCount() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(999L);

        // Act & Assert
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(999));

        verify(notificationService, times(1)).getUnreadCount(USER_ID);
    }

    @Test
    @DisplayName("GET /notifications/unread-count - Failure - Missing User ID Header")
    void testGetUnreadCount_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/notifications/unread-count"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getUnreadCount(anyString());
    }

    // ==================== PUT /notifications/{notificationId}/read - Success Tests ====================

    @Test
    @DisplayName("PUT /notifications/{notificationId}/read - Success")
    void testMarkAsRead_Success() throws Exception {
        // Arrange
        doNothing().when(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(put("/notifications/{notificationId}/read", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Notification marked as read"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(notificationService, times(1)).markAsRead(NOTIFICATION_ID, USER_ID);
    }

    @Test
    @DisplayName("PUT /notifications/{notificationId}/read - Failure - Notification Not Found")
    void testMarkAsRead_Failure_NotificationNotFound() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Notification not found"))
                .when(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(put("/notifications/{notificationId}/read", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(notificationService, times(1)).markAsRead(NOTIFICATION_ID, USER_ID);
    }

    @Test
    @DisplayName("PUT /notifications/{notificationId}/read - Failure - Unauthorized Access")
    void testMarkAsRead_Failure_UnauthorizedAccess() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Unauthorized access to notification"))
                .when(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(put("/notifications/{notificationId}/read", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(notificationService, times(1)).markAsRead(NOTIFICATION_ID, USER_ID);
    }

    @Test
    @DisplayName("PUT /notifications/{notificationId}/read - Failure - Missing User ID Header")
    void testMarkAsRead_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/notifications/{notificationId}/read", NOTIFICATION_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).markAsRead(anyString(), anyString());
    }

    @Test
    @DisplayName("PUT /notifications/{notificationId}/read - Success - Already Read Notification")
    void testMarkAsRead_Success_AlreadyRead() throws Exception {
        // Arrange
        doNothing().when(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(put("/notifications/{notificationId}/read", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(notificationService, times(1)).markAsRead(NOTIFICATION_ID, USER_ID);
    }

    // ==================== PUT /notifications/mark-all-read - Success Tests ====================

    @Test
    @DisplayName("PUT /notifications/mark-all-read - Success")
    void testMarkAllAsRead_Success() throws Exception {
        // Arrange
        doNothing().when(notificationService).markAllAsRead(USER_ID);

        // Act & Assert
        mockMvc.perform(put("/notifications/mark-all-read")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("All notifications marked as read"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(notificationService, times(1)).markAllAsRead(USER_ID);
    }

    @Test
    @DisplayName("PUT /notifications/mark-all-read - Success - No Unread Notifications")
    void testMarkAllAsRead_Success_NoUnreadNotifications() throws Exception {
        // Arrange
        doNothing().when(notificationService).markAllAsRead(USER_ID);

        // Act & Assert
        mockMvc.perform(put("/notifications/mark-all-read")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(notificationService, times(1)).markAllAsRead(USER_ID);
    }

    @Test
    @DisplayName("PUT /notifications/mark-all-read - Failure - Missing User ID Header")
    void testMarkAllAsRead_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/notifications/mark-all-read"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).markAllAsRead(anyString());
    }

    @Test
    @DisplayName("PUT /notifications/mark-all-read - Failure - Service Error")
    void testMarkAllAsRead_Failure_ServiceError() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database error"))
                .when(notificationService).markAllAsRead(USER_ID);

        // Act & Assert
        mockMvc.perform(put("/notifications/mark-all-read")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(notificationService, times(1)).markAllAsRead(USER_ID);
    }

    // ==================== DELETE /notifications/{notificationId} - Success Tests ====================

    @Test
    @DisplayName("DELETE /notifications/{notificationId} - Success")
    void testDeleteNotification_Success() throws Exception {
        // Arrange
        doNothing().when(notificationService).deleteNotification(NOTIFICATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/notifications/{notificationId}", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Notification deleted successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(notificationService, times(1)).deleteNotification(NOTIFICATION_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /notifications/{notificationId} - Failure - Notification Not Found")
    void testDeleteNotification_Failure_NotificationNotFound() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Notification not found"))
                .when(notificationService).deleteNotification(NOTIFICATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/notifications/{notificationId}", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(notificationService, times(1)).deleteNotification(NOTIFICATION_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /notifications/{notificationId} - Failure - Unauthorized Access")
    void testDeleteNotification_Failure_UnauthorizedAccess() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Unauthorized access to notification"))
                .when(notificationService).deleteNotification(NOTIFICATION_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/notifications/{notificationId}", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(notificationService, times(1)).deleteNotification(NOTIFICATION_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /notifications/{notificationId} - Failure - Missing User ID Header")
    void testDeleteNotification_Failure_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/notifications/{notificationId}", NOTIFICATION_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).deleteNotification(anyString(), anyString());
    }

    @Test
    @DisplayName("DELETE /notifications/{notificationId} - Success - Delete Unread Notification")
    void testDeleteNotification_Success_UnreadNotification() throws Exception {
        // Arrange
        doNothing().when(notificationService).deleteNotification("notification-789", USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/notifications/{notificationId}", "notification-789")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(notificationService, times(1)).deleteNotification("notification-789", USER_ID);
    }

    // ==================== Integration & Edge Case Tests ====================

    @Test
    @DisplayName("Complete Notification Flow - Receive, Read, Delete")
    void testCompleteNotificationFlow() throws Exception {
        // Arrange
        when(notificationService.getNotifications(USER_ID, false, 0, 20))
                .thenReturn(paginatedResponse);
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(1L);
        doNothing().when(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(0L);
        doNothing().when(notificationService).deleteNotification(NOTIFICATION_ID, USER_ID);

        // Get unread notifications
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("isRead", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)));

        // Check unread count
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // Mark as read
        mockMvc.perform(put("/notifications/{notificationId}/read", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Check unread count again
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        // Delete notification
        mockMvc.perform(delete("/notifications/{notificationId}", NOTIFICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).getNotifications(USER_ID, false, 0, 20);
        verify(notificationService, times(2)).getUnreadCount(USER_ID);
        verify(notificationService, times(1)).markAsRead(NOTIFICATION_ID, USER_ID);
        verify(notificationService, times(1)).deleteNotification(NOTIFICATION_ID, USER_ID);
    }

    @Test
    @DisplayName("Bulk Operations Flow - Mark All Read")
    void testBulkOperationsFlow() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(25L);
        doNothing().when(notificationService).markAllAsRead(USER_ID);
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(0L);

        // Check initial unread count
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(25));

        // Mark all as read
        mockMvc.perform(put("/notifications/mark-all-read")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Verify count is zero
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        verify(notificationService, times(2)).getUnreadCount(USER_ID);
        verify(notificationService, times(1)).markAllAsRead(USER_ID);
    }

    @Test
    @DisplayName("Pagination Boundary Test - First Page")
    void testPaginationBoundary_FirstPage() throws Exception {
        // Arrange
        PaginationDTO firstPagePagination = new PaginationDTO(0, 5, 100L, 20, true, false);
        PaginatedResponse<NotificationResponseDTO> firstPageResponse = new PaginatedResponse<>(
                Collections.singletonList(notificationResponseDTO),
                firstPagePagination
        );
        when(notificationService.getNotifications(USER_ID, null, 0, 20))
                .thenReturn(firstPageResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(true))
                .andExpect(jsonPath("$.data.pagination.hasPrevious").value(false));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("Pagination Boundary Test - Last Page")
    void testPaginationBoundary_LastPage() throws Exception {
        // Arrange
        PaginationDTO lastPagePagination = new PaginationDTO(4, 5, 100L, 20, false, true);
        PaginatedResponse<NotificationResponseDTO> lastPageResponse = new PaginatedResponse<>(
                Collections.singletonList(notificationResponseDTO),
                lastPagePagination
        );
        when(notificationService.getNotifications(USER_ID, null, 4, 20))
                .thenReturn(lastPageResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "4")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(4))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(false))
                .andExpect(jsonPath("$.data.pagination.hasPrevious").value(true));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 4, 20);
    }

    @Test
    @DisplayName("All Notification Types Coverage")
    void testAllNotificationTypes() throws Exception {
        // Arrange
        NotificationResponseDTO[] notificationTypes = {
                createNotificationDTO("n1", "INVITATION_RECEIVED", "Invitation received"),
                createNotificationDTO("n2", "INVITATION_ACCEPTED", "Invitation accepted"),
                createNotificationDTO("n3", "INVITATION_DECLINED", "Invitation declined"),
                createNotificationDTO("n4", "NEW_FOLLOWER", "New follower"),
                createNotificationDTO("n5", "PIN_SAVED", "Pin saved"),
                createNotificationDTO("n6", "PIN_LIKED", "Pin liked"),
                createNotificationDTO("n7", "BOARD_SHARED", "Board shared"),
                createNotificationDTO("n8", "COLLABORATION_ADDED", "Collaboration added")
        };

        PaginationDTO pagination = new PaginationDTO(0, 1, 8L, 20, false, false);
        PaginatedResponse<NotificationResponseDTO> allTypesResponse = new PaginatedResponse<>(
                Arrays.asList(notificationTypes),
                pagination
        );
        when(notificationService.getNotifications(USER_ID, null, 0, 20))
                .thenReturn(allTypesResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(8)))
                .andExpect(jsonPath("$.data.data[0].type").value("INVITATION_RECEIVED"))
                .andExpect(jsonPath("$.data.data[1].type").value("INVITATION_ACCEPTED"))
                .andExpect(jsonPath("$.data.data[2].type").value("INVITATION_DECLINED"))
                .andExpect(jsonPath("$.data.data[3].type").value("NEW_FOLLOWER"))
                .andExpect(jsonPath("$.data.data[4].type").value("PIN_SAVED"))
                .andExpect(jsonPath("$.data.data[5].type").value("PIN_LIKED"))
                .andExpect(jsonPath("$.data.data[6].type").value("BOARD_SHARED"))
                .andExpect(jsonPath("$.data.data[7].type").value("COLLABORATION_ADDED"));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 0, 20);
    }

    @Test
    @DisplayName("Multiple Users - Different User IDs")
    void testMultipleUsers_DifferentUserIds() throws Exception {
        // Arrange
        String user1 = "user-111";
        String user2 = "user-222";
        
        when(notificationService.getUnreadCount(user1)).thenReturn(5L);
        when(notificationService.getUnreadCount(user2)).thenReturn(10L);

        // Act & Assert - User 1
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));

        // Act & Assert - User 2
        mockMvc.perform(get("/notifications/unread-count")
                        .header(USER_ID_HEADER, user2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(10));

        verify(notificationService, times(1)).getUnreadCount(user1);
        verify(notificationService, times(1)).getUnreadCount(user2);
    }

    @Test
    @DisplayName("Edge Case - Large Page Number")
    void testEdgeCase_LargePageNumber() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(999, 5, 100L, 20, false, true);
        PaginatedResponse<NotificationResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(notificationService.getNotifications(USER_ID, null, 999, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "999"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 999, 20);
    }

    @Test
    @DisplayName("Edge Case - Custom Page Size")
    void testEdgeCase_CustomPageSize() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(0, 10, 100L, 10, true, false);
        PaginatedResponse<NotificationResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(notificationResponseDTO),
                customPagination
        );
        when(notificationService.getNotifications(USER_ID, null, 0, 10))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.pageSize").value(10));

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 0, 10);
    }

    @Test
    @DisplayName("Notification Without Sender")
    void testNotificationWithoutSender() throws Exception {
        // Arrange
        NotificationResponseDTO systemNotification = new NotificationResponseDTO();
        systemNotification.setNotificationId("system-001");
        systemNotification.setType("COLLABORATION_ADDED");
        systemNotification.setMessage("System notification");
        systemNotification.setIsRead(false);
        systemNotification.setCreatedAt(LocalDateTime.now());
        systemNotification.setSender(null);

        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        PaginatedResponse<NotificationResponseDTO> systemResponse = new PaginatedResponse<>(
                Collections.singletonList(systemNotification),
                pagination
        );
        when(notificationService.getNotifications(USER_ID, null, 0, 20))
                .thenReturn(systemResponse);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].sender").isEmpty());

        verify(notificationService, times(1)).getNotifications(USER_ID, null, 0, 20);
    }

    // ==================== Helper Methods ====================

    private NotificationResponseDTO createNotificationDTO(String id, String type, String message) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setNotificationId(id);
        dto.setType(type);
        dto.setMessage(message);
        dto.setIsRead(false);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }
}
