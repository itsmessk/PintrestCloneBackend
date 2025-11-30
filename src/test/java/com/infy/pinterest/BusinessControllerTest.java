package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.BusinessController;
import com.infy.pinterest.dto.*;
import com.infy.pinterest.exception.BusinessProfileAlreadyExistsException;
import com.infy.pinterest.exception.BusinessProfileNotFoundException;
import com.infy.pinterest.exception.PinNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.service.BusinessService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
@WebMvcTest(BusinessController.class)
@AutoConfigureMockMvc(addFilters = false)
class BusinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BusinessService businessService;

    private static final String USER_ID = "user-123";
    private static final String BUSINESS_ID = "business-456";
    private static final String SHOWCASE_ID = "showcase-789";
    private static final String SPONSORED_ID = "sponsored-111";
    private static final String PIN_ID = "pin-222";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String BUSINESS_ID_HEADER = "X-Business-Id";

    private BusinessProfileCreateDTO businessProfileCreateDTO;
    private BusinessProfileResponseDTO businessProfileResponseDTO;
    private BusinessProfileUpdateDTO businessProfileUpdateDTO;
    private BusinessShowcaseCreateDTO showcaseCreateDTO;
    private BusinessShowcaseResponseDTO showcaseResponseDTO;
    private SponsoredPinCreateDTO sponsoredPinCreateDTO;
    private SponsoredPinResponseDTO sponsoredPinResponseDTO;
    private PaginatedResponse<BusinessShowcaseResponseDTO> showcasePaginatedResponse;
    private PaginatedResponse<SponsoredPinResponseDTO> sponsoredPaginatedResponse;
    private CampaignAnalyticsDTO campaignAnalyticsDTO;

    @BeforeEach
    void setUp() {
        // Setup Business Profile Create DTO
        businessProfileCreateDTO = new BusinessProfileCreateDTO();
        businessProfileCreateDTO.setBusinessName("Test Business");
        businessProfileCreateDTO.setDescription("Test Description");
        businessProfileCreateDTO.setWebsite("https://test.com");
        businessProfileCreateDTO.setCategory("Fashion");
        businessProfileCreateDTO.setLogoUrl("https://example.com/logo.jpg");
        businessProfileCreateDTO.setContactEmail("test@business.com");

        // Setup Business Profile Response DTO
        businessProfileResponseDTO = new BusinessProfileResponseDTO();
        businessProfileResponseDTO.setBusinessId(BUSINESS_ID);
        businessProfileResponseDTO.setUserId(USER_ID);
        businessProfileResponseDTO.setBusinessName("Test Business");
        businessProfileResponseDTO.setDescription("Test Description");
        businessProfileResponseDTO.setWebsite("https://test.com");
        businessProfileResponseDTO.setCategory("Fashion");
        businessProfileResponseDTO.setLogoUrl("https://example.com/logo.jpg");
        businessProfileResponseDTO.setContactEmail("test@business.com");
        businessProfileResponseDTO.setFollowerCount(100);
        businessProfileResponseDTO.setCreatedAt(LocalDateTime.now());
        businessProfileResponseDTO.setBoards(Collections.emptyList());

        // Setup Business Profile Update DTO
        businessProfileUpdateDTO = new BusinessProfileUpdateDTO();
        businessProfileUpdateDTO.setBusinessName("Updated Business");
        businessProfileUpdateDTO.setDescription("Updated Description");

        // Setup Showcase Create DTO
        showcaseCreateDTO = new BusinessShowcaseCreateDTO();
        showcaseCreateDTO.setTitle("Spring Collection");
        showcaseCreateDTO.setDescription("Latest spring fashion");
        showcaseCreateDTO.setTheme("modern");

        // Setup Showcase Response DTO
        showcaseResponseDTO = new BusinessShowcaseResponseDTO();
        showcaseResponseDTO.setShowcaseId(SHOWCASE_ID);
        showcaseResponseDTO.setBusinessId(BUSINESS_ID);
        showcaseResponseDTO.setTitle("Spring Collection");
        showcaseResponseDTO.setDescription("Latest spring fashion");
        showcaseResponseDTO.setTheme("modern");
        showcaseResponseDTO.setIsActive(true);
        showcaseResponseDTO.setBusinessName("Test Business");
        showcaseResponseDTO.setPins(Collections.emptyList());

        // Setup Sponsored Pin Create DTO
        sponsoredPinCreateDTO = new SponsoredPinCreateDTO();
        sponsoredPinCreateDTO.setPinId(PIN_ID);
        sponsoredPinCreateDTO.setCampaignName("Summer Campaign");
        sponsoredPinCreateDTO.setBudget(BigDecimal.valueOf(1000.00));
        sponsoredPinCreateDTO.setStartDate(LocalDate.now());
        sponsoredPinCreateDTO.setEndDate(LocalDate.now().plusDays(30));

        // Setup Sponsored Pin Response DTO
        sponsoredPinResponseDTO = new SponsoredPinResponseDTO();
        sponsoredPinResponseDTO.setSponsoredId(SPONSORED_ID);
        sponsoredPinResponseDTO.setCampaignName("Summer Campaign");
        sponsoredPinResponseDTO.setStatus("ACTIVE");
        sponsoredPinResponseDTO.setImpressions(10000);
        sponsoredPinResponseDTO.setClicks(500);
        sponsoredPinResponseDTO.setSaves(150);
        sponsoredPinResponseDTO.setBudget(BigDecimal.valueOf(1000.00));
        sponsoredPinResponseDTO.setSpent(BigDecimal.valueOf(250.00));
        sponsoredPinResponseDTO.setStartDate(LocalDate.now());
        sponsoredPinResponseDTO.setEndDate(LocalDate.now().plusDays(30));
        sponsoredPinResponseDTO.setCreatedAt(LocalDateTime.now());

        // Setup paginated responses
        PaginationDTO pagination = new PaginationDTO(0, 1, 1L, 20, false, false);
        showcasePaginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(showcaseResponseDTO),
                pagination
        );
        sponsoredPaginatedResponse = new PaginatedResponse<>(
                Collections.singletonList(sponsoredPinResponseDTO),
                pagination
        );

        // Setup Campaign Analytics DTO
        campaignAnalyticsDTO = new CampaignAnalyticsDTO();
        campaignAnalyticsDTO.setCampaignId(SPONSORED_ID);
        campaignAnalyticsDTO.setCampaignName("Summer Campaign");
        MetricsDTO metrics = new MetricsDTO();
        metrics.setImpressions(10000);
        metrics.setClicks(500);
        metrics.setClickThroughRate(5.0);
        campaignAnalyticsDTO.setMetrics(metrics);
    }

    // ==================== BUSINESS PROFILE TESTS ====================

    @Test
    @DisplayName("POST /business/profile - Success")
    void testCreateBusinessProfile_Success() throws Exception {
        // Arrange
        when(businessService.createBusinessProfile(eq(USER_ID), any(BusinessProfileCreateDTO.class)))
                .thenReturn(businessProfileResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Business profile created successfully"))
                .andExpect(jsonPath("$.data.businessId").value(BUSINESS_ID))
                .andExpect(jsonPath("$.data.businessName").value("Test Business"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(businessService, times(1)).createBusinessProfile(eq(USER_ID), any(BusinessProfileCreateDTO.class));
    }

    @Test
    @DisplayName("POST /business/profile - Failure - Business Profile Already Exists")
    void testCreateBusinessProfile_AlreadyExists() throws Exception {
        // Arrange
        when(businessService.createBusinessProfile(eq(USER_ID), any(BusinessProfileCreateDTO.class)))
                .thenThrow(new BusinessProfileAlreadyExistsException("Business profile already exists for this user"));

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Business profile already exists for this user"));

        verify(businessService, times(1)).createBusinessProfile(eq(USER_ID), any(BusinessProfileCreateDTO.class));
    }

    @Test
    @DisplayName("POST /business/profile - Validation - Missing Business Name")
    void testCreateBusinessProfile_MissingBusinessName() throws Exception {
        // Arrange
        businessProfileCreateDTO.setBusinessName(null);

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createBusinessProfile(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/profile - Validation - Business Name Too Long")
    void testCreateBusinessProfile_BusinessNameTooLong() throws Exception {
        // Arrange
        businessProfileCreateDTO.setBusinessName("a".repeat(101));

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createBusinessProfile(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/profile - Validation - Invalid Email")
    void testCreateBusinessProfile_InvalidEmail() throws Exception {
        // Arrange
        businessProfileCreateDTO.setContactEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createBusinessProfile(anyString(), any());
    }

    @Test
    @DisplayName("GET /business/profile/{businessId} - Success")
    void testGetBusinessProfile_Success() throws Exception {
        // Arrange
        when(businessService.getBusinessProfile(BUSINESS_ID)).thenReturn(businessProfileResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/business/profile/{businessId}", BUSINESS_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Business profile retrieved successfully"))
                .andExpect(jsonPath("$.data.businessId").value(BUSINESS_ID))
                .andExpect(jsonPath("$.data.businessName").value("Test Business"));

        verify(businessService, times(1)).getBusinessProfile(BUSINESS_ID);
    }

    @Test
    @DisplayName("GET /business/profile/{businessId} - Failure - Not Found")
    void testGetBusinessProfile_NotFound() throws Exception {
        // Arrange
        when(businessService.getBusinessProfile(BUSINESS_ID))
                .thenThrow(new BusinessProfileNotFoundException("Business profile not found"));

        // Act & Assert
        mockMvc.perform(get("/business/profile/{businessId}", BUSINESS_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Business profile not found"));

        verify(businessService, times(1)).getBusinessProfile(BUSINESS_ID);
    }

    @Test
    @DisplayName("PUT /business/profile/{businessId} - Success")
    void testUpdateBusinessProfile_Success() throws Exception {
        // Arrange
        when(businessService.updateBusinessProfile(eq(BUSINESS_ID), eq(USER_ID), any(BusinessProfileUpdateDTO.class)))
                .thenReturn(businessProfileResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/business/profile/{businessId}", BUSINESS_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileUpdateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Business profile updated successfully"))
                .andExpect(jsonPath("$.data.businessId").value(BUSINESS_ID));

        verify(businessService, times(1)).updateBusinessProfile(eq(BUSINESS_ID), eq(USER_ID), any(BusinessProfileUpdateDTO.class));
    }

    // ==================== SHOWCASE TESTS ====================

    @Test
    @DisplayName("POST /business/showcases - Success")
    void testCreateShowcase_Success() throws Exception {
        // Arrange
        when(businessService.createShowcase(eq(BUSINESS_ID), any(BusinessShowcaseCreateDTO.class)))
                .thenReturn(showcaseResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/business/showcases")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showcaseCreateDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Showcase created successfully"))
                .andExpect(jsonPath("$.data.showcaseId").value(SHOWCASE_ID))
                .andExpect(jsonPath("$.data.title").value("Spring Collection"));

        verify(businessService, times(1)).createShowcase(eq(BUSINESS_ID), any(BusinessShowcaseCreateDTO.class));
    }

    @Test
    @DisplayName("POST /business/showcases - Validation - Missing Title")
    void testCreateShowcase_MissingTitle() throws Exception {
        // Arrange
        showcaseCreateDTO.setTitle(null);

        // Act & Assert
        mockMvc.perform(post("/business/showcases")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showcaseCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createShowcase(anyString(), any());
    }

    @Test
    @DisplayName("GET /business/showcases - Success - Default Pagination")
    void testGetActiveShowcases_DefaultPagination() throws Exception {
        // Arrange
        when(businessService.getActiveShowcases(0, 20))
                .thenReturn(showcasePaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/business/showcases"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Showcases retrieved successfully"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0));

        verify(businessService, times(1)).getActiveShowcases(0, 20);
    }

    @Test
    @DisplayName("PUT /business/showcases/{showcaseId} - Success")
    void testUpdateShowcase_Success() throws Exception {
        // Arrange
        BusinessShowcaseUpdateDTO updateDTO = new BusinessShowcaseUpdateDTO();
        updateDTO.setTitle("Updated Showcase");
        
        when(businessService.updateShowcase(eq(SHOWCASE_ID), eq(USER_ID), any(BusinessShowcaseUpdateDTO.class)))
                .thenReturn(showcaseResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/business/showcases/{showcaseId}", SHOWCASE_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Showcase updated successfully"));

        verify(businessService, times(1)).updateShowcase(eq(SHOWCASE_ID), eq(USER_ID), any(BusinessShowcaseUpdateDTO.class));
    }

    @Test
    @DisplayName("POST /business/showcases/{showcaseId}/pins/{pinId} - Success")
    void testAddPinToShowcase_Success() throws Exception {
        // Arrange
        doNothing().when(businessService).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, null);

        // Act & Assert
        mockMvc.perform(post("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin added to showcase successfully"));

        verify(businessService, times(1)).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, null);
    }

    @Test
    @DisplayName("POST /business/showcases/{showcaseId}/pins/{pinId} - With Display Order")
    void testAddPinToShowcase_WithDisplayOrder() throws Exception {
        // Arrange
        doNothing().when(businessService).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, 5);

        // Act & Assert
        mockMvc.perform(post("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("displayOrder", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(businessService, times(1)).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, 5);
    }

    @Test
    @DisplayName("DELETE /business/showcases/{showcaseId}/pins/{pinId} - Success")
    void testRemovePinFromShowcase_Success() throws Exception {
        // Arrange
        doNothing().when(businessService).removePinFromShowcase(SHOWCASE_ID, PIN_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Pin removed from showcase successfully"));

        verify(businessService, times(1)).removePinFromShowcase(SHOWCASE_ID, PIN_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /business/showcases/{showcaseId} - Success")
    void testDeleteShowcase_Success() throws Exception {
        // Arrange
        doNothing().when(businessService).deleteShowcase(SHOWCASE_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/business/showcases/{showcaseId}", SHOWCASE_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Showcase deleted successfully"));

        verify(businessService, times(1)).deleteShowcase(SHOWCASE_ID, USER_ID);
    }

    // ==================== SPONSORED PIN TESTS ====================

    @Test
    @DisplayName("POST /business/sponsored-pins - Success")
    void testCreateSponsoredPin_Success() throws Exception {
        // Arrange
        when(businessService.createSponsoredPin(eq(BUSINESS_ID), any(SponsoredPinCreateDTO.class)))
                .thenReturn(sponsoredPinResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Sponsored pin created successfully"))
                .andExpect(jsonPath("$.data.sponsoredId").value(SPONSORED_ID))
                .andExpect(jsonPath("$.data.campaignName").value("Summer Campaign"));

        verify(businessService, times(1)).createSponsoredPin(eq(BUSINESS_ID), any(SponsoredPinCreateDTO.class));
    }

    @Test
    @DisplayName("POST /business/sponsored-pins - Validation - Missing Pin ID")
    void testCreateSponsoredPin_MissingPinId() throws Exception {
        // Arrange
        sponsoredPinCreateDTO.setPinId(null);

        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createSponsoredPin(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/sponsored-pins - Validation - Invalid Budget")
    void testCreateSponsoredPin_InvalidBudget() throws Exception {
        // Arrange
        sponsoredPinCreateDTO.setBudget(BigDecimal.valueOf(-100));

        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createSponsoredPin(anyString(), any());
    }

    @Test
    @DisplayName("GET /business/sponsored-pins - Success")
    void testGetSponsoredPins_Success() throws Exception {
        // Arrange
        when(businessService.getSponsoredPins(BUSINESS_ID, 0, 20))
                .thenReturn(sponsoredPaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Sponsored pins retrieved successfully"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(1)));

        verify(businessService, times(1)).getSponsoredPins(BUSINESS_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /business/sponsored-feed - Success")
    void testGetActiveSponsoredFeed_Success() throws Exception {
        // Arrange
        when(businessService.getActiveSponsoredFeed(0, 20))
                .thenReturn(sponsoredPaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/business/sponsored-feed"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Sponsored feed retrieved successfully"))
                .andExpect(jsonPath("$.data.data").isArray());

        verify(businessService, times(1)).getActiveSponsoredFeed(0, 20);
    }

    @Test
    @DisplayName("PUT /business/sponsored-pins/{sponsoredId} - Success")
    void testUpdateSponsoredPin_Success() throws Exception {
        // Arrange
        SponsoredPinUpdateDTO updateDTO = new SponsoredPinUpdateDTO();
        updateDTO.setCampaignName("Updated Campaign");
        
        when(businessService.updateSponsoredPin(eq(SPONSORED_ID), eq(USER_ID), any(SponsoredPinUpdateDTO.class)))
                .thenReturn(sponsoredPinResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Sponsored pin updated successfully"));

        verify(businessService, times(1)).updateSponsoredPin(eq(SPONSORED_ID), eq(USER_ID), any(SponsoredPinUpdateDTO.class));
    }

    @Test
    @DisplayName("PUT /business/sponsored-pins/{sponsoredId}/pause - Success")
    void testPauseSponsoredPin_Success() throws Exception {
        // Arrange
        when(businessService.pauseSponsoredPin(SPONSORED_ID, USER_ID))
                .thenReturn(sponsoredPinResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}/pause", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Sponsored pin paused successfully"));

        verify(businessService, times(1)).pauseSponsoredPin(SPONSORED_ID, USER_ID);
    }

    @Test
    @DisplayName("PUT /business/sponsored-pins/{sponsoredId}/resume - Success")
    void testResumeSponsoredPin_Success() throws Exception {
        // Arrange
        when(businessService.resumeSponsoredPin(SPONSORED_ID, USER_ID))
                .thenReturn(sponsoredPinResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}/resume", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Sponsored pin resumed successfully"));

        verify(businessService, times(1)).resumeSponsoredPin(SPONSORED_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /business/sponsored-pins/{sponsoredId} - Success")
    void testDeleteSponsoredPin_Success() throws Exception {
        // Arrange
        doNothing().when(businessService).deleteSponsoredPin(SPONSORED_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/business/sponsored-pins/{sponsoredId}", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Sponsored pin deleted successfully"));

        verify(businessService, times(1)).deleteSponsoredPin(SPONSORED_ID, USER_ID);
    }

    // ==================== ANALYTICS TESTS ====================

    @Test
    @DisplayName("GET /business/analytics/{campaignId} - Success")
    void testGetCampaignAnalytics_Success() throws Exception {
        // Arrange
        when(businessService.getCampaignAnalytics(SPONSORED_ID))
                .thenReturn(campaignAnalyticsDTO);

        // Act & Assert
        mockMvc.perform(get("/business/analytics/{campaignId}", SPONSORED_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Analytics retrieved successfully"))
                .andExpect(jsonPath("$.data.campaignId").value(SPONSORED_ID))
                .andExpect(jsonPath("$.data.campaignName").value("Summer Campaign"))
                .andExpect(jsonPath("$.data.metrics.impressions").value(10000));

        verify(businessService, times(1)).getCampaignAnalytics(SPONSORED_ID);
    }

    @Test
    @DisplayName("GET /business/analytics/{campaignId} - Failure - Campaign Not Found")
    void testGetCampaignAnalytics_NotFound() throws Exception {
        // Arrange
        when(businessService.getCampaignAnalytics(SPONSORED_ID))
                .thenThrow(new ResourceNotFoundException("Campaign not found"));

        // Act & Assert
        mockMvc.perform(get("/business/analytics/{campaignId}", SPONSORED_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Campaign not found"));

        verify(businessService, times(1)).getCampaignAnalytics(SPONSORED_ID);
    }

    // ==================== EDGE CASES & INTEGRATION TESTS ====================

    @Test
    @DisplayName("Business Lifecycle - Create Profile, Showcase, and Sponsored Pin")
    void testBusinessLifecycle() throws Exception {
        // Arrange
        when(businessService.createBusinessProfile(eq(USER_ID), any(BusinessProfileCreateDTO.class)))
                .thenReturn(businessProfileResponseDTO);
        when(businessService.createShowcase(eq(BUSINESS_ID), any(BusinessShowcaseCreateDTO.class)))
                .thenReturn(showcaseResponseDTO);
        when(businessService.createSponsoredPin(eq(BUSINESS_ID), any(SponsoredPinCreateDTO.class)))
                .thenReturn(sponsoredPinResponseDTO);

        // Create Profile
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andExpect(status().isCreated());

        // Create Showcase
        mockMvc.perform(post("/business/showcases")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showcaseCreateDTO)))
                .andExpect(status().isCreated());

        // Create Sponsored Pin
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andExpect(status().isCreated());

        verify(businessService, times(1)).createBusinessProfile(eq(USER_ID), any());
        verify(businessService, times(1)).createShowcase(eq(BUSINESS_ID), any());
        verify(businessService, times(1)).createSponsoredPin(eq(BUSINESS_ID), any());
    }

    @Test
    @DisplayName("GET /business/showcases - Custom Pagination")
    void testGetActiveShowcases_CustomPagination() throws Exception {
        // Arrange
        when(businessService.getActiveShowcases(2, 10))
                .thenReturn(showcasePaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/business/showcases")
                        .param("page", "2")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(businessService, times(1)).getActiveShowcases(2, 10);
    }

    @Test
    @DisplayName("POST /business/profile - Missing User ID Header")
    void testCreateBusinessProfile_MissingHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(businessService, never()).createBusinessProfile(anyString(), any());
    }

    @Test
    @DisplayName("Sponsored Pin - Pause and Resume Flow")
    void testSponsoredPinPauseResumeFlow() throws Exception {
        // Arrange
        when(businessService.pauseSponsoredPin(SPONSORED_ID, USER_ID))
                .thenReturn(sponsoredPinResponseDTO);
        when(businessService.resumeSponsoredPin(SPONSORED_ID, USER_ID))
                .thenReturn(sponsoredPinResponseDTO);

        // Pause
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}/pause", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sponsored pin paused successfully"));

        // Resume
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}/resume", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sponsored pin resumed successfully"));

        verify(businessService, times(1)).pauseSponsoredPin(SPONSORED_ID, USER_ID);
        verify(businessService, times(1)).resumeSponsoredPin(SPONSORED_ID, USER_ID);
    }

    @Test
    @DisplayName("Showcase Pin Management - Add and Remove Flow")
    void testShowcasePinManagementFlow() throws Exception {
        // Arrange
        doNothing().when(businessService).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, 1);
        doNothing().when(businessService).removePinFromShowcase(SHOWCASE_ID, PIN_ID, USER_ID);

        // Add Pin
        mockMvc.perform(post("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("displayOrder", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pin added to showcase successfully"));

        // Remove Pin
        mockMvc.perform(delete("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pin removed from showcase successfully"));

        verify(businessService, times(1)).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, 1);
        verify(businessService, times(1)).removePinFromShowcase(SHOWCASE_ID, PIN_ID, USER_ID);
    }

    @Test
    @DisplayName("POST /business/sponsored-pins - Pin Not Found")
    void testCreateSponsoredPin_PinNotFound() throws Exception {
        // Arrange
        when(businessService.createSponsoredPin(eq(BUSINESS_ID), any(SponsoredPinCreateDTO.class)))
                .thenThrow(new PinNotFoundException("Pin not found"));

        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Pin not found"));

        verify(businessService, times(1)).createSponsoredPin(eq(BUSINESS_ID), any());
    }

    // ==================== ADDITIONAL COMPREHENSIVE TESTS ====================

    @Test
    @DisplayName("POST /business/profile - Validation - Description Too Long")
    void testCreateBusinessProfile_DescriptionTooLong() throws Exception {
        // Arrange
        businessProfileCreateDTO.setDescription("a".repeat(501));

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createBusinessProfile(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/profile - Validation - Empty Business Name")
    void testCreateBusinessProfile_EmptyBusinessName() throws Exception {
        // Arrange
        businessProfileCreateDTO.setBusinessName("");

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createBusinessProfile(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/profile - Success - With All Optional Fields")
    void testCreateBusinessProfile_WithAllOptionalFields() throws Exception {
        // Arrange
        businessProfileCreateDTO.setWebsite("https://example.com");
        businessProfileCreateDTO.setCategory("Technology");
        businessProfileCreateDTO.setLogoUrl("https://example.com/logo.png");
        
        when(businessService.createBusinessProfile(eq(USER_ID), any(BusinessProfileCreateDTO.class)))
                .thenReturn(businessProfileResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"));

        verify(businessService, times(1)).createBusinessProfile(eq(USER_ID), any());
    }

    @Test
    @DisplayName("POST /business/profile - Failure - User Not Found")
    void testCreateBusinessProfile_UserNotFound() throws Exception {
        // Arrange
        when(businessService.createBusinessProfile(eq(USER_ID), any(BusinessProfileCreateDTO.class)))
                .thenThrow(new ResourceNotFoundException("User not found with ID: " + USER_ID));

        // Act & Assert
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User not found with ID: " + USER_ID));

        verify(businessService, times(1)).createBusinessProfile(eq(USER_ID), any());
    }

    @Test
    @DisplayName("PUT /business/profile/{businessId} - Failure - Not Found")
    void testUpdateBusinessProfile_NotFound() throws Exception {
        // Arrange
        when(businessService.updateBusinessProfile(eq(BUSINESS_ID), eq(USER_ID), any(BusinessProfileUpdateDTO.class)))
                .thenThrow(new BusinessProfileNotFoundException("Business profile not found"));

        // Act & Assert
        mockMvc.perform(put("/business/profile/{businessId}", BUSINESS_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileUpdateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Business profile not found"));

        verify(businessService, times(1)).updateBusinessProfile(eq(BUSINESS_ID), eq(USER_ID), any());
    }

    @Test
    @DisplayName("PUT /business/profile/{businessId} - Validation - Business Name Too Short")
    void testUpdateBusinessProfile_BusinessNameTooShort() throws Exception {
        // Arrange
        businessProfileUpdateDTO.setBusinessName("a");

        // Act & Assert
        mockMvc.perform(put("/business/profile/{businessId}", BUSINESS_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).updateBusinessProfile(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /business/profile/{businessId} - Validation - Invalid Email Update")
    void testUpdateBusinessProfile_InvalidEmail() throws Exception {
        // Arrange
        businessProfileUpdateDTO.setContactEmail("invalid-email-format");

        // Act & Assert
        mockMvc.perform(put("/business/profile/{businessId}", BUSINESS_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).updateBusinessProfile(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /business/profile/{businessId} - Missing User ID Header")
    void testUpdateBusinessProfile_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/business/profile/{businessId}", BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileUpdateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(businessService, never()).updateBusinessProfile(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("POST /business/showcases - Validation - Title Too Long")
    void testCreateShowcase_TitleTooLong() throws Exception {
        // Arrange
        showcaseCreateDTO.setTitle("a".repeat(101));

        // Act & Assert
        mockMvc.perform(post("/business/showcases")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showcaseCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createShowcase(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/showcases - Validation - Description Too Long")
    void testCreateShowcase_DescriptionTooLong() throws Exception {
        // Arrange
        showcaseCreateDTO.setDescription("a".repeat(501));

        // Act & Assert
        mockMvc.perform(post("/business/showcases")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showcaseCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createShowcase(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/showcases - Missing Business ID Header")
    void testCreateShowcase_MissingBusinessIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/business/showcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showcaseCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(businessService, never()).createShowcase(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/showcases - Failure - Business Profile Not Found")
    void testCreateShowcase_BusinessProfileNotFound() throws Exception {
        // Arrange
        when(businessService.createShowcase(eq(BUSINESS_ID), any(BusinessShowcaseCreateDTO.class)))
                .thenThrow(new BusinessProfileNotFoundException("Business profile not found"));

        // Act & Assert
        mockMvc.perform(post("/business/showcases")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showcaseCreateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Business profile not found"));

        verify(businessService, times(1)).createShowcase(eq(BUSINESS_ID), any());
    }

    @Test
    @DisplayName("GET /business/showcases - Success - Empty List")
    void testGetActiveShowcases_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<BusinessShowcaseResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(businessService.getActiveShowcases(0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/business/showcases"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(businessService, times(1)).getActiveShowcases(0, 20);
    }

    @Test
    @DisplayName("GET /business/showcases - Success - Multiple Showcases")
    void testGetActiveShowcases_MultipleShowcases() throws Exception {
        // Arrange
        BusinessShowcaseResponseDTO showcase2 = new BusinessShowcaseResponseDTO();
        showcase2.setShowcaseId("showcase-002");
        showcase2.setTitle("Winter Collection");

        List<BusinessShowcaseResponseDTO> showcases = Arrays.asList(showcaseResponseDTO, showcase2);
        PaginationDTO pagination = new PaginationDTO(0, 1, 2L, 20, false, false);
        PaginatedResponse<BusinessShowcaseResponseDTO> multipleResponse = new PaginatedResponse<>(
                showcases,
                pagination
        );

        when(businessService.getActiveShowcases(0, 20))
                .thenReturn(multipleResponse);

        // Act & Assert
        mockMvc.perform(get("/business/showcases"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].showcaseId").value(SHOWCASE_ID))
                .andExpect(jsonPath("$.data.data[1].showcaseId").value("showcase-002"));

        verify(businessService, times(1)).getActiveShowcases(0, 20);
    }

    @Test
    @DisplayName("PUT /business/showcases/{showcaseId} - Validation - Title Too Short")
    void testUpdateShowcase_TitleTooShort() throws Exception {
        // Arrange
        BusinessShowcaseUpdateDTO updateDTO = new BusinessShowcaseUpdateDTO();
        updateDTO.setTitle("a");

        // Act & Assert
        mockMvc.perform(put("/business/showcases/{showcaseId}", SHOWCASE_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).updateShowcase(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /business/showcases/{showcaseId} - Failure - Showcase Not Found")
    void testUpdateShowcase_NotFound() throws Exception {
        // Arrange
        BusinessShowcaseUpdateDTO updateDTO = new BusinessShowcaseUpdateDTO();
        updateDTO.setTitle("Updated Showcase");
        
        when(businessService.updateShowcase(eq(SHOWCASE_ID), eq(USER_ID), any(BusinessShowcaseUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("Showcase not found"));

        // Act & Assert
        mockMvc.perform(put("/business/showcases/{showcaseId}", SHOWCASE_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Showcase not found"));

        verify(businessService, times(1)).updateShowcase(eq(SHOWCASE_ID), eq(USER_ID), any());
    }

    @Test
    @DisplayName("POST /business/showcases/{showcaseId}/pins/{pinId} - Failure - Pin Not Found")
    void testAddPinToShowcase_PinNotFound() throws Exception {
        // Arrange
        doThrow(new PinNotFoundException("Pin not found"))
                .when(businessService).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, null);

        // Act & Assert
        mockMvc.perform(post("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Pin not found"));

        verify(businessService, times(1)).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, null);
    }

    @Test
    @DisplayName("POST /business/showcases/{showcaseId}/pins/{pinId} - Failure - Showcase Not Found")
    void testAddPinToShowcase_ShowcaseNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Showcase not found"))
                .when(businessService).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, null);

        // Act & Assert
        mockMvc.perform(post("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Showcase not found"));

        verify(businessService, times(1)).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, null);
    }

    @Test
    @DisplayName("DELETE /business/showcases/{showcaseId}/pins/{pinId} - Failure - Pin Not In Showcase")
    void testRemovePinFromShowcase_PinNotInShowcase() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Pin not found in showcase"))
                .when(businessService).removePinFromShowcase(SHOWCASE_ID, PIN_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Pin not found in showcase"));

        verify(businessService, times(1)).removePinFromShowcase(SHOWCASE_ID, PIN_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /business/showcases/{showcaseId} - Failure - Showcase Not Found")
    void testDeleteShowcase_NotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Showcase not found"))
                .when(businessService).deleteShowcase(SHOWCASE_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/business/showcases/{showcaseId}", SHOWCASE_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Showcase not found"));

        verify(businessService, times(1)).deleteShowcase(SHOWCASE_ID, USER_ID);
    }

    @Test
    @DisplayName("POST /business/sponsored-pins - Validation - Missing Campaign Name")
    void testCreateSponsoredPin_MissingCampaignName() throws Exception {
        // Arrange
        sponsoredPinCreateDTO.setCampaignName(null);

        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createSponsoredPin(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/sponsored-pins - Validation - Missing Start Date")
    void testCreateSponsoredPin_MissingStartDate() throws Exception {
        // Arrange
        sponsoredPinCreateDTO.setStartDate(null);

        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createSponsoredPin(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/sponsored-pins - Validation - Missing End Date")
    void testCreateSponsoredPin_MissingEndDate() throws Exception {
        // Arrange
        sponsoredPinCreateDTO.setEndDate(null);

        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createSponsoredPin(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/sponsored-pins - Validation - Zero Budget")
    void testCreateSponsoredPin_ZeroBudget() throws Exception {
        // Arrange
        sponsoredPinCreateDTO.setBudget(BigDecimal.ZERO);

        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).createSponsoredPin(anyString(), any());
    }

    @Test
    @DisplayName("POST /business/sponsored-pins - Missing Business ID Header")
    void testCreateSponsoredPin_MissingBusinessIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/business/sponsored-pins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(businessService, never()).createSponsoredPin(anyString(), any());
    }

    @Test
    @DisplayName("GET /business/sponsored-pins - Success - Empty List")
    void testGetSponsoredPins_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<SponsoredPinResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(businessService.getSponsoredPins(BUSINESS_ID, 0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(businessService, times(1)).getSponsoredPins(BUSINESS_ID, 0, 20);
    }

    @Test
    @DisplayName("GET /business/sponsored-pins - Success - Custom Pagination")
    void testGetSponsoredPins_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(2, 5, 50L, 10, true, true);
        PaginatedResponse<SponsoredPinResponseDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(sponsoredPinResponseDTO),
                customPagination
        );
        when(businessService.getSponsoredPins(BUSINESS_ID, 2, 10))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .param("page", "2")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(5));

        verify(businessService, times(1)).getSponsoredPins(BUSINESS_ID, 2, 10);
    }

    @Test
    @DisplayName("GET /business/sponsored-feed - Success - Empty List")
    void testGetActiveSponsoredFeed_EmptyList() throws Exception {
        // Arrange
        PaginationDTO emptyPagination = new PaginationDTO(0, 0, 0L, 20, false, false);
        PaginatedResponse<SponsoredPinResponseDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                emptyPagination
        );
        when(businessService.getActiveSponsoredFeed(0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/business/sponsored-feed"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(businessService, times(1)).getActiveSponsoredFeed(0, 20);
    }

    @Test
    @DisplayName("GET /business/sponsored-feed - Success - Custom Pagination")
    void testGetActiveSponsoredFeed_CustomPagination() throws Exception {
        // Arrange
        when(businessService.getActiveSponsoredFeed(1, 10))
                .thenReturn(sponsoredPaginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/business/sponsored-feed")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(businessService, times(1)).getActiveSponsoredFeed(1, 10);
    }

    @Test
    @DisplayName("PUT /business/sponsored-pins/{sponsoredId} - Validation - Campaign Name Too Short")
    void testUpdateSponsoredPin_CampaignNameTooShort() throws Exception {
        // Arrange
        SponsoredPinUpdateDTO updateDTO = new SponsoredPinUpdateDTO();
        updateDTO.setCampaignName("a");

        // Act & Assert
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).updateSponsoredPin(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /business/sponsored-pins/{sponsoredId} - Validation - Budget Too Low")
    void testUpdateSponsoredPin_BudgetTooLow() throws Exception {
        // Arrange
        SponsoredPinUpdateDTO updateDTO = new SponsoredPinUpdateDTO();
        updateDTO.setBudget(BigDecimal.valueOf(0.001));

        // Act & Assert
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(businessService, never()).updateSponsoredPin(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PUT /business/sponsored-pins/{sponsoredId} - Failure - Sponsored Pin Not Found")
    void testUpdateSponsoredPin_NotFound() throws Exception {
        // Arrange
        SponsoredPinUpdateDTO updateDTO = new SponsoredPinUpdateDTO();
        updateDTO.setCampaignName("Updated Campaign");
        
        when(businessService.updateSponsoredPin(eq(SPONSORED_ID), eq(USER_ID), any(SponsoredPinUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("Sponsored pin not found"));

        // Act & Assert
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Sponsored pin not found"));

        verify(businessService, times(1)).updateSponsoredPin(eq(SPONSORED_ID), eq(USER_ID), any());
    }

    @Test
    @DisplayName("PUT /business/sponsored-pins/{sponsoredId}/pause - Failure - Sponsored Pin Not Found")
    void testPauseSponsoredPin_NotFound() throws Exception {
        // Arrange
        when(businessService.pauseSponsoredPin(SPONSORED_ID, USER_ID))
                .thenThrow(new ResourceNotFoundException("Sponsored pin not found"));

        // Act & Assert
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}/pause", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Sponsored pin not found"));

        verify(businessService, times(1)).pauseSponsoredPin(SPONSORED_ID, USER_ID);
    }

    @Test
    @DisplayName("PUT /business/sponsored-pins/{sponsoredId}/resume - Failure - Sponsored Pin Not Found")
    void testResumeSponsoredPin_NotFound() throws Exception {
        // Arrange
        when(businessService.resumeSponsoredPin(SPONSORED_ID, USER_ID))
                .thenThrow(new ResourceNotFoundException("Sponsored pin not found"));

        // Act & Assert
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}/resume", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Sponsored pin not found"));

        verify(businessService, times(1)).resumeSponsoredPin(SPONSORED_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /business/sponsored-pins/{sponsoredId} - Failure - Sponsored Pin Not Found")
    void testDeleteSponsoredPin_NotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Sponsored pin not found"))
                .when(businessService).deleteSponsoredPin(SPONSORED_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/business/sponsored-pins/{sponsoredId}", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Sponsored pin not found"));

        verify(businessService, times(1)).deleteSponsoredPin(SPONSORED_ID, USER_ID);
    }

    @Test
    @DisplayName("GET /business/analytics/{campaignId} - Success - Detailed Metrics")
    void testGetCampaignAnalytics_DetailedMetrics() throws Exception {
        // Arrange
        MetricsDTO detailedMetrics = new MetricsDTO();
        detailedMetrics.setImpressions(50000);
        detailedMetrics.setClicks(2500);
        detailedMetrics.setClickThroughRate(5.0);
        campaignAnalyticsDTO.setMetrics(detailedMetrics);
        
        when(businessService.getCampaignAnalytics(SPONSORED_ID))
                .thenReturn(campaignAnalyticsDTO);

        // Act & Assert
        mockMvc.perform(get("/business/analytics/{campaignId}", SPONSORED_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.impressions").value(50000))
                .andExpect(jsonPath("$.data.metrics.clicks").value(2500))
                .andExpect(jsonPath("$.data.metrics.clickThroughRate").value(5.0));

        verify(businessService, times(1)).getCampaignAnalytics(SPONSORED_ID);
    }

    @Test
    @DisplayName("Complete Business Flow - Profile, Showcase, Sponsored Pin, Analytics")
    void testCompleteBusinessFlow() throws Exception {
        // Arrange
        when(businessService.createBusinessProfile(eq(USER_ID), any(BusinessProfileCreateDTO.class)))
                .thenReturn(businessProfileResponseDTO);
        when(businessService.createShowcase(eq(BUSINESS_ID), any(BusinessShowcaseCreateDTO.class)))
                .thenReturn(showcaseResponseDTO);
        doNothing().when(businessService).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, 1);
        when(businessService.createSponsoredPin(eq(BUSINESS_ID), any(SponsoredPinCreateDTO.class)))
                .thenReturn(sponsoredPinResponseDTO);
        when(businessService.getCampaignAnalytics(SPONSORED_ID))
                .thenReturn(campaignAnalyticsDTO);

        // Create Business Profile
        mockMvc.perform(post("/business/profile")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessProfileCreateDTO)))
                .andExpect(status().isCreated());

        // Create Showcase
        mockMvc.perform(post("/business/showcases")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showcaseCreateDTO)))
                .andExpect(status().isCreated());

        // Add Pin to Showcase
        mockMvc.perform(post("/business/showcases/{showcaseId}/pins/{pinId}", SHOWCASE_ID, PIN_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .param("displayOrder", "1"))
                .andExpect(status().isOk());

        // Create Sponsored Pin
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andExpect(status().isCreated());

        // Get Analytics
        mockMvc.perform(get("/business/analytics/{campaignId}", SPONSORED_ID))
                .andExpect(status().isOk());

        verify(businessService, times(1)).createBusinessProfile(eq(USER_ID), any());
        verify(businessService, times(1)).createShowcase(eq(BUSINESS_ID), any());
        verify(businessService, times(1)).addPinToShowcase(SHOWCASE_ID, PIN_ID, USER_ID, 1);
        verify(businessService, times(1)).createSponsoredPin(eq(BUSINESS_ID), any());
        verify(businessService, times(1)).getCampaignAnalytics(SPONSORED_ID);
    }

    @Test
    @DisplayName("Sponsored Pin - Complete Campaign Lifecycle")
    void testSponsoredPinCompleteCampaignLifecycle() throws Exception {
        // Arrange
        when(businessService.createSponsoredPin(eq(BUSINESS_ID), any(SponsoredPinCreateDTO.class)))
                .thenReturn(sponsoredPinResponseDTO);
        when(businessService.pauseSponsoredPin(SPONSORED_ID, USER_ID))
                .thenReturn(sponsoredPinResponseDTO);
        when(businessService.resumeSponsoredPin(SPONSORED_ID, USER_ID))
                .thenReturn(sponsoredPinResponseDTO);
        SponsoredPinUpdateDTO updateDTO = new SponsoredPinUpdateDTO();
        updateDTO.setCampaignName("Updated Campaign Name");
        when(businessService.updateSponsoredPin(eq(SPONSORED_ID), eq(USER_ID), any(SponsoredPinUpdateDTO.class)))
                .thenReturn(sponsoredPinResponseDTO);
        doNothing().when(businessService).deleteSponsoredPin(SPONSORED_ID, USER_ID);

        // Create
        mockMvc.perform(post("/business/sponsored-pins")
                        .header(BUSINESS_ID_HEADER, BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sponsoredPinCreateDTO)))
                .andExpect(status().isCreated());

        // Update
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());

        // Pause
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}/pause", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Resume
        mockMvc.perform(put("/business/sponsored-pins/{sponsoredId}/resume", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        // Delete
        mockMvc.perform(delete("/business/sponsored-pins/{sponsoredId}", SPONSORED_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk());

        verify(businessService, times(1)).createSponsoredPin(eq(BUSINESS_ID), any());
        verify(businessService, times(1)).updateSponsoredPin(eq(SPONSORED_ID), eq(USER_ID), any());
        verify(businessService, times(1)).pauseSponsoredPin(SPONSORED_ID, USER_ID);
        verify(businessService, times(1)).resumeSponsoredPin(SPONSORED_ID, USER_ID);
        verify(businessService, times(1)).deleteSponsoredPin(SPONSORED_ID, USER_ID);
    }
}
