package com.infy.pinterest;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.infy.pinterest.dto.BusinessProfileCreateDTO;
import com.infy.pinterest.dto.BusinessProfileResponseDTO;
import com.infy.pinterest.dto.BusinessProfileUpdateDTO;
import com.infy.pinterest.dto.BusinessShowcaseCreateDTO;
import com.infy.pinterest.dto.BusinessShowcaseResponseDTO;
import com.infy.pinterest.dto.BusinessShowcaseUpdateDTO;
import com.infy.pinterest.dto.CampaignAnalyticsDTO;
import com.infy.pinterest.dto.MetricsDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.SpendingDTO;
import com.infy.pinterest.dto.SponsoredPinCreateDTO;
import com.infy.pinterest.dto.SponsoredPinResponseDTO;
import com.infy.pinterest.dto.SponsoredPinUpdateDTO;
import com.infy.pinterest.entity.Board;
import com.infy.pinterest.entity.BusinessProfile;
import com.infy.pinterest.entity.BusinessShowcase;
import com.infy.pinterest.entity.Pin;
import com.infy.pinterest.entity.ShowcasePin;
import com.infy.pinterest.entity.SponsoredPin;
import com.infy.pinterest.entity.SponsoredPin.Status;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.exception.BusinessProfileAlreadyExistsException;
import com.infy.pinterest.exception.BusinessProfileNotFoundException;
import com.infy.pinterest.exception.PinNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.repository.BoardRepository;
import com.infy.pinterest.repository.BusinessProfileRepository;
import com.infy.pinterest.repository.BusinessShowcaseRepository;
import com.infy.pinterest.repository.PinRepository;
import com.infy.pinterest.repository.ShowcasePinRepository;
import com.infy.pinterest.repository.SponsoredPinRepository;
import com.infy.pinterest.repository.UserRepository;
import com.infy.pinterest.service.BusinessService;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @Mock
    private BusinessShowcaseRepository showcaseRepository;

    @Mock
    private ShowcasePinRepository showcasePinRepository;

    @Mock
    private SponsoredPinRepository sponsoredPinRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PinRepository pinRepository;

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BusinessService businessService;

    private User user;
    private BusinessProfile businessProfile;
    private BusinessShowcase showcase;
    private SponsoredPin sponsoredPin;
    private Pin pin;
    private Board board;
    private ShowcasePin showcasePin;

    @BeforeEach
    void setUp() {
        // Setup user
        user = new User();
        user.setUserId("user-123");
        user.setUsername("business_user");
        user.setEmail("business@example.com");
        user.setFullName("Business User");
        user.setAccountType(User.AccountType.personal);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());

        // Setup business profile
        businessProfile = new BusinessProfile();
        businessProfile.setBusinessId("business-123");
        businessProfile.setUserId("user-123");
        businessProfile.setBusinessName("Test Business");
        businessProfile.setDescription("A test business");
        businessProfile.setWebsite("https://testbusiness.com");
        businessProfile.setCategory("Technology");
        businessProfile.setLogoUrl("https://example.com/logo.jpg");
        businessProfile.setContactEmail("contact@testbusiness.com");
        businessProfile.setFollowerCount(100);
        businessProfile.setCreatedAt(LocalDateTime.now());

        // Setup showcase
        showcase = new BusinessShowcase();
        showcase.setShowcaseId("showcase-123");
        showcase.setBusinessId("business-123");
        showcase.setTitle("Featured Products");
        showcase.setDescription("Our best products");
        showcase.setTheme("modern");
        showcase.setIsActive(true);
        showcase.setCreatedAt(LocalDateTime.now());

        // Setup pin
        pin = new Pin();
        pin.setPinId("pin-123");
        pin.setUserId("user-123");
        pin.setTitle("Test Pin");
        pin.setDescription("Test Description");
        pin.setImageUrl("https://example.com/pin.jpg");
        pin.setIsSponsored(false);
        pin.setCreatedAt(LocalDateTime.now());

        // Setup sponsored pin
        sponsoredPin = new SponsoredPin();
        sponsoredPin.setSponsoredId("sponsored-123");
        sponsoredPin.setPinId("pin-123");
        sponsoredPin.setBusinessId("business-123");
        sponsoredPin.setCampaignName("Spring Campaign");
        sponsoredPin.setBudget(new BigDecimal("1000.00"));
        sponsoredPin.setSpent(new BigDecimal("250.00"));
        sponsoredPin.setStatus(Status.ACTIVE);
        sponsoredPin.setImpressions(10000);
        sponsoredPin.setClicks(500);
        sponsoredPin.setSaves(150);
        sponsoredPin.setStartDate(LocalDate.now().minusDays(10));
        sponsoredPin.setEndDate(LocalDate.now().plusDays(20));
        sponsoredPin.setCreatedAt(LocalDateTime.now());

        // Setup board
        board = new Board();
        board.setBoardId("board-123");
        board.setUserId("user-123");
        board.setName("Test Board");
        board.setDescription("Test board description");
        board.setCreatedAt(LocalDateTime.now());

        // Setup showcase pin
        showcasePin = new ShowcasePin();
        showcasePin.setShowcasePinId("showcase-pin-123");
        showcasePin.setShowcaseId("showcase-123");
        showcasePin.setPinId("pin-123");
        showcasePin.setDisplayOrder(0);
        showcasePin.setAddedAt(LocalDateTime.now());
    }

    // ==================== CREATE BUSINESS PROFILE TESTS ====================

    @Test
    void testCreateBusinessProfile_Success() {
        // Arrange
        BusinessProfileCreateDTO createDTO = new BusinessProfileCreateDTO();
        createDTO.setBusinessName("New Business");
        createDTO.setDescription("Business Description");
        createDTO.setWebsite("https://newbusiness.com");
        createDTO.setCategory("Retail");
        createDTO.setLogoUrl("https://example.com/newlogo.jpg");
        createDTO.setContactEmail("contact@newbusiness.com");

        when(businessProfileRepository.existsByUserId("user-123")).thenReturn(false);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(businessProfile);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(boardRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        BusinessProfileResponseDTO result = businessService.createBusinessProfile("user-123", createDTO);

        // Assert
        assertNotNull(result);
        assertEquals("business-123", result.getBusinessId());
        assertEquals("user-123", result.getUserId());
        verify(businessProfileRepository).save(any(BusinessProfile.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateBusinessProfile_AlreadyExists() {
        // Arrange
        BusinessProfileCreateDTO createDTO = new BusinessProfileCreateDTO();
        createDTO.setBusinessName("New Business");

        when(businessProfileRepository.existsByUserId("user-123")).thenReturn(true);

        // Act & Assert
        BusinessProfileAlreadyExistsException exception = assertThrows(
            BusinessProfileAlreadyExistsException.class,
            () -> businessService.createBusinessProfile("user-123", createDTO)
        );

        assertEquals("Business profile already exists for this user", exception.getMessage());
        verify(businessProfileRepository, never()).save(any(BusinessProfile.class));
    }

    @Test
    void testCreateBusinessProfile_UserNotFound() {
        // Arrange
        BusinessProfileCreateDTO createDTO = new BusinessProfileCreateDTO();
        createDTO.setBusinessName("New Business");

        when(businessProfileRepository.existsByUserId("user-123")).thenReturn(false);
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.createBusinessProfile("user-123", createDTO)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testCreateBusinessProfile_UpdatesUserAccountType() {
        // Arrange
        BusinessProfileCreateDTO createDTO = new BusinessProfileCreateDTO();
        createDTO.setBusinessName("New Business");

        when(businessProfileRepository.existsByUserId("user-123")).thenReturn(false);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(businessProfile);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(boardRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        businessService.createBusinessProfile("user-123", createDTO);

        // Assert
        assertEquals(User.AccountType.business, user.getAccountType());
        verify(userRepository).save(user);
    }

    // ==================== GET BUSINESS PROFILE TESTS ====================

    @Test
    void testGetBusinessProfile_Success() {
        // Arrange
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(boardRepository.findByUserId("user-123")).thenReturn(Arrays.asList(board));

        // Act
        BusinessProfileResponseDTO result = businessService.getBusinessProfile("business-123");

        // Assert
        assertNotNull(result);
        assertEquals("business-123", result.getBusinessId());
        assertEquals("Test Business", result.getBusinessName());
        assertEquals(100, result.getFollowerCount());
        assertEquals(1, result.getBoards().size());
    }

    @Test
    void testGetBusinessProfile_NotFound() {
        // Arrange
        when(businessProfileRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        BusinessProfileNotFoundException exception = assertThrows(
            BusinessProfileNotFoundException.class,
            () -> businessService.getBusinessProfile("non-existent")
        );

        assertEquals("Business profile not found", exception.getMessage());
    }

    @Test
    void testGetBusinessProfile_WithBoards() {
        // Arrange
        Board board2 = new Board();
        board2.setBoardId("board-456");
        board2.setUserId("user-123");
        board2.setName("Second Board");

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(boardRepository.findByUserId("user-123")).thenReturn(Arrays.asList(board, board2));

        // Act
        BusinessProfileResponseDTO result = businessService.getBusinessProfile("business-123");

        // Assert
        assertEquals(2, result.getBoards().size());
        assertEquals("board-123", result.getBoards().get(0).getBoardId());
        assertEquals("Test Board", result.getBoards().get(0).getBoardName());
    }

    // ==================== CREATE SHOWCASE TESTS ====================

    @Test
    void testCreateShowcase_Success() {
        // Arrange
        BusinessShowcaseCreateDTO createDTO = new BusinessShowcaseCreateDTO();
        createDTO.setTitle("New Showcase");
        createDTO.setDescription("Showcase description");
        createDTO.setTheme("elegant");

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcaseRepository.save(any(BusinessShowcase.class))).thenReturn(showcase);
        when(showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc("showcase-123"))
            .thenReturn(new ArrayList<>());

        // Act
        BusinessShowcaseResponseDTO result = businessService.createShowcase("business-123", createDTO);

        // Assert
        assertNotNull(result);
        assertEquals("showcase-123", result.getShowcaseId());
        assertEquals("business-123", result.getBusinessId());
        verify(showcaseRepository).save(any(BusinessShowcase.class));
    }

    @Test
    void testCreateShowcase_BusinessProfileNotFound() {
        // Arrange
        BusinessShowcaseCreateDTO createDTO = new BusinessShowcaseCreateDTO();
        createDTO.setTitle("New Showcase");

        when(businessProfileRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        BusinessProfileNotFoundException exception = assertThrows(
            BusinessProfileNotFoundException.class,
            () -> businessService.createShowcase("non-existent", createDTO)
        );

        assertEquals("Business profile not found", exception.getMessage());
    }

    @Test
    void testCreateShowcase_WithBusinessDetails() {
        // Arrange
        BusinessShowcaseCreateDTO createDTO = new BusinessShowcaseCreateDTO();
        createDTO.setTitle("New Showcase");

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcaseRepository.save(any(BusinessShowcase.class))).thenReturn(showcase);
        when(showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc("showcase-123"))
            .thenReturn(new ArrayList<>());

        // Act
        BusinessShowcaseResponseDTO result = businessService.createShowcase("business-123", createDTO);

        // Assert
        assertEquals("Test Business", result.getBusinessName());
        assertEquals("https://example.com/logo.jpg", result.getLogoUrl());
        assertEquals(100, result.getFollowerCount());
    }

    // ==================== GET ACTIVE SHOWCASES TESTS ====================

    @Test
    void testGetActiveShowcases_Success() {
        // Arrange
        List<BusinessShowcase> showcases = Arrays.asList(showcase);
        Page<BusinessShowcase> showcasePage = new PageImpl<>(showcases, PageRequest.of(0, 10), 1);

        when(showcaseRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(showcasePage);
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc("showcase-123"))
            .thenReturn(new ArrayList<>());

        // Act
        PaginatedResponse<BusinessShowcaseResponseDTO> result = businessService.getActiveShowcases(0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(0, result.getPagination().getCurrentPage());
        assertEquals(1, result.getPagination().getTotalPages());
    }

    @Test
    void testGetActiveShowcases_EmptyList() {
        // Arrange
        Page<BusinessShowcase> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(showcaseRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<BusinessShowcaseResponseDTO> result = businessService.getActiveShowcases(0, 10);

        // Assert
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPagination().getTotalPages());
    }

    @Test
    void testGetActiveShowcases_WithPagination() {
        // Arrange
        List<BusinessShowcase> showcases = Arrays.asList(showcase);
        Page<BusinessShowcase> showcasePage = new PageImpl<>(showcases, PageRequest.of(1, 5), 20);

        when(showcaseRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(showcasePage);
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc("showcase-123"))
            .thenReturn(new ArrayList<>());

        // Act
        PaginatedResponse<BusinessShowcaseResponseDTO> result = businessService.getActiveShowcases(1, 5);

        // Assert
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(4, result.getPagination().getTotalPages());
        assertEquals(20L, result.getPagination().getTotalItems());
    }

    // ==================== CREATE SPONSORED PIN TESTS ====================

    @Test
    void testCreateSponsoredPin_Success() {
        // Arrange
        SponsoredPinCreateDTO createDTO = new SponsoredPinCreateDTO();
        createDTO.setPinId("pin-123");
        createDTO.setCampaignName("Summer Campaign");
        createDTO.setBudget(new BigDecimal("500.00"));
        createDTO.setStartDate(LocalDate.now());
        createDTO.setEndDate(LocalDate.now().plusDays(30));

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));
        when(sponsoredPinRepository.save(any(SponsoredPin.class))).thenReturn(sponsoredPin);
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        SponsoredPinResponseDTO result = businessService.createSponsoredPin("business-123", createDTO);

        // Assert
        assertNotNull(result);
        assertEquals("sponsored-123", result.getSponsoredId());
        assertTrue(pin.getIsSponsored());
        verify(sponsoredPinRepository).save(any(SponsoredPin.class));
        verify(pinRepository).save(pin);
    }

    @Test
    void testCreateSponsoredPin_BusinessProfileNotFound() {
        // Arrange
        SponsoredPinCreateDTO createDTO = new SponsoredPinCreateDTO();
        createDTO.setPinId("pin-123");

        when(businessProfileRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        BusinessProfileNotFoundException exception = assertThrows(
            BusinessProfileNotFoundException.class,
            () -> businessService.createSponsoredPin("non-existent", createDTO)
        );

        assertEquals("Business profile not found", exception.getMessage());
    }

    @Test
    void testCreateSponsoredPin_PinNotFound() {
        // Arrange
        SponsoredPinCreateDTO createDTO = new SponsoredPinCreateDTO();
        createDTO.setPinId("non-existent-pin");

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(pinRepository.findById("non-existent-pin")).thenReturn(Optional.empty());

        // Act & Assert
        PinNotFoundException exception = assertThrows(
            PinNotFoundException.class,
            () -> businessService.createSponsoredPin("business-123", createDTO)
        );

        assertEquals("Pin not found", exception.getMessage());
    }

    @Test
    void testCreateSponsoredPin_InitializedWithZeroSpent() {
        // Arrange
        SponsoredPinCreateDTO createDTO = new SponsoredPinCreateDTO();
        createDTO.setPinId("pin-123");
        createDTO.setCampaignName("Test Campaign");
        createDTO.setBudget(new BigDecimal("1000.00"));
        createDTO.setStartDate(LocalDate.now());
        createDTO.setEndDate(LocalDate.now().plusDays(30));

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));
        when(sponsoredPinRepository.save(any(SponsoredPin.class))).thenAnswer(invocation -> {
            SponsoredPin saved = invocation.getArgument(0);
            assertEquals(BigDecimal.ZERO, saved.getSpent());
            assertEquals(Status.ACTIVE, saved.getStatus());
            return sponsoredPin;
        });
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);

        // Act
        businessService.createSponsoredPin("business-123", createDTO);

        // Assert
        verify(sponsoredPinRepository).save(any(SponsoredPin.class));
    }

    // ==================== GET SPONSORED PINS TESTS ====================

    @Test
    void testGetSponsoredPins_Success() {
        // Arrange
        List<SponsoredPin> sponsoredPins = Arrays.asList(sponsoredPin);
        Page<SponsoredPin> sponsoredPage = new PageImpl<>(sponsoredPins, PageRequest.of(0, 10), 1);

        when(sponsoredPinRepository.findByBusinessId(eq("business-123"), any(Pageable.class)))
            .thenReturn(sponsoredPage);
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));

        // Act
        PaginatedResponse<SponsoredPinResponseDTO> result = businessService.getSponsoredPins("business-123", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("Spring Campaign", result.getData().get(0).getCampaignName());
    }

    @Test
    void testGetSponsoredPins_EmptyList() {
        // Arrange
        Page<SponsoredPin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(sponsoredPinRepository.findByBusinessId(eq("business-123"), any(Pageable.class)))
            .thenReturn(emptyPage);

        // Act
        PaginatedResponse<SponsoredPinResponseDTO> result = businessService.getSponsoredPins("business-123", 0, 10);

        // Assert
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testGetSponsoredPins_WithPinDetails() {
        // Arrange
        List<SponsoredPin> sponsoredPins = Arrays.asList(sponsoredPin);
        Page<SponsoredPin> sponsoredPage = new PageImpl<>(sponsoredPins, PageRequest.of(0, 10), 1);

        when(sponsoredPinRepository.findByBusinessId(eq("business-123"), any(Pageable.class)))
            .thenReturn(sponsoredPage);
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));

        // Act
        PaginatedResponse<SponsoredPinResponseDTO> result = businessService.getSponsoredPins("business-123", 0, 10);

        // Assert
        SponsoredPinResponseDTO dto = result.getData().get(0);
        assertNotNull(dto.getPin());
        assertEquals("pin-123", dto.getPin().getPinId());
        assertEquals("Test Pin", dto.getPin().getTitle());
    }

    // ==================== GET ACTIVE SPONSORED FEED TESTS ====================

    @Test
    void testGetActiveSponsoredFeed_Success() {
        // Arrange
        List<SponsoredPin> sponsoredPins = Arrays.asList(sponsoredPin);
        Page<SponsoredPin> sponsoredPage = new PageImpl<>(sponsoredPins, PageRequest.of(0, 10), 1);

        when(sponsoredPinRepository.findActiveSponsoredPins(any(LocalDate.class), any(Pageable.class)))
            .thenReturn(sponsoredPage);
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));

        // Act
        PaginatedResponse<SponsoredPinResponseDTO> result = businessService.getActiveSponsoredFeed(0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("ACTIVE", result.getData().get(0).getStatus());
    }

    @Test
    void testGetActiveSponsoredFeed_OnlyActiveWithinDateRange() {
        // Arrange
        List<SponsoredPin> sponsoredPins = Arrays.asList(sponsoredPin);
        Page<SponsoredPin> sponsoredPage = new PageImpl<>(sponsoredPins, PageRequest.of(0, 10), 1);

        when(sponsoredPinRepository.findActiveSponsoredPins(any(LocalDate.class), any(Pageable.class)))
            .thenReturn(sponsoredPage);
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));

        // Act
        businessService.getActiveSponsoredFeed(0, 10);

        // Assert
        verify(sponsoredPinRepository).findActiveSponsoredPins(any(LocalDate.class), any(Pageable.class));
    }

    // ==================== GET CAMPAIGN ANALYTICS TESTS ====================

    @Test
    void testGetCampaignAnalytics_Success() {
        // Arrange
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));

        // Act
        CampaignAnalyticsDTO result = businessService.getCampaignAnalytics("sponsored-123");

        // Assert
        assertNotNull(result);
        assertEquals("sponsored-123", result.getCampaignId());
        assertEquals("Spring Campaign", result.getCampaignName());
        assertNotNull(result.getMetrics());
        assertNotNull(result.getDemographics());
        assertNotNull(result.getSpending());
        assertNotNull(result.getTimeline());
    }

    @Test
    void testGetCampaignAnalytics_NotFound() {
        // Arrange
        when(sponsoredPinRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.getCampaignAnalytics("non-existent")
        );

        assertEquals("Campaign not found", exception.getMessage());
    }

    @Test
    void testGetCampaignAnalytics_MetricsCalculation() {
        // Arrange
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));

        // Act
        CampaignAnalyticsDTO result = businessService.getCampaignAnalytics("sponsored-123");

        // Assert
        MetricsDTO metrics = result.getMetrics();
        assertEquals(10000, metrics.getImpressions());
        assertEquals(500, metrics.getClicks());
        assertEquals(150, metrics.getSaves());
        assertEquals(650, metrics.getEngagement()); // clicks + saves
        assertEquals(5.0, metrics.getClickThroughRate(), 0.01); // (500/10000) * 100
        assertEquals(6.5, metrics.getEngagementRate(), 0.01); // (650/10000) * 100
    }

    @Test
    void testGetCampaignAnalytics_SpendingCalculation() {
        // Arrange
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));

        // Act
        CampaignAnalyticsDTO result = businessService.getCampaignAnalytics("sponsored-123");

        // Assert
        SpendingDTO spending = result.getSpending();
        assertEquals(new BigDecimal("1000.00"), spending.getBudget());
        assertEquals(new BigDecimal("250.00"), spending.getSpent());
        assertEquals(new BigDecimal("750.00"), spending.getRemaining());
        assertEquals(new BigDecimal("0.50"), spending.getCostPerClick()); // 250/500
        assertEquals(new BigDecimal("1.67"), spending.getCostPerSave()); // 250/150
    }

    @Test
    void testGetCampaignAnalytics_WithZeroImpressions() {
        // Arrange
        sponsoredPin.setImpressions(0);
        sponsoredPin.setClicks(0);
        sponsoredPin.setSaves(0);
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));

        // Act
        CampaignAnalyticsDTO result = businessService.getCampaignAnalytics("sponsored-123");

        // Assert
        MetricsDTO metrics = result.getMetrics();
        assertEquals(0.0, metrics.getClickThroughRate());
        assertEquals(0.0, metrics.getEngagementRate());
    }

    // ==================== UPDATE BUSINESS PROFILE TESTS ====================

    @Test
    void testUpdateBusinessProfile_Success() {
        // Arrange
        BusinessProfileUpdateDTO updateDTO = new BusinessProfileUpdateDTO();
        updateDTO.setBusinessName("Updated Business");
        updateDTO.setDescription("Updated description");

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(businessProfile);
        when(boardRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        BusinessProfileResponseDTO result = businessService.updateBusinessProfile("business-123", "user-123", updateDTO);

        // Assert
        assertNotNull(result);
        verify(businessProfileRepository).save(businessProfile);
    }

    @Test
    void testUpdateBusinessProfile_NotFound() {
        // Arrange
        BusinessProfileUpdateDTO updateDTO = new BusinessProfileUpdateDTO();
        when(businessProfileRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        BusinessProfileNotFoundException exception = assertThrows(
            BusinessProfileNotFoundException.class,
            () -> businessService.updateBusinessProfile("non-existent", "user-123", updateDTO)
        );

        assertEquals("Business profile not found", exception.getMessage());
    }

    @Test
    void testUpdateBusinessProfile_UnauthorizedUser() {
        // Arrange
        BusinessProfileUpdateDTO updateDTO = new BusinessProfileUpdateDTO();
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.updateBusinessProfile("business-123", "wrong-user", updateDTO)
        );

        assertEquals("Unauthorized to update this business profile", exception.getMessage());
    }

    @Test
    void testUpdateBusinessProfile_PartialUpdate() {
        // Arrange
        BusinessProfileUpdateDTO updateDTO = new BusinessProfileUpdateDTO();
        updateDTO.setBusinessName("New Name Only");

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(businessProfile);
        when(boardRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        businessService.updateBusinessProfile("business-123", "user-123", updateDTO);

        // Assert
        assertEquals("New Name Only", businessProfile.getBusinessName());
        verify(businessProfileRepository).save(businessProfile);
    }

    // ==================== UPDATE SHOWCASE TESTS ====================

    @Test
    void testUpdateShowcase_Success() {
        // Arrange
        BusinessShowcaseUpdateDTO updateDTO = new BusinessShowcaseUpdateDTO();
        updateDTO.setTitle("Updated Showcase");
        updateDTO.setIsActive(false);

        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcaseRepository.save(any(BusinessShowcase.class))).thenReturn(showcase);
        when(showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc("showcase-123"))
            .thenReturn(new ArrayList<>());

        // Act
        BusinessShowcaseResponseDTO result = businessService.updateShowcase("showcase-123", "user-123", updateDTO);

        // Assert
        assertNotNull(result);
        verify(showcaseRepository).save(showcase);
    }

    @Test
    void testUpdateShowcase_NotFound() {
        // Arrange
        BusinessShowcaseUpdateDTO updateDTO = new BusinessShowcaseUpdateDTO();
        when(showcaseRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.updateShowcase("non-existent", "user-123", updateDTO)
        );

        assertEquals("Showcase not found", exception.getMessage());
    }

    @Test
    void testUpdateShowcase_UnauthorizedUser() {
        // Arrange
        BusinessShowcaseUpdateDTO updateDTO = new BusinessShowcaseUpdateDTO();
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.updateShowcase("showcase-123", "wrong-user", updateDTO)
        );

        assertEquals("Unauthorized to update this showcase", exception.getMessage());
    }

    // ==================== ADD PIN TO SHOWCASE TESTS ====================

    @Test
    void testAddPinToShowcase_Success() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));
        when(showcasePinRepository.existsByShowcaseIdAndPinId("showcase-123", "pin-123")).thenReturn(false);
        when(showcasePinRepository.countByShowcaseId("showcase-123")).thenReturn(5);
        when(showcasePinRepository.save(any(ShowcasePin.class))).thenReturn(showcasePin);

        // Act
        businessService.addPinToShowcase("showcase-123", "pin-123", "user-123", null);

        // Assert
        verify(showcasePinRepository).save(any(ShowcasePin.class));
    }

    @Test
    void testAddPinToShowcase_WithCustomDisplayOrder() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));
        when(showcasePinRepository.existsByShowcaseIdAndPinId("showcase-123", "pin-123")).thenReturn(false);
        when(showcasePinRepository.save(any(ShowcasePin.class))).thenAnswer(invocation -> {
            ShowcasePin saved = invocation.getArgument(0);
            assertEquals(3, saved.getDisplayOrder());
            return showcasePin;
        });

        // Act
        businessService.addPinToShowcase("showcase-123", "pin-123", "user-123", 3);

        // Assert
        verify(showcasePinRepository).save(any(ShowcasePin.class));
    }

    @Test
    void testAddPinToShowcase_PinAlreadyExists() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));
        when(showcasePinRepository.existsByShowcaseIdAndPinId("showcase-123", "pin-123")).thenReturn(true);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.addPinToShowcase("showcase-123", "pin-123", "user-123", null)
        );

        assertEquals("Pin is already in this showcase", exception.getMessage());
    }

    @Test
    void testAddPinToShowcase_UnauthorizedUser() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.addPinToShowcase("showcase-123", "pin-123", "wrong-user", null)
        );

        assertEquals("Unauthorized to modify this showcase", exception.getMessage());
    }

    // ==================== REMOVE PIN FROM SHOWCASE TESTS ====================

    @Test
    void testRemovePinFromShowcase_Success() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcasePinRepository.findByShowcaseIdAndPinId("showcase-123", "pin-123"))
            .thenReturn(Optional.of(showcasePin));
        doNothing().when(showcasePinRepository).delete(showcasePin);

        // Act
        businessService.removePinFromShowcase("showcase-123", "pin-123", "user-123");

        // Assert
        verify(showcasePinRepository).delete(showcasePin);
    }

    @Test
    void testRemovePinFromShowcase_PinNotFound() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcasePinRepository.findByShowcaseIdAndPinId("showcase-123", "non-existent-pin"))
            .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.removePinFromShowcase("showcase-123", "non-existent-pin", "user-123")
        );

        assertEquals("Pin not found in showcase", exception.getMessage());
    }

    // ==================== UPDATE SPONSORED PIN TESTS ====================

    @Test
    void testUpdateSponsoredPin_Success() {
        // Arrange
        SponsoredPinUpdateDTO updateDTO = new SponsoredPinUpdateDTO();
        updateDTO.setCampaignName("Updated Campaign");
        updateDTO.setBudget(new BigDecimal("2000.00"));

        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(sponsoredPinRepository.save(any(SponsoredPin.class))).thenReturn(sponsoredPin);
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));

        // Act
        SponsoredPinResponseDTO result = businessService.updateSponsoredPin("sponsored-123", "user-123", updateDTO);

        // Assert
        assertNotNull(result);
        verify(sponsoredPinRepository).save(sponsoredPin);
    }

    @Test
    void testUpdateSponsoredPin_NotFound() {
        // Arrange
        SponsoredPinUpdateDTO updateDTO = new SponsoredPinUpdateDTO();
        when(sponsoredPinRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.updateSponsoredPin("non-existent", "user-123", updateDTO)
        );

        assertEquals("Sponsored pin not found", exception.getMessage());
    }

    // ==================== PAUSE SPONSORED PIN TESTS ====================

    @Test
    void testPauseSponsoredPin_Success() {
        // Arrange
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(sponsoredPinRepository.save(any(SponsoredPin.class))).thenReturn(sponsoredPin);
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));

        // Act
        SponsoredPinResponseDTO result = businessService.pauseSponsoredPin("sponsored-123", "user-123");

        // Assert
        assertNotNull(result);
        assertEquals(Status.PAUSED, sponsoredPin.getStatus());
        verify(sponsoredPinRepository).save(sponsoredPin);
    }

    @Test
    void testPauseSponsoredPin_UnauthorizedUser() {
        // Arrange
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.pauseSponsoredPin("sponsored-123", "wrong-user")
        );

        assertEquals("Unauthorized to pause this sponsored pin", exception.getMessage());
    }

    // ==================== RESUME SPONSORED PIN TESTS ====================

    @Test
    void testResumeSponsoredPin_Success() {
        // Arrange
        sponsoredPin.setStatus(Status.PAUSED);
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(sponsoredPinRepository.save(any(SponsoredPin.class))).thenReturn(sponsoredPin);
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));

        // Act
        SponsoredPinResponseDTO result = businessService.resumeSponsoredPin("sponsored-123", "user-123");

        // Assert
        assertNotNull(result);
        assertEquals(Status.ACTIVE, sponsoredPin.getStatus());
        verify(sponsoredPinRepository).save(sponsoredPin);
    }

    // ==================== DELETE SPONSORED PIN TESTS ====================

    @Test
    void testDeleteSponsoredPin_Success() {
        // Arrange
        pin.setIsSponsored(true);
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));
        when(pinRepository.save(any(Pin.class))).thenReturn(pin);
        doNothing().when(sponsoredPinRepository).delete(sponsoredPin);

        // Act
        businessService.deleteSponsoredPin("sponsored-123", "user-123");

        // Assert
        assertFalse(pin.getIsSponsored());
        verify(pinRepository).save(pin);
        verify(sponsoredPinRepository).delete(sponsoredPin);
    }

    @Test
    void testDeleteSponsoredPin_UnauthorizedUser() {
        // Arrange
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.deleteSponsoredPin("sponsored-123", "wrong-user")
        );

        assertEquals("Unauthorized to delete this sponsored pin", exception.getMessage());
    }

    // ==================== DELETE SHOWCASE TESTS ====================

    @Test
    void testDeleteShowcase_Success() {
        // Arrange
        List<ShowcasePin> showcasePins = Arrays.asList(showcasePin);
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc("showcase-123")).thenReturn(showcasePins);
        doNothing().when(showcasePinRepository).deleteAll(showcasePins);
        doNothing().when(showcaseRepository).delete(showcase);

        // Act
        businessService.deleteShowcase("showcase-123", "user-123");

        // Assert
        verify(showcasePinRepository).deleteAll(showcasePins);
        verify(showcaseRepository).delete(showcase);
    }

    @Test
    void testDeleteShowcase_UnauthorizedUser() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> businessService.deleteShowcase("showcase-123", "wrong-user")
        );

        assertEquals("Unauthorized to delete this showcase", exception.getMessage());
    }

    // ==================== EDGE CASES AND INTEGRATION TESTS ====================

    @Test
    void testCreateBusinessProfile_WithAllFields() {
        // Arrange
        BusinessProfileCreateDTO createDTO = new BusinessProfileCreateDTO();
        createDTO.setBusinessName("Complete Business");
        createDTO.setDescription("Full description");
        createDTO.setWebsite("https://complete.com");
        createDTO.setCategory("E-commerce");
        createDTO.setLogoUrl("https://logo.jpg");
        createDTO.setContactEmail("contact@complete.com");

        when(businessProfileRepository.existsByUserId("user-123")).thenReturn(false);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(businessProfile);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(boardRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        BusinessProfileResponseDTO result = businessService.createBusinessProfile("user-123", createDTO);

        // Assert
        assertNotNull(result);
        verify(businessProfileRepository).save(any(BusinessProfile.class));
    }

    @Test
    void testGetCampaignAnalytics_WithDemographics() {
        // Arrange
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));

        // Act
        CampaignAnalyticsDTO result = businessService.getCampaignAnalytics("sponsored-123");

        // Assert
        assertNotNull(result.getDemographics());
        assertEquals(3, result.getDemographics().getAgeGroups().size());
        assertEquals(3, result.getDemographics().getTopLocations().size());
    }

    @Test
    void testGetCampaignAnalytics_WithTimeline() {
        // Arrange
        when(sponsoredPinRepository.findById("sponsored-123")).thenReturn(Optional.of(sponsoredPin));

        // Act
        CampaignAnalyticsDTO result = businessService.getCampaignAnalytics("sponsored-123");

        // Assert
        assertNotNull(result.getTimeline());
        assertEquals(3, result.getTimeline().size());
    }

    @Test
    void testUpdateBusinessProfile_AllFields() {
        // Arrange
        BusinessProfileUpdateDTO updateDTO = new BusinessProfileUpdateDTO();
        updateDTO.setBusinessName("All Updated");
        updateDTO.setDescription("New desc");
        updateDTO.setWebsite("https://new.com");
        updateDTO.setCategory("New Category");
        updateDTO.setLogoUrl("https://newlogo.jpg");
        updateDTO.setContactEmail("new@email.com");

        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(businessProfile);
        when(boardRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        businessService.updateBusinessProfile("business-123", "user-123", updateDTO);

        // Assert
        assertEquals("All Updated", businessProfile.getBusinessName());
        assertEquals("New desc", businessProfile.getDescription());
        assertEquals("https://new.com", businessProfile.getWebsite());
    }

    @Test
    void testShowcaseWithPins() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc("showcase-123"))
            .thenReturn(Arrays.asList(showcasePin));
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));
        when(boardRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        // Act
        BusinessProfileResponseDTO profileResult = businessService.getBusinessProfile("business-123");

        // Assert
        assertNotNull(profileResult);
    }

    @Test
    void testGetSponsoredPins_WithPagination() {
        // Arrange
        List<SponsoredPin> sponsoredPins = Arrays.asList(sponsoredPin);
        Page<SponsoredPin> sponsoredPage = new PageImpl<>(sponsoredPins, PageRequest.of(2, 5), 20);

        when(sponsoredPinRepository.findByBusinessId(eq("business-123"), any(Pageable.class)))
            .thenReturn(sponsoredPage);
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));

        // Act
        PaginatedResponse<SponsoredPinResponseDTO> result = businessService.getSponsoredPins("business-123", 2, 5);

        // Assert
        assertEquals(2, result.getPagination().getCurrentPage());
        assertEquals(4, result.getPagination().getTotalPages());
        assertEquals(20L, result.getPagination().getTotalItems());
    }

    @Test
    void testAddPinToShowcase_DefaultDisplayOrder() {
        // Arrange
        when(showcaseRepository.findById("showcase-123")).thenReturn(Optional.of(showcase));
        when(businessProfileRepository.findById("business-123")).thenReturn(Optional.of(businessProfile));
        when(pinRepository.findById("pin-123")).thenReturn(Optional.of(pin));
        when(showcasePinRepository.existsByShowcaseIdAndPinId("showcase-123", "pin-123")).thenReturn(false);
        when(showcasePinRepository.countByShowcaseId("showcase-123")).thenReturn(10);
        when(showcasePinRepository.save(any(ShowcasePin.class))).thenAnswer(invocation -> {
            ShowcasePin saved = invocation.getArgument(0);
            assertEquals(10, saved.getDisplayOrder());
            return showcasePin;
        });

        // Act
        businessService.addPinToShowcase("showcase-123", "pin-123", "user-123", null);

        // Assert
        verify(showcasePinRepository).save(any(ShowcasePin.class));
    }
}
