package com.infy.pinterest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.pinterest.controller.SearchController;
import com.infy.pinterest.dto.*;
import com.infy.pinterest.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    private PinSearchResultDTO pinSearchResult;
    private BoardSearchResultDTO boardSearchResult;
    private UserSearchResultDTO userSearchResult;
    private SearchResultDTO<PinSearchResultDTO> pinSearchResultDTO;
    private SearchResultDTO<BoardSearchResultDTO> boardSearchResultDTO;
    private SearchResultDTO<UserSearchResultDTO> userSearchResultDTO;
    private PaginatedResponse<PinSearchResultDTO> paginatedPinResponse;
    private UserSummaryDTO userSummary;
    private PaginationDTO pagination;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
        objectMapper = new ObjectMapper();
        
        // Setup user summary
        userSummary = new UserSummaryDTO();
        userSummary.setUserId("user-123");
        userSummary.setUsername("testuser");
        userSummary.setProfilePictureUrl("https://example.com/profile.jpg");

        // Setup pagination
        pagination = new PaginationDTO(0, 1, 10L, 20, false, false);

        // Setup pin search result
        pinSearchResult = new PinSearchResultDTO();
        pinSearchResult.setPinId("pin-123");
        pinSearchResult.setTitle("Beautiful Sunset");
        pinSearchResult.setDescription("Amazing sunset at the beach");
        pinSearchResult.setImageUrl("https://example.com/sunset.jpg");
        pinSearchResult.setCreatedBy(userSummary);
        pinSearchResult.setSaves(50);
        pinSearchResult.setLikes(100);
        pinSearchResult.setRelevanceScore(0.95);

        // Setup board search result
        boardSearchResult = new BoardSearchResultDTO();
        boardSearchResult.setBoardId("board-123");
        boardSearchResult.setName("Travel Board");
        boardSearchResult.setDescription("My travel collection");
        boardSearchResult.setCoverImageUrl("https://example.com/board-cover.jpg");
        boardSearchResult.setCreatedBy(userSummary);
        boardSearchResult.setPinCount(25);
        boardSearchResult.setFollowers(150);

        // Setup user search result
        userSearchResult = new UserSearchResultDTO();
        userSearchResult.setUserId("user-123");
        userSearchResult.setUsername("testuser");
        userSearchResult.setFullName("Test User");
        userSearchResult.setProfilePictureUrl("https://example.com/profile.jpg");
        userSearchResult.setBio("Travel enthusiast");
        userSearchResult.setFollowers(500);
        userSearchResult.setFollowing(300);
        userSearchResult.setPinCount(100);

        // Setup pin search resultDTO
        pinSearchResultDTO = new SearchResultDTO<>();
        pinSearchResultDTO.setQuery("sunset");
        pinSearchResultDTO.setResults(Collections.singletonList(pinSearchResult));
        pinSearchResultDTO.setSuggestions(Arrays.asList("sunset beach", "sunset photography", "sunset colors"));
        pinSearchResultDTO.setPagination(pagination);
        pinSearchResultDTO.setTotalResults(10L);

        // Setup board search resultDTO
        boardSearchResultDTO = new SearchResultDTO<>();
        boardSearchResultDTO.setQuery("travel");
        boardSearchResultDTO.setResults(Collections.singletonList(boardSearchResult));
        boardSearchResultDTO.setSuggestions(Arrays.asList("travel destinations", "travel tips", "travel photography"));
        boardSearchResultDTO.setPagination(pagination);
        boardSearchResultDTO.setTotalResults(5L);

        // Setup user search resultDTO
        userSearchResultDTO = new SearchResultDTO<>();
        userSearchResultDTO.setQuery("test");
        userSearchResultDTO.setResults(Collections.singletonList(userSearchResult));
        userSearchResultDTO.setSuggestions(Arrays.asList("testuser", "testaccount", "tester"));
        userSearchResultDTO.setPagination(pagination);
        userSearchResultDTO.setTotalResults(3L);

        // Setup paginated pin response
        paginatedPinResponse = new PaginatedResponse<>(
                Collections.singletonList(pinSearchResult),
                pagination
        );
    }

    // ==================== SEARCH PINS TESTS ====================

    @Test
    @DisplayName("GET /search/pins - Success - Basic Search")
    void testSearchPins_Success_BasicSearch() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Search completed successfully"))
                .andExpect(jsonPath("$.data.query").value("sunset"))
                .andExpect(jsonPath("$.data.results", hasSize(1)))
                .andExpect(jsonPath("$.data.results[0].pinId").value("pin-123"))
                .andExpect(jsonPath("$.data.results[0].title").value("Beautiful Sunset"))
                .andExpect(jsonPath("$.data.totalResults").value(10));

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/pins - Success - With Category Filter")
    void testSearchPins_Success_WithCategory() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset")
                        .param("category", "nature"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(1)));

        verify(searchService, times(1)).searchPins(argThat(req ->
                req.getQuery().equals("sunset") &&
                        req.getCategory().equals("nature")
        ));
    }

    @Test
    @DisplayName("GET /search/pins - Success - Sort by Relevance")
    void testSearchPins_Success_SortByRelevance() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset")
                        .param("sort", "relevance"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].relevanceScore").value(0.95));

        verify(searchService, times(1)).searchPins(argThat(req ->
                req.getSortBy().equals("relevance")
        ));
    }

    @Test
    @DisplayName("GET /search/pins - Success - Sort by Popular")
    void testSearchPins_Success_SortByPopular() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset")
                        .param("sort", "popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].saves").value(50))
                .andExpect(jsonPath("$.data.results[0].likes").value(100));

        verify(searchService, times(1)).searchPins(argThat(req ->
                req.getSortBy().equals("popular")
        ));
    }

    @Test
    @DisplayName("GET /search/pins - Success - Sort by Recent")
    void testSearchPins_Success_SortByRecent() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset")
                        .param("sort", "recent"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(searchService, times(1)).searchPins(argThat(req ->
                req.getSortBy().equals("recent")
        ));
    }

    @Test
    @DisplayName("GET /search/pins - Success - With Pagination")
    void testSearchPins_Success_WithPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(2, 5, 100L, 20, true, true);
        SearchResultDTO<PinSearchResultDTO> customResult = new SearchResultDTO<>();
        customResult.setQuery("sunset");
        customResult.setResults(Collections.singletonList(pinSearchResult));
        customResult.setSuggestions(Arrays.asList("sunset"));
        customResult.setPagination(customPagination);
        customResult.setTotalResults(100L);

        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(customResult);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset")
                        .param("page", "2")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(5))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(100));

        verify(searchService, times(1)).searchPins(argThat(req ->
                req.getPage() == 2 && req.getSize() == 20
        ));
    }

    @Test
    @DisplayName("GET /search/pins - Success - With Suggestions")
    void testSearchPins_Success_WithSuggestions() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestions", hasSize(3)))
                .andExpect(jsonPath("$.data.suggestions[0]").value("sunset beach"))
                .andExpect(jsonPath("$.data.suggestions[1]").value("sunset photography"));

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/pins - Success - Empty Results")
    void testSearchPins_Success_EmptyResults() throws Exception {
        // Arrange
        SearchResultDTO<PinSearchResultDTO> emptyResult = new SearchResultDTO<>();
        emptyResult.setQuery("nonexistent");
        emptyResult.setResults(Collections.emptyList());
        emptyResult.setSuggestions(Collections.emptyList());
        emptyResult.setPagination(new PaginationDTO(0, 0, 0L, 20, false, false));
        emptyResult.setTotalResults(0L);

        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(emptyResult);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "nonexistent"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(0)))
                .andExpect(jsonPath("$.data.totalResults").value(0));

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/pins - Success - Multiple Results")
    void testSearchPins_Success_MultipleResults() throws Exception {
        // Arrange
        PinSearchResultDTO pin2 = new PinSearchResultDTO();
        pin2.setPinId("pin-456");
        pin2.setTitle("Sunrise Mountain");
        pin2.setSaves(30);
        pin2.setLikes(60);

        SearchResultDTO<PinSearchResultDTO> multipleResult = new SearchResultDTO<>();
        multipleResult.setQuery("sunset");
        multipleResult.setResults(Arrays.asList(pinSearchResult, pin2));
        multipleResult.setSuggestions(Arrays.asList("sunset"));
        multipleResult.setPagination(new PaginationDTO(0, 1, 2L, 20, false, false));
        multipleResult.setTotalResults(2L);

        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(multipleResult);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(2)))
                .andExpect(jsonPath("$.data.results[0].pinId").value("pin-123"))
                .andExpect(jsonPath("$.data.results[1].pinId").value("pin-456"));

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/pins - Success - With User Info")
    void testSearchPins_Success_WithUserInfo() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].createdBy.userId").value("user-123"))
                .andExpect(jsonPath("$.data.results[0].createdBy.username").value("testuser"));

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/pins - Failure - Missing Query Parameter")
    void testSearchPins_Failure_MissingQuery() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/search/pins"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(searchService, never()).searchPins(any(SearchRequestDTO.class));
    }

    // ==================== SEARCH BOARDS TESTS ====================

    @Test
    @DisplayName("GET /search/boards - Success - Basic Search")
    void testSearchBoards_Success_BasicSearch() throws Exception {
        // Arrange
        when(searchService.searchBoards(any(SearchRequestDTO.class)))
                .thenReturn(boardSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/boards")
                        .param("q", "travel"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Search completed successfully"))
                .andExpect(jsonPath("$.data.query").value("travel"))
                .andExpect(jsonPath("$.data.results", hasSize(1)))
                .andExpect(jsonPath("$.data.results[0].boardId").value("board-123"))
                .andExpect(jsonPath("$.data.results[0].name").value("Travel Board"))
                .andExpect(jsonPath("$.data.totalResults").value(5));

        verify(searchService, times(1)).searchBoards(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/boards - Success - With Category")
    void testSearchBoards_Success_WithCategory() throws Exception {
        // Arrange
        when(searchService.searchBoards(any(SearchRequestDTO.class)))
                .thenReturn(boardSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/boards")
                        .param("q", "travel")
                        .param("category", "vacation"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(1)));

        verify(searchService, times(1)).searchBoards(argThat(req ->
                req.getQuery().equals("travel") &&
                        req.getCategory().equals("vacation")
        ));
    }

    @Test
    @DisplayName("GET /search/boards - Success - With Pagination")
    void testSearchBoards_Success_WithPagination() throws Exception {
        // Arrange
        when(searchService.searchBoards(any(SearchRequestDTO.class)))
                .thenReturn(boardSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/boards")
                        .param("q", "travel")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(searchService, times(1)).searchBoards(argThat(req ->
                req.getPage() == 1 && req.getSize() == 10
        ));
    }

    @Test
    @DisplayName("GET /search/boards - Success - With Board Details")
    void testSearchBoards_Success_WithBoardDetails() throws Exception {
        // Arrange
        when(searchService.searchBoards(any(SearchRequestDTO.class)))
                .thenReturn(boardSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/boards")
                        .param("q", "travel"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].description").value("My travel collection"))
                .andExpect(jsonPath("$.data.results[0].pinCount").value(25))
                .andExpect(jsonPath("$.data.results[0].followers").value(150))
                .andExpect(jsonPath("$.data.results[0].coverImageUrl").exists());

        verify(searchService, times(1)).searchBoards(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/boards - Success - With Creator Info")
    void testSearchBoards_Success_WithCreatorInfo() throws Exception {
        // Arrange
        when(searchService.searchBoards(any(SearchRequestDTO.class)))
                .thenReturn(boardSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/boards")
                        .param("q", "travel"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].createdBy.userId").value("user-123"))
                .andExpect(jsonPath("$.data.results[0].createdBy.username").value("testuser"));

        verify(searchService, times(1)).searchBoards(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/boards - Success - Empty Results")
    void testSearchBoards_Success_EmptyResults() throws Exception {
        // Arrange
        SearchResultDTO<BoardSearchResultDTO> emptyResult = new SearchResultDTO<>();
        emptyResult.setQuery("nonexistent");
        emptyResult.setResults(Collections.emptyList());
        emptyResult.setSuggestions(Collections.emptyList());
        emptyResult.setPagination(new PaginationDTO(0, 0, 0L, 20, false, false));
        emptyResult.setTotalResults(0L);

        when(searchService.searchBoards(any(SearchRequestDTO.class)))
                .thenReturn(emptyResult);

        // Act & Assert
        mockMvc.perform(get("/search/boards")
                        .param("q", "nonexistent"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(0)));

        verify(searchService, times(1)).searchBoards(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/boards - Success - With Suggestions")
    void testSearchBoards_Success_WithSuggestions() throws Exception {
        // Arrange
        when(searchService.searchBoards(any(SearchRequestDTO.class)))
                .thenReturn(boardSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/boards")
                        .param("q", "travel"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestions", hasSize(3)))
                .andExpect(jsonPath("$.data.suggestions[0]").value("travel destinations"));

        verify(searchService, times(1)).searchBoards(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/boards - Failure - Missing Query Parameter")
    void testSearchBoards_Failure_MissingQuery() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/search/boards"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(searchService, never()).searchBoards(any(SearchRequestDTO.class));
    }

    // ==================== SEARCH USERS TESTS ====================

    @Test
    @DisplayName("GET /search/users - Success - Basic Search")
    void testSearchUsers_Success_BasicSearch() throws Exception {
        // Arrange
        when(searchService.searchUsers(any(SearchRequestDTO.class)))
                .thenReturn(userSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/users")
                        .param("q", "test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Search completed successfully"))
                .andExpect(jsonPath("$.data.query").value("test"))
                .andExpect(jsonPath("$.data.results", hasSize(1)))
                .andExpect(jsonPath("$.data.results[0].userId").value("user-123"))
                .andExpect(jsonPath("$.data.results[0].username").value("testuser"))
                .andExpect(jsonPath("$.data.totalResults").value(3));

        verify(searchService, times(1)).searchUsers(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/users - Success - With User Details")
    void testSearchUsers_Success_WithUserDetails() throws Exception {
        // Arrange
        when(searchService.searchUsers(any(SearchRequestDTO.class)))
                .thenReturn(userSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/users")
                        .param("q", "test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].fullName").value("Test User"))
                .andExpect(jsonPath("$.data.results[0].bio").value("Travel enthusiast"))
                .andExpect(jsonPath("$.data.results[0].followers").value(500))
                .andExpect(jsonPath("$.data.results[0].following").value(300))
                .andExpect(jsonPath("$.data.results[0].pinCount").value(100));

        verify(searchService, times(1)).searchUsers(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/users - Success - With Pagination")
    void testSearchUsers_Success_WithPagination() throws Exception {
        // Arrange
        when(searchService.searchUsers(any(SearchRequestDTO.class)))
                .thenReturn(userSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/users")
                        .param("q", "test")
                        .param("page", "1")
                        .param("size", "15"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(searchService, times(1)).searchUsers(argThat(req ->
                req.getPage() == 1 && req.getSize() == 15
        ));
    }

    @Test
    @DisplayName("GET /search/users - Success - Multiple Users")
    void testSearchUsers_Success_MultipleUsers() throws Exception {
        // Arrange
        UserSearchResultDTO user2 = new UserSearchResultDTO();
        user2.setUserId("user-456");
        user2.setUsername("testaccount");
        user2.setFullName("Test Account");

        SearchResultDTO<UserSearchResultDTO> multipleResult = new SearchResultDTO<>();
        multipleResult.setQuery("test");
        multipleResult.setResults(Arrays.asList(userSearchResult, user2));
        multipleResult.setSuggestions(Arrays.asList("testuser", "testaccount"));
        multipleResult.setPagination(new PaginationDTO(0, 1, 2L, 20, false, false));
        multipleResult.setTotalResults(2L);

        when(searchService.searchUsers(any(SearchRequestDTO.class)))
                .thenReturn(multipleResult);

        // Act & Assert
        mockMvc.perform(get("/search/users")
                        .param("q", "test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(2)))
                .andExpect(jsonPath("$.data.results[0].username").value("testuser"))
                .andExpect(jsonPath("$.data.results[1].username").value("testaccount"));

        verify(searchService, times(1)).searchUsers(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/users - Success - Empty Results")
    void testSearchUsers_Success_EmptyResults() throws Exception {
        // Arrange
        SearchResultDTO<UserSearchResultDTO> emptyResult = new SearchResultDTO<>();
        emptyResult.setQuery("nonexistent");
        emptyResult.setResults(Collections.emptyList());
        emptyResult.setSuggestions(Collections.emptyList());
        emptyResult.setPagination(new PaginationDTO(0, 0, 0L, 20, false, false));
        emptyResult.setTotalResults(0L);

        when(searchService.searchUsers(any(SearchRequestDTO.class)))
                .thenReturn(emptyResult);

        // Act & Assert
        mockMvc.perform(get("/search/users")
                        .param("q", "nonexistent"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(0)));

        verify(searchService, times(1)).searchUsers(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/users - Success - With Suggestions")
    void testSearchUsers_Success_WithSuggestions() throws Exception {
        // Arrange
        when(searchService.searchUsers(any(SearchRequestDTO.class)))
                .thenReturn(userSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/users")
                        .param("q", "test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestions", hasSize(3)))
                .andExpect(jsonPath("$.data.suggestions[0]").value("testuser"));

        verify(searchService, times(1)).searchUsers(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("GET /search/users - Failure - Missing Query Parameter")
    void testSearchUsers_Failure_MissingQuery() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/search/users"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(searchService, never()).searchUsers(any(SearchRequestDTO.class));
    }

    // ==================== SEARCH SUGGESTIONS TESTS ====================

    @Test
    @DisplayName("GET /search/suggestions - Success")
    void testGetSearchSuggestions_Success() throws Exception {
        // Arrange
        List<String> suggestions = Arrays.asList("sunset", "sunset beach", "sunset photography");
        when(searchService.getSearchSuggestions("sun"))
                .thenReturn(suggestions);

        // Act & Assert
        mockMvc.perform(get("/search/suggestions")
                        .param("q", "sun"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Suggestions retrieved successfully"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0]").value("sunset"))
                .andExpect(jsonPath("$.data[1]").value("sunset beach"));

        verify(searchService, times(1)).getSearchSuggestions("sun");
    }

    @Test
    @DisplayName("GET /search/suggestions - Success - With Limit")
    void testGetSearchSuggestions_Success_WithLimit() throws Exception {
        // Arrange
        List<String> suggestions = Arrays.asList("sunset", "sunrise", "sun");
        when(searchService.getSearchSuggestions("sun"))
                .thenReturn(suggestions);

        // Act & Assert
        mockMvc.perform(get("/search/suggestions")
                        .param("q", "sun")
                        .param("limit", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));

        verify(searchService, times(1)).getSearchSuggestions("sun");
    }

    @Test
    @DisplayName("GET /search/suggestions - Success - Empty Suggestions")
    void testGetSearchSuggestions_Success_EmptySuggestions() throws Exception {
        // Arrange
        when(searchService.getSearchSuggestions("xyz"))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/search/suggestions")
                        .param("q", "xyz"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(searchService, times(1)).getSearchSuggestions("xyz");
    }

    @Test
    @DisplayName("GET /search/suggestions - Success - Short Query")
    void testGetSearchSuggestions_Success_ShortQuery() throws Exception {
        // Arrange
        when(searchService.getSearchSuggestions("a"))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/search/suggestions")
                        .param("q", "a"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(searchService, times(1)).getSearchSuggestions("a");
    }

    @Test
    @DisplayName("GET /search/suggestions - Failure - Missing Query")
    void testGetSearchSuggestions_Failure_MissingQuery() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/search/suggestions"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(searchService, never()).getSearchSuggestions(anyString());
    }

    // ==================== POPULAR PINS TESTS ====================

    @Test
    @DisplayName("GET /search/popular - Success")
    void testGetPopularPins_Success() throws Exception {
        // Arrange
        when(searchService.getPopularPins(0, 20))
                .thenReturn(paginatedPinResponse);

        // Act & Assert
        mockMvc.perform(get("/search/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Popular pins retrieved successfully"))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].pinId").value("pin-123"))
                .andExpect(jsonPath("$.data.data[0].saves").value(50))
                .andExpect(jsonPath("$.data.data[0].likes").value(100));

        verify(searchService, times(1)).getPopularPins(0, 20);
    }

    @Test
    @DisplayName("GET /search/popular - Success - Custom Pagination")
    void testGetPopularPins_Success_CustomPagination() throws Exception {
        // Arrange
        PaginationDTO customPagination = new PaginationDTO(1, 3, 60L, 20, true, true);
        PaginatedResponse<PinSearchResultDTO> customResponse = new PaginatedResponse<>(
                Collections.singletonList(pinSearchResult),
                customPagination
        );

        when(searchService.getPopularPins(1, 20))
                .thenReturn(customResponse);

        // Act & Assert
        mockMvc.perform(get("/search/popular")
                        .param("page", "1")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(3));

        verify(searchService, times(1)).getPopularPins(1, 20);
    }

    @Test
    @DisplayName("GET /search/popular - Success - Multiple Popular Pins")
    void testGetPopularPins_Success_MultiplePins() throws Exception {
        // Arrange
        PinSearchResultDTO popularPin1 = new PinSearchResultDTO();
        popularPin1.setPinId("pin-popular-1");
        popularPin1.setSaves(500);
        popularPin1.setLikes(1000);

        PinSearchResultDTO popularPin2 = new PinSearchResultDTO();
        popularPin2.setPinId("pin-popular-2");
        popularPin2.setSaves(400);
        popularPin2.setLikes(800);

        PaginatedResponse<PinSearchResultDTO> multipleResponse = new PaginatedResponse<>(
                Arrays.asList(popularPin1, popularPin2),
                new PaginationDTO(0, 1, 2L, 20, false, false)
        );

        when(searchService.getPopularPins(0, 20))
                .thenReturn(multipleResponse);

        // Act & Assert
        mockMvc.perform(get("/search/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(2)))
                .andExpect(jsonPath("$.data.data[0].saves").value(500))
                .andExpect(jsonPath("$.data.data[1].saves").value(400));

        verify(searchService, times(1)).getPopularPins(0, 20);
    }

    @Test
    @DisplayName("GET /search/popular - Success - Empty List")
    void testGetPopularPins_Success_EmptyList() throws Exception {
        // Arrange
        PaginatedResponse<PinSearchResultDTO> emptyResponse = new PaginatedResponse<>(
                Collections.emptyList(),
                new PaginationDTO(0, 0, 0L, 20, false, false)
        );

        when(searchService.getPopularPins(0, 20))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/search/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data", hasSize(0)));

        verify(searchService, times(1)).getPopularPins(0, 20);
    }

    @Test
    @DisplayName("GET /search/popular - Success - Default Pagination")
    void testGetPopularPins_Success_DefaultPagination() throws Exception {
        // Arrange
        when(searchService.getPopularPins(0, 20))
                .thenReturn(paginatedPinResponse);

        // Act & Assert
        mockMvc.perform(get("/search/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.pageSize").value(20));

        verify(searchService, times(1)).getPopularPins(0, 20);
    }

    // ==================== INTEGRATION & EDGE CASE TESTS ====================

    @Test
    @DisplayName("Search Workflow - Pins with All Filters")
    void testSearchWorkflow_PinsWithAllFilters() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset")
                        .param("category", "nature")
                        .param("sort", "popular")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(1)));

        verify(searchService, times(1)).searchPins(argThat(req ->
                req.getQuery().equals("sunset") &&
                        req.getCategory().equals("nature") &&
                        req.getSortBy().equals("popular") &&
                        req.getPage() == 0 &&
                        req.getSize() == 10
        ));
    }

    @Test
    @DisplayName("Edge Case - Special Characters in Query")
    void testEdgeCase_SpecialCharactersInQuery() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset & beach!"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("Edge Case - Very Long Query String")
    void testEdgeCase_VeryLongQuery() throws Exception {
        // Arrange
        String longQuery = "a".repeat(200);
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", longQuery))
                .andDo(print())
                .andExpect(status().isOk());

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("Edge Case - Unicode Characters in Query")
    void testEdgeCase_UnicodeCharacters() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "日本の夕日"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("Pagination Edge Case - Large Page Number")
    void testPaginationEdgeCase_LargePageNumber() throws Exception {
        // Arrange
        SearchResultDTO<PinSearchResultDTO> emptyResult = new SearchResultDTO<>();
        emptyResult.setQuery("sunset");
        emptyResult.setResults(Collections.emptyList());
        emptyResult.setSuggestions(Collections.emptyList());
        emptyResult.setPagination(new PaginationDTO(1000, 100, 1000L, 10, false, true));
        emptyResult.setTotalResults(1000L);

        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(emptyResult);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset")
                        .param("page", "1000"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(1000));

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("Search All Types - Pins, Boards, Users")
    void testSearchAllTypes() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);
        when(searchService.searchBoards(any(SearchRequestDTO.class)))
                .thenReturn(boardSearchResultDTO);
        when(searchService.searchUsers(any(SearchRequestDTO.class)))
                .thenReturn(userSearchResultDTO);

        // Search pins
        mockMvc.perform(get("/search/pins")
                        .param("q", "test"))
                .andExpect(status().isOk());

        // Search boards
        mockMvc.perform(get("/search/boards")
                        .param("q", "test"))
                .andExpect(status().isOk());

        // Search users
        mockMvc.perform(get("/search/users")
                        .param("q", "test"))
                .andExpect(status().isOk());

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
        verify(searchService, times(1)).searchBoards(any(SearchRequestDTO.class));
        verify(searchService, times(1)).searchUsers(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("Suggestion Workflow - Get Suggestions Then Search")
    void testSuggestionWorkflow() throws Exception {
        // Arrange
        List<String> suggestions = Arrays.asList("sunset beach", "sunset photography");
        when(searchService.getSearchSuggestions("sun"))
                .thenReturn(suggestions);
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Get suggestions
        mockMvc.perform(get("/search/suggestions")
                        .param("q", "sun"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Search using suggestion
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset beach"))
                .andExpect(status().isOk());

        verify(searchService, times(1)).getSearchSuggestions("sun");
        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }

    @Test
    @DisplayName("Popular Pins with Different Page Sizes")
    void testPopularPins_DifferentPageSizes() throws Exception {
        // Arrange
        when(searchService.getPopularPins(0, 10))
                .thenReturn(paginatedPinResponse);
        when(searchService.getPopularPins(0, 50))
                .thenReturn(paginatedPinResponse);

        // Page size 10
        mockMvc.perform(get("/search/popular")
                        .param("size", "10"))
                .andExpect(status().isOk());

        // Page size 50
        mockMvc.perform(get("/search/popular")
                        .param("size", "50"))
                .andExpect(status().isOk());

        verify(searchService, times(1)).getPopularPins(0, 10);
        verify(searchService, times(1)).getPopularPins(0, 50);
    }

    @Test
    @DisplayName("Response Validation - All Search Result Fields Present")
    void testResponseValidation_AllFieldsPresent() throws Exception {
        // Arrange
        when(searchService.searchPins(any(SearchRequestDTO.class)))
                .thenReturn(pinSearchResultDTO);

        // Act & Assert
        mockMvc.perform(get("/search/pins")
                        .param("q", "sunset"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.query").exists())
                .andExpect(jsonPath("$.data.results").exists())
                .andExpect(jsonPath("$.data.suggestions").exists())
                .andExpect(jsonPath("$.data.pagination").exists())
                .andExpect(jsonPath("$.data.totalResults").exists())
                .andExpect(jsonPath("$.data.results[0].pinId").exists())
                .andExpect(jsonPath("$.data.results[0].title").exists())
                .andExpect(jsonPath("$.data.results[0].imageUrl").exists())
                .andExpect(jsonPath("$.data.results[0].saves").exists())
                .andExpect(jsonPath("$.data.results[0].likes").exists())
                .andExpect(jsonPath("$.data.results[0].relevanceScore").exists());

        verify(searchService, times(1)).searchPins(any(SearchRequestDTO.class));
    }
}
