package com.infy.pinterest.service;

import com.infy.pinterest.dto.*;
import com.infy.pinterest.entity.*;
import com.infy.pinterest.exception.BusinessProfileAlreadyExistsException;
import com.infy.pinterest.exception.BusinessProfileNotFoundException;
import com.infy.pinterest.exception.PinNotFoundException;
import com.infy.pinterest.exception.ResourceNotFoundException;
import com.infy.pinterest.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
@Service
@Slf4j
public class BusinessService {

    private final BusinessProfileRepository businessProfileRepository;
    private final BusinessShowcaseRepository showcaseRepository;
    private final ShowcasePinRepository showcasePinRepository;
    private final SponsoredPinRepository sponsoredPinRepository;
    private final UserRepository userRepository;
    private final PinRepository pinRepository;
    private final BoardRepository boardRepository;

    @Autowired
    public BusinessService(BusinessProfileRepository businessProfileRepository,
                          BusinessShowcaseRepository showcaseRepository,
                          ShowcasePinRepository showcasePinRepository,
                          SponsoredPinRepository sponsoredPinRepository,
                          UserRepository userRepository,
                          PinRepository pinRepository,
                          BoardRepository boardRepository) {
        this.businessProfileRepository = businessProfileRepository;
        this.showcaseRepository = showcaseRepository;
        this.showcasePinRepository = showcasePinRepository;
        this.sponsoredPinRepository = sponsoredPinRepository;
        this.userRepository = userRepository;
        this.pinRepository = pinRepository;
        this.boardRepository = boardRepository;
    }

    @Transactional
    public BusinessProfileResponseDTO createBusinessProfile(String userId,
                                                            BusinessProfileCreateDTO profileDTO) {
        log.info("Creating business profile for user: {}", userId);

        if (businessProfileRepository.existsByUserId(userId)) {
            throw new BusinessProfileAlreadyExistsException("Business profile already exists for this user");
        } User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BusinessProfile profile = new BusinessProfile();
        profile.setUserId(userId);
        profile.setBusinessName(profileDTO.getBusinessName());
        profile.setDescription(profileDTO.getDescription());
        profile.setWebsite(profileDTO.getWebsite());
        profile.setCategory(profileDTO.getCategory());
        profile.setLogoUrl(profileDTO.getLogoUrl());
        profile.setContactEmail(profileDTO.getContactEmail());
        profile.setFollowerCount(0);

        BusinessProfile savedProfile = businessProfileRepository.save(profile);

        // Update user account type to business
        user.setAccountType(User.AccountType.business);
        userRepository.save(user);

        log.info("Business profile created: {}", savedProfile.getBusinessId());
        return buildBusinessProfileResponse(savedProfile);
    }

    public PaginatedResponse<BusinessProfileResponseDTO> getAllBusinesses(int page, int size) {
        log.info("Fetching all business profiles");
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BusinessProfile> businessPage = businessProfileRepository.findAll(pageable);

        List<BusinessProfileResponseDTO> businesses = businessPage.getContent().stream()
                .map(this::buildBusinessProfileResponse)
                .toList();

        PaginationDTO pagination = new PaginationDTO(
                businessPage.getNumber(),
                businessPage.getTotalPages(),
                businessPage.getTotalElements(),
                businessPage.getSize(),
                businessPage.hasNext(),
                businessPage.hasPrevious()
        );

        return new PaginatedResponse<>(businesses, pagination);
    }

    public BusinessProfileResponseDTO getBusinessProfile(String businessId) {
        BusinessProfile profile = businessProfileRepository.findById(businessId)
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        return buildBusinessProfileResponse(profile);
    }

    @Transactional
    public BusinessShowcaseResponseDTO createShowcase(String businessId,
                                                      BusinessShowcaseCreateDTO showcaseDTO) {
        log.info("Creating showcase for business: {}", businessId);

        BusinessProfile profile = businessProfileRepository.findById(businessId)
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        BusinessShowcase showcase = new BusinessShowcase();
        showcase.setBusinessId(businessId);
        showcase.setTitle(showcaseDTO.getTitle());
        showcase.setDescription(showcaseDTO.getDescription());
        showcase.setTheme(showcaseDTO.getTheme());
        showcase.setIsActive(true);

        BusinessShowcase savedShowcase = showcaseRepository.save(showcase);
        return buildShowcaseResponse(savedShowcase, profile); }

    public PaginatedResponse<BusinessShowcaseResponseDTO> getActiveShowcases(int page, int size)
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,
                "createdAt"));
        Page<BusinessShowcase> showcasePage = showcaseRepository.findByIsActive(true, pageable);

        List<BusinessShowcaseResponseDTO> showcases = showcasePage.getContent().stream()
                .map(showcase -> {
                    BusinessProfile profile =
                            businessProfileRepository.findById(showcase.getBusinessId()).orElse(null);
                    return buildShowcaseResponse(showcase, profile);
                })
                .toList();

        PaginationDTO pagination = new PaginationDTO(
                showcasePage.getNumber(),
                showcasePage.getTotalPages(),
                showcasePage.getTotalElements(),
                showcasePage.getSize(),
                showcasePage.hasNext(),
                showcasePage.hasPrevious()
        );

        return new PaginatedResponse<>(showcases, pagination);
    }
    @Transactional
    public SponsoredPinResponseDTO createSponsoredPin(String businessId, SponsoredPinCreateDTO
            sponsoredDTO) {
        log.info("Creating sponsored pin for business: {}", businessId);

        businessProfileRepository.findById(businessId)
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

                        Pin pin = pinRepository.findById(sponsoredDTO.getPinId())
                        .orElseThrow(() -> new PinNotFoundException("Pin not found"));

        SponsoredPin sponsored = new SponsoredPin();
        sponsored.setPinId(sponsoredDTO.getPinId());
        sponsored.setBusinessId(businessId);
        sponsored.setCampaignName(sponsoredDTO.getCampaignName());
        sponsored.setBudget(sponsoredDTO.getBudget());
        sponsored.setSpent(BigDecimal.ZERO);
        sponsored.setStatus(SponsoredPin.Status.ACTIVE);
        sponsored.setStartDate(sponsoredDTO.getStartDate());
        sponsored.setEndDate(sponsoredDTO.getEndDate());

        SponsoredPin savedSponsored = sponsoredPinRepository.save(sponsored);

        // Mark pin as sponsored
        pin.setIsSponsored(true);
        pinRepository.save(pin);

        return buildSponsoredPinResponse(savedSponsored, pin);
    }

    public PaginatedResponse<SponsoredPinResponseDTO> getSponsoredPins(String businessId, int
            page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,
                "createdAt"));
        Page<SponsoredPin> sponsoredPage = sponsoredPinRepository.findByBusinessId(businessId,
                pageable);

        List<SponsoredPinResponseDTO> sponsored = sponsoredPage.getContent().stream()
                .map(sp -> {
                    Pin pin = pinRepository.findById(sp.getPinId()).orElse(null);
                    return buildSponsoredPinResponse(sp, pin);
                })
                .toList();

        PaginationDTO pagination = new PaginationDTO(
                sponsoredPage.getNumber(),
                sponsoredPage.getTotalPages(),
                sponsoredPage.getTotalElements(),
                sponsoredPage.getSize(),
                sponsoredPage.hasNext(),
                sponsoredPage.hasPrevious()
        );

        return new PaginatedResponse<>(sponsored, pagination);
    }

    public PaginatedResponse<SponsoredPinResponseDTO> getActiveSponsoredFeed(int page, int size)
    {
        Pageable pageable = PageRequest.of(page, size);
        Page<SponsoredPin> sponsoredPage =
                sponsoredPinRepository.findActiveSponsoredPins(LocalDate.now(), pageable);

        List<SponsoredPinResponseDTO> sponsored = sponsoredPage.getContent().stream()
                .map(sp -> {
                    Pin pin = pinRepository.findById(sp.getPinId()).orElse(null);
                    return buildSponsoredPinResponse(sp, pin);
                })
                .toList();

        PaginationDTO pagination = new PaginationDTO(
                sponsoredPage.getNumber(),
                sponsoredPage.getTotalPages(),
                sponsoredPage.getTotalElements(),
                sponsoredPage.getSize(),
                sponsoredPage.hasNext(),
                sponsoredPage.hasPrevious()
        );

        return new PaginatedResponse<>(sponsored, pagination);
    }

    public CampaignAnalyticsDTO getCampaignAnalytics(String campaignId) {
        SponsoredPin sponsored = sponsoredPinRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        CampaignAnalyticsDTO analytics = new CampaignAnalyticsDTO();
        analytics.setCampaignId(sponsored.getSponsoredId());
        analytics.setCampaignName(sponsored.getCampaignName());

        // Metrics
        MetricsDTO metrics = new MetricsDTO();
        metrics.setImpressions(sponsored.getImpressions());
        metrics.setClicks(sponsored.getClicks());
        metrics.setClickThroughRate(calculateCTR(sponsored.getClicks(),
                sponsored.getImpressions()));
        metrics.setSaves(sponsored.getSaves());
        metrics.setEngagement(sponsored.getClicks() + sponsored.getSaves());
        metrics.setEngagementRate(calculateEngagementRate(sponsored.getClicks() +
                sponsored.getSaves(), sponsored.getImpressions()));
        analytics.setMetrics(metrics);

        // Demographics (mock data for demonstration)
        DemographicsDTO demographics = new DemographicsDTO();
        demographics.setAgeGroups(Arrays.asList(
                new AgeGroupDTO("18-24", 35),
                new AgeGroupDTO("25-34", 45),
                new AgeGroupDTO("35-44", 20)
        ));
        demographics.setTopLocations(Arrays.asList(
                new LocationDTO("US", 60),
                new LocationDTO("UK", 25),
                new LocationDTO("CA", 15)
        ));
        analytics.setDemographics(demographics);

        // Spending
        SpendingDTO spending = new SpendingDTO();
        spending.setBudget(sponsored.getBudget());
        spending.setSpent(sponsored.getSpent());
        spending.setRemaining(sponsored.getBudget().subtract(sponsored.getSpent()));
        spending.setCostPerClick(calculateCPC(sponsored.getSpent(), sponsored.getClicks())); spending.setCostPerSave(calculateCPS(sponsored.getSpent(), sponsored.getSaves()));
        analytics.setSpending(spending);

        // Timeline (mock data)
        analytics.setTimeline(Arrays.asList(
                new TimelineDataDTO(LocalDate.now().minusDays(2), 5000, 250, 68),
                new TimelineDataDTO(LocalDate.now().minusDays(1), 4500, 230, 62),
                new TimelineDataDTO(LocalDate.now(), 4000, 200, 55)
        ));

        return analytics;
    }

    // Helper methods

    private BusinessProfileResponseDTO buildBusinessProfileResponse(BusinessProfile profile) {
        BusinessProfileResponseDTO dto = new BusinessProfileResponseDTO();
        dto.setBusinessId(profile.getBusinessId());
        dto.setUserId(profile.getUserId());
        dto.setBusinessName(profile.getBusinessName());
        dto.setDescription(profile.getDescription());
        dto.setWebsite(profile.getWebsite());
        dto.setCategory(profile.getCategory());
        dto.setLogoUrl(profile.getLogoUrl());
        dto.setContactEmail(profile.getContactEmail());
        dto.setFollowerCount(profile.getFollowerCount());
        dto.setCreatedAt(profile.getCreatedAt());

        // Get boards
        List<Board> boards = boardRepository.findByUserId(profile.getUserId());
        List<BoardSummaryDTO> boardSummaries = boards.stream()
                .map(b -> new BoardSummaryDTO(b.getBoardId(), b.getName()))
                .toList();
        dto.setBoards(boardSummaries);

        return dto;
    }

    private BusinessShowcaseResponseDTO buildShowcaseResponse(BusinessShowcase showcase,
                                                              BusinessProfile profile) {
        BusinessShowcaseResponseDTO dto = new BusinessShowcaseResponseDTO();
        dto.setShowcaseId(showcase.getShowcaseId());
        dto.setBusinessId(showcase.getBusinessId());
        dto.setTitle(showcase.getTitle());
        dto.setDescription(showcase.getDescription());
        dto.setTheme(showcase.getTheme());
        dto.setIsActive(showcase.getIsActive());

        if (profile != null) {
            dto.setBusinessName(profile.getBusinessName());
            dto.setLogoUrl(profile.getLogoUrl());
            dto.setFollowerCount(profile.getFollowerCount());
        }

        // Get showcase pins
        List<ShowcasePin> showcasePins =
                showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc(showcase.getShowcaseId());
        List<PinSummaryDTO> pins = showcasePins.stream()
                .map(sp -> {
                    Pin pin = pinRepository.findById(sp.getPinId()).orElse(null);
                    if (pin != null) {
                        return new PinSummaryDTO(pin.getPinId(), pin.getTitle(),
                                pin.getImageUrl());
                    }
                    return null;
                })
                .filter(p -> p != null)
                .toList();
        dto.setPins(pins);

        return dto;
    }

    private SponsoredPinResponseDTO buildSponsoredPinResponse(SponsoredPin sponsored, Pin pin) {
        SponsoredPinResponseDTO dto = new SponsoredPinResponseDTO();
        dto.setSponsoredId(sponsored.getSponsoredId());
        dto.setCampaignName(sponsored.getCampaignName());
        dto.setStatus(sponsored.getStatus().name());
        dto.setImpressions(sponsored.getImpressions());
        dto.setClicks(sponsored.getClicks());
        dto.setSaves(sponsored.getSaves());
        dto.setBudget(sponsored.getBudget());
        dto.setSpent(sponsored.getSpent());
        dto.setStartDate(sponsored.getStartDate());
        dto.setEndDate(sponsored.getEndDate());
        dto.setCreatedAt(sponsored.getCreatedAt());

        if (pin != null) {
            dto.setPin(new PinSummaryDTO(pin.getPinId(), pin.getTitle(), pin.getImageUrl()));
        }

        return dto;
    }

    private Double calculateCTR(Integer clicks, Integer impressions) {
        if (impressions == 0) return 0.0;
        return (clicks.doubleValue() / impressions.doubleValue()) * 100;
    }

    private Double calculateEngagementRate(Integer engagement, Integer impressions) {
        if (impressions == 0) return 0.0;
        return (engagement.doubleValue() / impressions.doubleValue()) * 100;
    }

    private BigDecimal calculateCPC(BigDecimal spent, Integer clicks) {
        if (clicks == 0) return BigDecimal.ZERO;
        return spent.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCPS(BigDecimal spent, Integer saves) {
        if (saves == 0) return BigDecimal.ZERO;
        return spent.divide(BigDecimal.valueOf(saves), 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public BusinessProfileResponseDTO updateBusinessProfile(String businessId, String userId,
                                                            BusinessProfileUpdateDTO updateDTO) {
        log.info("Updating business profile: {}", businessId);

        BusinessProfile profile = businessProfileRepository.findById(businessId)
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        // Verify ownership
        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to update this business profile");
        }

        // Update fields if provided
        if (updateDTO.getBusinessName() != null) {
            profile.setBusinessName(updateDTO.getBusinessName());
        }
        if (updateDTO.getDescription() != null) {
            profile.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getWebsite() != null) {
            profile.setWebsite(updateDTO.getWebsite());
        }
        if (updateDTO.getCategory() != null) {
            profile.setCategory(updateDTO.getCategory());
        }
        if (updateDTO.getLogoUrl() != null) {
            profile.setLogoUrl(updateDTO.getLogoUrl());
        }
        if (updateDTO.getContactEmail() != null) {
            profile.setContactEmail(updateDTO.getContactEmail());
        }

        BusinessProfile updated = businessProfileRepository.save(profile);
        return buildBusinessProfileResponse(updated);
    }

    @Transactional
    public BusinessShowcaseResponseDTO updateShowcase(String showcaseId, String userId,
                                                      BusinessShowcaseUpdateDTO updateDTO) {
        log.info("Updating showcase: {}", showcaseId);

        BusinessShowcase showcase = showcaseRepository.findById(showcaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Showcase not found"));

        // Verify ownership
        BusinessProfile profile = businessProfileRepository.findById(showcase.getBusinessId())
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to update this showcase");
        }

        // Update fields if provided
        if (updateDTO.getTitle() != null) {
            showcase.setTitle(updateDTO.getTitle());
        }
        if (updateDTO.getDescription() != null) {
            showcase.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getTheme() != null) {
            showcase.setTheme(updateDTO.getTheme());
        }
        if (updateDTO.getIsActive() != null) {
            showcase.setIsActive(updateDTO.getIsActive());
        }

        BusinessShowcase updated = showcaseRepository.save(showcase);
        return buildShowcaseResponse(updated, profile);
    }

    @Transactional
    public void addPinToShowcase(String showcaseId, String pinId, String userId, Integer displayOrder) {
        log.info("Adding pin {} to showcase {}", pinId, showcaseId);

        BusinessShowcase showcase = showcaseRepository.findById(showcaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Showcase not found"));

        // Verify ownership
        BusinessProfile profile = businessProfileRepository.findById(showcase.getBusinessId())
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to modify this showcase");
        }

        // Verify pin exists
        pinRepository.findById(pinId)
                .orElseThrow(() -> new PinNotFoundException("Pin not found"));

        // Check if pin is already in showcase
        if (showcasePinRepository.existsByShowcaseIdAndPinId(showcaseId, pinId)) {
            throw new ResourceNotFoundException("Pin is already in this showcase");
        }

        // If no display order provided, add to end
        if (displayOrder == null) {
            displayOrder = showcasePinRepository.countByShowcaseId(showcaseId);
        }

        ShowcasePin showcasePin = new ShowcasePin();
        showcasePin.setShowcaseId(showcaseId);
        showcasePin.setPinId(pinId);
        showcasePin.setDisplayOrder(displayOrder);

        showcasePinRepository.save(showcasePin);
    }

    @Transactional
    public void removePinFromShowcase(String showcaseId, String pinId, String userId) {
        log.info("Removing pin {} from showcase {}", pinId, showcaseId);

        BusinessShowcase showcase = showcaseRepository.findById(showcaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Showcase not found"));

        // Verify ownership
        BusinessProfile profile = businessProfileRepository.findById(showcase.getBusinessId())
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to modify this showcase");
        }

        ShowcasePin showcasePin = showcasePinRepository.findByShowcaseIdAndPinId(showcaseId, pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found in showcase"));

        showcasePinRepository.delete(showcasePin);
    }

    @Transactional
    public SponsoredPinResponseDTO updateSponsoredPin(String sponsoredId, String userId,
                                                      SponsoredPinUpdateDTO updateDTO) {
        log.info("Updating sponsored pin: {}", sponsoredId);

        SponsoredPin sponsored = sponsoredPinRepository.findById(sponsoredId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsored pin not found"));

        // Verify ownership
        BusinessProfile profile = businessProfileRepository.findById(sponsored.getBusinessId())
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to update this sponsored pin");
        }

        // Update fields if provided
        if (updateDTO.getCampaignName() != null) {
            sponsored.setCampaignName(updateDTO.getCampaignName());
        }
        if (updateDTO.getBudget() != null) {
            sponsored.setBudget(updateDTO.getBudget());
        }
        if (updateDTO.getEndDate() != null) {
            sponsored.setEndDate(updateDTO.getEndDate());
        }

        SponsoredPin updated = sponsoredPinRepository.save(sponsored);
        Pin pin = pinRepository.findById(updated.getPinId()).orElse(null);
        return buildSponsoredPinResponse(updated, pin);
    }

    @Transactional
    public SponsoredPinResponseDTO pauseSponsoredPin(String sponsoredId, String userId) {
        log.info("Pausing sponsored pin: {}", sponsoredId);

        SponsoredPin sponsored = sponsoredPinRepository.findById(sponsoredId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsored pin not found"));

        // Verify ownership
        BusinessProfile profile = businessProfileRepository.findById(sponsored.getBusinessId())
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to pause this sponsored pin");
        }

        sponsored.setStatus(SponsoredPin.Status.PAUSED);
        SponsoredPin updated = sponsoredPinRepository.save(sponsored);
        Pin pin = pinRepository.findById(updated.getPinId()).orElse(null);
        return buildSponsoredPinResponse(updated, pin);
    }

    @Transactional
    public SponsoredPinResponseDTO resumeSponsoredPin(String sponsoredId, String userId) {
        log.info("Resuming sponsored pin: {}", sponsoredId);

        SponsoredPin sponsored = sponsoredPinRepository.findById(sponsoredId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsored pin not found"));

        // Verify ownership
        BusinessProfile profile = businessProfileRepository.findById(sponsored.getBusinessId())
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to resume this sponsored pin");
        }

        sponsored.setStatus(SponsoredPin.Status.ACTIVE);
        SponsoredPin updated = sponsoredPinRepository.save(sponsored);
        Pin pin = pinRepository.findById(updated.getPinId()).orElse(null);
        return buildSponsoredPinResponse(updated, pin);
    }

    @Transactional
    public void deleteSponsoredPin(String sponsoredId, String userId) {
        log.info("Deleting sponsored pin: {}", sponsoredId);

        SponsoredPin sponsored = sponsoredPinRepository.findById(sponsoredId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsored pin not found"));

        // Verify ownership
        BusinessProfile profile = businessProfileRepository.findById(sponsored.getBusinessId())
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to delete this sponsored pin");
        }

        // Unmark pin as sponsored
        Pin pin = pinRepository.findById(sponsored.getPinId()).orElse(null);
        if (pin != null) {
            pin.setIsSponsored(false);
            pinRepository.save(pin);
        }

        sponsoredPinRepository.delete(sponsored);
    }

    @Transactional
    public void deleteShowcase(String showcaseId, String userId) {
        log.info("Deleting showcase: {}", showcaseId);

        BusinessShowcase showcase = showcaseRepository.findById(showcaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Showcase not found"));

        // Verify ownership
        BusinessProfile profile = businessProfileRepository.findById(showcase.getBusinessId())
                .orElseThrow(() -> new BusinessProfileNotFoundException("Business profile not found"));

        if (!profile.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized to delete this showcase");
        }

        // Delete all showcase pins first
        List<ShowcasePin> showcasePins = showcasePinRepository.findByShowcaseIdOrderByDisplayOrderAsc(showcaseId);
        showcasePinRepository.deleteAll(showcasePins);

        // Delete the showcase
        showcaseRepository.delete(showcase);
    }
}

