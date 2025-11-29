package com.infy.pinterest.controller;

 import com.infy.pinterest.dto.*;
 import com.infy.pinterest.service.BusinessService;
 import io.swagger.v3.oas.annotations.Operation;
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
 import io.swagger.v3.oas.annotations.tags.Tag;
 import jakarta.validation.Valid;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;


 @RestController
 @RequestMapping("/business")
 @Tag(name = "Business", description = "Business account and advertising APIs")
 @SecurityRequirement(name = "JWT")
 @Slf4j
 public class BusinessController {
     @Autowired    private BusinessService businessService;
     // ========== BUSINESS PROFILE ==========
     @PostMapping("/profile")
     @Operation(summary = "Create business profile")
     public ResponseEntity<ApiResponse<BusinessProfileResponseDTO>> createBusinessProfile(@RequestHeader("X-User-Id") String userId, @Valid @RequestBody BusinessProfileCreateDTO profileDTO) {



    log.info("POST /business/profile - Creating business profile");
    BusinessProfileResponseDTO response = businessService.createBusinessProfile(userId, profileDTO);
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Business profile created successfully", response));
     }
     @GetMapping("/profile/{businessId}")
     @Operation(summary = "Get business profile by ID")
     public ResponseEntity<ApiResponse<BusinessProfileResponseDTO>> getBusinessProfile(@PathVariable String businessId) {
         log.info("GET /business/profile/{} - Fetching business profile", businessId);
         BusinessProfileResponseDTO response = businessService.getBusinessProfile(businessId);
         return ResponseEntity.ok(ApiResponse.success("Business profile retrieved successfully", response));
     }
     // ========== SHOWCASES ==========
    @PostMapping("/showcases")
    @Operation(summary = "Create business showcase")
    public ResponseEntity<ApiResponse<BusinessShowcaseResponseDTO>> createShowcase(@RequestHeader("X-Business-Id") String businessId, @Valid @RequestBody BusinessShowcaseCreateDTO showcaseDTO) {
         log.info("POST /business/showcases - Creating showcase");
         BusinessShowcaseResponseDTO response = businessService.createShowcase(businessId, showcaseDTO);
         return ResponseEntity
                 .status(HttpStatus.CREATED)
                 .body(ApiResponse.success("Showcase created successfully", response));
     }

     @GetMapping("/showcases")
     @Operation(summary = "Get active business showcases")
     public ResponseEntity<ApiResponse<PaginatedResponse<BusinessShowcaseResponseDTO>>> getActiveShowcases(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
         log.info("GET /business/showcases - Fetching active showcases");
         PaginatedResponse<BusinessShowcaseResponseDTO> response = businessService.getActiveShowcases(page, size);
         return ResponseEntity.ok(ApiResponse.success("Showcases retrieved successfully", response));
     }


// ========== SPONSORED PINS ==========
    @PostMapping("/sponsored-pins")
    @Operation(summary = "Create sponsored pin")
    public ResponseEntity<ApiResponse<SponsoredPinResponseDTO>> createSponsoredPin(@RequestHeader("X-Business-Id") String businessId, @Valid @RequestBody SponsoredPinCreateDTO sponsoredDTO) {
         log.info("POST /business/sponsored-pins - Creating sponsored pin");
         SponsoredPinResponseDTO response = businessService.createSponsoredPin(businessId, sponsoredDTO);
         return ResponseEntity
                 .status(HttpStatus.CREATED)
                 .body(ApiResponse.success("Sponsored pin created successfully", response));
     }
     @GetMapping("/sponsored-pins")
     @Operation(summary = "Get business sponsored pins")
     public ResponseEntity<ApiResponse<PaginatedResponse<SponsoredPinResponseDTO>>> getSponsoredPins(@RequestHeader("X-Business-Id") String businessId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
         log.info("GET /business/sponsored-pins - Fetching sponsored pins");
         PaginatedResponse<SponsoredPinResponseDTO> response = businessService.getSponsoredPins(businessId, page, size);
         return ResponseEntity.ok(ApiResponse.success("Sponsored pins retrieved successfully", response));
     }

     @GetMapping("/sponsored-feed")
     @Operation(summary = "Get active sponsored pins feed")
     public ResponseEntity<ApiResponse<PaginatedResponse<SponsoredPinResponseDTO>>>
     getActiveSponsoredFeed(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "20") int size) {
         log.info("GET /business/sponsored-feed - Fetching active sponsored feed");
         PaginatedResponse<SponsoredPinResponseDTO> response = businessService.getActiveSponsoredFeed(page, size);
         return ResponseEntity.ok(ApiResponse.success("Sponsored feed retrieved successfully", response));
     }    // ========== ANALYTICS ==========
    @GetMapping("/analytics/{campaignId}")
    @Operation(summary = "Get campaign analytics")
    public ResponseEntity<ApiResponse<CampaignAnalyticsDTO>> getCampaignAnalytics(@PathVariable String campaignId) {
         log.info("GET /business/analytics/{} - Fetching campaign analytics", campaignId);
         CampaignAnalyticsDTO analytics = businessService.getCampaignAnalytics(campaignId);
         return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
     }

     // ========== UPDATE ENDPOINTS ==========
     @PutMapping("/profile/{businessId}")
     @Operation(summary = "Update business profile")
     public ResponseEntity<ApiResponse<BusinessProfileResponseDTO>> updateBusinessProfile(
             @PathVariable String businessId,
             @RequestHeader("X-User-Id") String userId,
             @Valid @RequestBody BusinessProfileUpdateDTO updateDTO) {
         log.info("PUT /business/profile/{} - Updating business profile", businessId);
         BusinessProfileResponseDTO updated = businessService.updateBusinessProfile(businessId, userId, updateDTO);
         return ResponseEntity.ok(ApiResponse.success("Business profile updated successfully", updated));
     }

     @PutMapping("/showcases/{showcaseId}")
     @Operation(summary = "Update showcase")
     public ResponseEntity<ApiResponse<BusinessShowcaseResponseDTO>> updateShowcase(
             @PathVariable String showcaseId,
             @RequestHeader("X-User-Id") String userId,
             @Valid @RequestBody BusinessShowcaseUpdateDTO updateDTO) {
         log.info("PUT /business/showcases/{} - Updating showcase", showcaseId);
         BusinessShowcaseResponseDTO updated = businessService.updateShowcase(showcaseId, userId, updateDTO);
         return ResponseEntity.ok(ApiResponse.success("Showcase updated successfully", updated));
     }

     @PostMapping("/showcases/{showcaseId}/pins/{pinId}")
     @Operation(summary = "Add pin to showcase")
     public ResponseEntity<ApiResponse<String>> addPinToShowcase(
             @PathVariable String showcaseId,
             @PathVariable String pinId,
             @RequestHeader("X-User-Id") String userId,
             @RequestParam(required = false) Integer displayOrder) {
         log.info("POST /business/showcases/{}/pins/{} - Adding pin to showcase", showcaseId, pinId);
         businessService.addPinToShowcase(showcaseId, pinId, userId, displayOrder);
         return ResponseEntity.ok(ApiResponse.success("Pin added to showcase successfully", null));
     }

     @DeleteMapping("/showcases/{showcaseId}/pins/{pinId}")
     @Operation(summary = "Remove pin from showcase")
     public ResponseEntity<ApiResponse<String>> removePinFromShowcase(
             @PathVariable String showcaseId,
             @PathVariable String pinId,
             @RequestHeader("X-User-Id") String userId) {
         log.info("DELETE /business/showcases/{}/pins/{} - Removing pin from showcase", showcaseId, pinId);
         businessService.removePinFromShowcase(showcaseId, pinId, userId);
         return ResponseEntity.ok(ApiResponse.success("Pin removed from showcase successfully", null));
     }

     @DeleteMapping("/showcases/{showcaseId}")
     @Operation(summary = "Delete showcase")
     public ResponseEntity<ApiResponse<String>> deleteShowcase(
             @PathVariable String showcaseId,
             @RequestHeader("X-User-Id") String userId) {
         log.info("DELETE /business/showcases/{} - Deleting showcase", showcaseId);
         businessService.deleteShowcase(showcaseId, userId);
         return ResponseEntity.ok(ApiResponse.success("Showcase deleted successfully", null));
     }

     @PutMapping("/sponsored-pins/{sponsoredId}")
     @Operation(summary = "Update sponsored pin campaign")
     public ResponseEntity<ApiResponse<SponsoredPinResponseDTO>> updateSponsoredPin(
             @PathVariable String sponsoredId,
             @RequestHeader("X-User-Id") String userId,
             @Valid @RequestBody SponsoredPinUpdateDTO updateDTO) {
         log.info("PUT /business/sponsored-pins/{} - Updating sponsored pin", sponsoredId);
         SponsoredPinResponseDTO updated = businessService.updateSponsoredPin(sponsoredId, userId, updateDTO);
         return ResponseEntity.ok(ApiResponse.success("Sponsored pin updated successfully", updated));
     }

     @PutMapping("/sponsored-pins/{sponsoredId}/pause")
     @Operation(summary = "Pause sponsored pin campaign")
     public ResponseEntity<ApiResponse<SponsoredPinResponseDTO>> pauseSponsoredPin(
             @PathVariable String sponsoredId,
             @RequestHeader("X-User-Id") String userId) {
         log.info("PUT /business/sponsored-pins/{}/pause - Pausing sponsored pin", sponsoredId);
         SponsoredPinResponseDTO paused = businessService.pauseSponsoredPin(sponsoredId, userId);
         return ResponseEntity.ok(ApiResponse.success("Sponsored pin paused successfully", paused));
     }

     @PutMapping("/sponsored-pins/{sponsoredId}/resume")
     @Operation(summary = "Resume sponsored pin campaign")
     public ResponseEntity<ApiResponse<SponsoredPinResponseDTO>> resumeSponsoredPin(
             @PathVariable String sponsoredId,
             @RequestHeader("X-User-Id") String userId) {
         log.info("PUT /business/sponsored-pins/{}/resume - Resuming sponsored pin", sponsoredId);
         SponsoredPinResponseDTO resumed = businessService.resumeSponsoredPin(sponsoredId, userId);
         return ResponseEntity.ok(ApiResponse.success("Sponsored pin resumed successfully", resumed));
     }

     @DeleteMapping("/sponsored-pins/{sponsoredId}")
     @Operation(summary = "Delete sponsored pin campaign")
     public ResponseEntity<ApiResponse<String>> deleteSponsoredPin(
             @PathVariable String sponsoredId,
             @RequestHeader("X-User-Id") String userId) {
         log.info("DELETE /business/sponsored-pins/{} - Deleting sponsored pin", sponsoredId);
         businessService.deleteSponsoredPin(sponsoredId, userId);
         return ResponseEntity.ok(ApiResponse.success("Sponsored pin deleted successfully", null));
     }
 }