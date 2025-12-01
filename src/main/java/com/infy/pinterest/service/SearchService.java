package com.infy.pinterest.service;

import com.infy.pinterest.dto.*;
import com.infy.pinterest.entity.Board;
import com.infy.pinterest.entity.Pin;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.repository.BoardRepository;
import com.infy.pinterest.repository.PinRepository;
import com.infy.pinterest.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class SearchService {

    private final PinRepository pinRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public SearchService(PinRepository pinRepository, BoardRepository boardRepository,
                        UserRepository userRepository, ModelMapper modelMapper) {
        this.pinRepository = pinRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Search pins with filters and sorting
     */
    public SearchResultDTO<PinSearchResultDTO> searchPins(SearchRequestDTO searchRequest) {
        log.info("Searching pins with query: {}", searchRequest.getQuery());

        String keyword = searchRequest.getQuery().trim();
        Pageable pageable = createPageable(searchRequest);

        Page<Pin> pinPage;

        // Apply category filter if provided
        if (searchRequest.getCategory() != null && !searchRequest.getCategory().isEmpty()) {
            pinPage = pinRepository.searchPinsByCategory(keyword, searchRequest.getCategory(),
                    pageable);
        } else {
            pinPage = pinRepository.searchPins(keyword, pageable);
        }

        // Convert to DTOs using streams
        List<PinSearchResultDTO> results = pinPage.getContent().stream()
                .map(this::convertToPinSearchResult)
                .toList();

        // Get search suggestions
        List<String> suggestions = getSearchSuggestions(keyword);

        // Build pagination
        PaginationDTO pagination = new PaginationDTO(
                pinPage.getNumber(),
                pinPage.getTotalPages(),
                pinPage.getTotalElements(),
                pinPage.getSize(),
                pinPage.hasNext(),
                pinPage.hasPrevious()
        );

        SearchResultDTO<PinSearchResultDTO> response = new SearchResultDTO<>();
        response.setQuery(keyword);
        response.setResults(results);
        response.setSuggestions(suggestions);
        response.setPagination(pagination);
        response.setTotalResults(pinPage.getTotalElements());

        log.info("Found {} pins for query: {}", pinPage.getTotalElements(), keyword);
        return response;
    }
        /* Search boards */
        public SearchResultDTO<BoardSearchResultDTO> searchBoards(SearchRequestDTO searchRequest) {
            log.info("Searching boards with query: {}", searchRequest.getQuery());

            String keyword = searchRequest.getQuery().trim();
            Pageable pageable = createPageable(searchRequest);

            Page<Board> boardPage;

            // Apply category filter if provided
            if (searchRequest.getCategory() != null && !searchRequest.getCategory().isEmpty()) {
                boardPage = boardRepository.searchBoardsByCategory(keyword,
                        searchRequest.getCategory(), pageable);
            } else {
                boardPage = boardRepository.searchBoards(keyword, pageable);
            }

            // Convert to DTOs using streams
            List<BoardSearchResultDTO> results = boardPage.getContent().stream()
                    .map(this::convertToBoardSearchResult)
                    .toList();


            // Get board name suggestions

        List<String> suggestions = getBoardSuggestions(keyword);

        // Build pagination
        PaginationDTO pagination = new PaginationDTO(
                boardPage.getNumber(),
                boardPage.getTotalPages(),
                boardPage.getTotalElements(),
                boardPage.getSize(),
                boardPage.hasNext(),
                boardPage.hasPrevious()
        );

        SearchResultDTO<BoardSearchResultDTO> response = new SearchResultDTO<>();
        response.setQuery(keyword);
        response.setResults(results);
        response.setSuggestions(suggestions);
        response.setPagination(pagination);
        response.setTotalResults(boardPage.getTotalElements());

        log.info("Found {} boards for query: {}", boardPage.getTotalElements(), keyword);
        return response;
    }

    /**
     * Search users
     */
    public SearchResultDTO<UserSearchResultDTO> searchUsers(SearchRequestDTO searchRequest) {
        log.info("Searching users with query: {}", searchRequest.getQuery());

        String keyword = searchRequest.getQuery().trim();
        Pageable pageable = PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                Sort.by(Sort.Direction.ASC, "username")
        );

        Page<User> userPage = userRepository.searchUsers(keyword, pageable);

        // Convert to DTOs using streams
        List<UserSearchResultDTO> results = userPage.getContent().stream()
                .map(this::convertToUserSearchResult)
                .toList();

        // Get username suggestions
        List<String> suggestions = getUsernameSuggestions(keyword);

        // Build pagination
        PaginationDTO pagination = new PaginationDTO(
                userPage.getNumber(),userPage.getTotalPages(),
                userPage.getTotalElements(),
                userPage.getSize(),
                userPage.hasNext(),
                userPage.hasPrevious()
        );

        SearchResultDTO<UserSearchResultDTO> response = new SearchResultDTO<>();
        response.setQuery(keyword);
        response.setResults(results);
        response.setSuggestions(suggestions);
        response.setPagination(pagination);
        response.setTotalResults(userPage.getTotalElements());

        log.info("Found {} users for query: {}", userPage.getTotalElements(), keyword);
        return response;
    }

    /**
     * Get real-time search suggestions
     */
    public List<String> getSearchSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty() || keyword.trim().length() < 2) {
            return new ArrayList<>();
        }

        Pageable pageable = PageRequest.of(0, 10);
        List<String> suggestions = pinRepository.findTitleSuggestions(keyword.trim(), pageable);

        // Limit to 10 unique suggestions
        return suggestions.stream()
                .distinct()
                .limit(10)
                .toList();
    }

    /**
     * Get board name suggestions
     */
    public List<String> getBoardSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty() || keyword.trim().length() < 2) {
            return new ArrayList<>();
        }

        Pageable pageable = PageRequest.of(0, 10);
        List<String> suggestions = boardRepository.findBoardNameSuggestions(keyword.trim(),
                pageable);

        return suggestions.stream()
                .distinct().limit(10)
                .toList();
    }

    /**
     * Get username suggestions
     */
    public List<String> getUsernameSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty() || keyword.trim().length() < 2) {
            return new ArrayList<>();
        }

        Pageable pageable = PageRequest.of(0, 10);
        List<String> suggestions = userRepository.findUsernameSuggestions(keyword.trim(), pageable);

        return suggestions.stream()
                .distinct()
                .limit(10)
                .toList();
    }

    /**
     * Get popular pins (trending)
     */
    public PaginatedResponse<PinSearchResultDTO> getPopularPins(int page, int size) {
        log.info("Fetching popular pins");

        Pageable pageable = PageRequest.of(page, size);
        Page<Pin> pinPage = pinRepository.findPopularPins(pageable);

        List<PinSearchResultDTO> results = pinPage.getContent().stream()
                .map(this::convertToPinSearchResult)
                .toList();

        PaginationDTO pagination = new PaginationDTO(
                pinPage.getNumber(),
                pinPage.getTotalPages(),
                pinPage.getTotalElements(),
                pinPage.getSize(),
                pinPage.hasNext(),
                pinPage.hasPrevious()
        );

        return new PaginatedResponse<>(results, pagination);
    }

    // Helper methods

    private Pageable createPageable(SearchRequestDTO searchRequest) {String sortBy = searchRequest.getSortBy();
        Sort sort;

        if ("popular".equalsIgnoreCase(sortBy)) {
            // Sort by saves + likes (popularity)
            sort = Sort.by(Sort.Direction.DESC, "saveCount", "likeCount");
        } else if ("recent".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            // Default: relevance (by title match first, then description)
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }

    private PinSearchResultDTO convertToPinSearchResult(Pin pin) {
        PinSearchResultDTO dto = new PinSearchResultDTO();
        dto.setPinId(pin.getPinId());
        dto.setTitle(pin.getTitle());
        dto.setDescription(pin.getDescription());
        dto.setImageUrl(pin.getImageUrl());
        dto.setSaves(pin.getSaveCount());
        dto.setLikes(pin.getLikeCount());
        dto.setRelevanceScore(calculateRelevanceScore(pin));

        // Get creator info
        userRepository.findById(pin.getUserId()).ifPresent(user -> {
            UserSummaryDTO userSummary = new UserSummaryDTO();
            userSummary.setUserId(user.getUserId());
            userSummary.setUsername(user.getUsername());
            userSummary.setProfilePictureUrl(user.getProfilePictureUrl());
            dto.setCreatedBy(userSummary);
        });

        return dto;
    }

    private BoardSearchResultDTO convertToBoardSearchResult(Board board) {
        BoardSearchResultDTO dto = new BoardSearchResultDTO();
        dto.setBoardId(board.getBoardId());
        dto.setName(board.getName());
        dto.setDescription(board.getDescription());
        dto.setCoverImageUrl(board.getCoverImageUrl());
        dto.setPinCount(board.getPinCount());
        dto.setFollowers(0); 

        // Get creator info
        userRepository.findById(board.getUserId()).ifPresent(user -> {
            UserSummaryDTO userSummary = new UserSummaryDTO();userSummary.setUserId(user.getUserId());
            userSummary.setUsername(user.getUsername());
            userSummary.setProfilePictureUrl(user.getProfilePictureUrl());
            dto.setCreatedBy(userSummary);
        });

        return dto;
    }

    private UserSearchResultDTO convertToUserSearchResult(User user) {
        UserSearchResultDTO dto = new UserSearchResultDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setBio(user.getBio());

        
        dto.setFollowers(0);
        dto.setFollowing(0);

        // Get pin count
        Long pinCount = pinRepository.countByUserId(user.getUserId());
        dto.setPinCount(pinCount.intValue());

        return dto;
    }

    private Double calculateRelevanceScore(Pin pin) {
        // Simple relevance calculation based on engagement
        int totalEngagement = pin.getSaveCount() + pin.getLikeCount();
        return Math.min(1.0, totalEngagement / 100.0);
    }

}
