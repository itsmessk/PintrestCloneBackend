package com.infy.pinterest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.infy.pinterest.dto.BoardSearchResultDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PinSearchResultDTO;
import com.infy.pinterest.dto.SearchRequestDTO;
import com.infy.pinterest.dto.SearchResultDTO;
import com.infy.pinterest.dto.UserSearchResultDTO;
import com.infy.pinterest.entity.Board;
import com.infy.pinterest.entity.Pin;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.repository.BoardRepository;
import com.infy.pinterest.repository.PinRepository;
import com.infy.pinterest.repository.UserRepository;
import com.infy.pinterest.service.SearchService;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private PinRepository pinRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private SearchService searchService;

    private User testUser;
    private Pin testPin;
    private Board testBoard;
    private SearchRequestDTO searchRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setUserId("user-123");
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setProfilePictureUrl("https://example.com/profile.jpg");
        testUser.setBio("Test bio");
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

        // Setup test pin
        testPin = new Pin();
        testPin.setPinId("pin-123");
        testPin.setUserId("user-123");
        testPin.setBoardId("board-123");
        testPin.setTitle("Beautiful Sunset");
        testPin.setDescription("Amazing sunset view");
        testPin.setImageUrl("https://example.com/sunset.jpg");
        testPin.setVisibility(Pin.Visibility.PUBLIC);
        testPin.setIsDraft(false);
        testPin.setSaveCount(100);
        testPin.setLikeCount(50);
        testPin.setCreatedAt(LocalDateTime.now());

        // Setup test board
        testBoard = new Board();
        testBoard.setBoardId("board-123");
        testBoard.setUserId("user-123");
        testBoard.setName("Nature Photography");
        testBoard.setDescription("Beautiful nature photos");
        testBoard.setCategory("Photography");
        testBoard.setCoverImageUrl("https://example.com/cover.jpg");
        testBoard.setVisibility(Board.Visibility.PUBLIC);
        testBoard.setPinCount(25);
        testBoard.setCreatedAt(LocalDateTime.now());

        // Setup search request
        searchRequest = new SearchRequestDTO();
        searchRequest.setQuery("sunset");
        searchRequest.setPage(0);
        searchRequest.setSize(20);
        searchRequest.setSortBy("relevance");
    }

    // ==================== PIN SEARCH TESTS ====================

    @Test
    void testSearchPins_Success() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList("Sunset Beach", "Sunset Mountain"));

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals("sunset", result.getQuery());
        assertEquals(1, result.getResults().size());
        assertEquals(1L, result.getTotalResults());
        assertEquals(2, result.getSuggestions().size());

        PinSearchResultDTO pinResult = result.getResults().get(0);
        assertEquals("pin-123", pinResult.getPinId());
        assertEquals("Beautiful Sunset", pinResult.getTitle());
        assertEquals(100, pinResult.getSaves());
        assertEquals(50, pinResult.getLikes());
        assertNotNull(pinResult.getCreatedBy());

        verify(pinRepository).searchPins(eq("sunset"), any(Pageable.class));
        verify(userRepository).findById("user-123");
    }

    @Test
    void testSearchPins_WithCategory() {
        // Arrange
        searchRequest.setCategory("Photography");
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPinsByCategory(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        verify(pinRepository).searchPinsByCategory(eq("sunset"), eq("Photography"), any(Pageable.class));
    }

    @Test
    void testSearchPins_WithPopularSort() {
        // Arrange
        searchRequest.setSortBy("popular");
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        verify(pinRepository).searchPins(eq("sunset"), any(Pageable.class));
    }

    @Test
    void testSearchPins_WithRecentSort() {
        // Arrange
        searchRequest.setSortBy("recent");
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        verify(pinRepository).searchPins(eq("sunset"), any(Pageable.class));
    }

    @Test
    void testSearchPins_NoResults() {
        // Arrange
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(emptyPage);
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getResults().size());
        assertEquals(0L, result.getTotalResults());
        verify(pinRepository).searchPins(eq("sunset"), any(Pageable.class));
    }

    @Test
    void testSearchPins_UserNotFound() {
        // Arrange
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertNull(result.getResults().get(0).getCreatedBy());
    }

    @Test
    void testSearchPins_Pagination() {
        // Arrange
        searchRequest.setPage(1);
        searchRequest.setSize(10);

        Pin pin2 = new Pin();
        pin2.setPinId("pin-456");
        pin2.setUserId("user-123");
        pin2.setBoardId("board-123");
        pin2.setTitle("Sunset Paradise");
        pin2.setDescription("Another sunset");
        pin2.setImageUrl("https://example.com/sunset2.jpg");
        pin2.setVisibility(Pin.Visibility.PUBLIC);
        pin2.setIsDraft(false);
        pin2.setSaveCount(80);
        pin2.setLikeCount(40);

        List<Pin> pins = Arrays.asList(pin2);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(1, 10), 25);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getPagination());
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(3, result.getPagination().getTotalPages());
        assertEquals(25L, result.getPagination().getTotalItems());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    @Test
    void testSearchPins_TrimWhitespace() {
        // Arrange
        searchRequest.setQuery("  sunset  ");
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertEquals("sunset", result.getQuery());
        verify(pinRepository).searchPins(eq("sunset"), any(Pageable.class));
    }

    // ==================== BOARD SEARCH TESTS ====================

    @Test
    void testSearchBoards_Success() {
        // Arrange
        searchRequest.setQuery("nature");
        List<Board> boards = Arrays.asList(testBoard);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 20), 1);

        when(boardRepository.searchBoards(anyString(), any(Pageable.class))).thenReturn(boardPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findBoardNameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList("Nature Photography", "Nature Art"));

        // Act
        SearchResultDTO<BoardSearchResultDTO> result = searchService.searchBoards(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals("nature", result.getQuery());
        assertEquals(1, result.getResults().size());
        assertEquals(1L, result.getTotalResults());
        assertEquals(2, result.getSuggestions().size());

        BoardSearchResultDTO boardResult = result.getResults().get(0);
        assertEquals("board-123", boardResult.getBoardId());
        assertEquals("Nature Photography", boardResult.getName());
        assertEquals(25, boardResult.getPinCount());
        assertNotNull(boardResult.getCreatedBy());

        verify(boardRepository).searchBoards(eq("nature"), any(Pageable.class));
    }

    @Test
    void testSearchBoards_WithCategory() {
        // Arrange
        searchRequest.setQuery("nature");
        searchRequest.setCategory("Photography");
        List<Board> boards = Arrays.asList(testBoard);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 20), 1);

        when(boardRepository.searchBoardsByCategory(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(boardPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findBoardNameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<BoardSearchResultDTO> result = searchService.searchBoards(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        verify(boardRepository).searchBoardsByCategory(eq("nature"), eq("Photography"), any(Pageable.class));
    }

    @Test
    void testSearchBoards_NoResults() {
        // Arrange
        searchRequest.setQuery("nonexistent");
        Page<Board> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(boardRepository.searchBoards(anyString(), any(Pageable.class))).thenReturn(emptyPage);
        when(boardRepository.findBoardNameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<BoardSearchResultDTO> result = searchService.searchBoards(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getResults().size());
        assertEquals(0L, result.getTotalResults());
    }

    @Test
    void testSearchBoards_UserNotFound() {
        // Arrange
        searchRequest.setQuery("nature");
        List<Board> boards = Arrays.asList(testBoard);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 20), 1);

        when(boardRepository.searchBoards(anyString(), any(Pageable.class))).thenReturn(boardPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());
        when(boardRepository.findBoardNameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<BoardSearchResultDTO> result = searchService.searchBoards(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertNull(result.getResults().get(0).getCreatedBy());
    }

    // ==================== USER SEARCH TESTS ====================

    @Test
    void testSearchUsers_Success() {
        // Arrange
        searchRequest.setQuery("test");
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 1);

        when(userRepository.searchUsers(anyString(), any(Pageable.class))).thenReturn(userPage);
        when(userRepository.findUsernameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList("testuser", "testuser2"));
        when(pinRepository.countByUserId("user-123")).thenReturn(15L);

        // Act
        SearchResultDTO<UserSearchResultDTO> result = searchService.searchUsers(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals("test", result.getQuery());
        assertEquals(1, result.getResults().size());
        assertEquals(1L, result.getTotalResults());
        assertEquals(2, result.getSuggestions().size());

        UserSearchResultDTO userResult = result.getResults().get(0);
        assertEquals("user-123", userResult.getUserId());
        assertEquals("testuser", userResult.getUsername());
        assertEquals("Test User", userResult.getFullName());
        assertEquals(15, userResult.getPinCount());

        verify(userRepository).searchUsers(eq("test"), any(Pageable.class));
        verify(pinRepository).countByUserId("user-123");
    }

    @Test
    void testSearchUsers_NoResults() {
        // Arrange
        searchRequest.setQuery("nonexistent");
        Page<User> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(userRepository.searchUsers(anyString(), any(Pageable.class))).thenReturn(emptyPage);
        when(userRepository.findUsernameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<UserSearchResultDTO> result = searchService.searchUsers(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getResults().size());
        assertEquals(0L, result.getTotalResults());
    }

    @Test
    void testSearchUsers_MultiplePins() {
        // Arrange
        searchRequest.setQuery("test");
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 1);

        when(userRepository.searchUsers(anyString(), any(Pageable.class))).thenReturn(userPage);
        when(userRepository.findUsernameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());
        when(pinRepository.countByUserId("user-123")).thenReturn(150L);

        // Act
        SearchResultDTO<UserSearchResultDTO> result = searchService.searchUsers(searchRequest);

        // Assert
        UserSearchResultDTO userResult = result.getResults().get(0);
        assertEquals(150, userResult.getPinCount());
    }

    // ==================== SUGGESTION TESTS ====================

    @Test
    void testGetSearchSuggestions_Success() {
        // Arrange
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList("Sunset Beach", "Sunset Mountain", "Sunset City"));

        // Act
        List<String> suggestions = searchService.getSearchSuggestions("sun");

        // Assert
        assertNotNull(suggestions);
        assertEquals(3, suggestions.size());
        assertTrue(suggestions.contains("Sunset Beach"));
        verify(pinRepository).findTitleSuggestions(eq("sun"), any(Pageable.class));
    }

    @Test
    void testGetSearchSuggestions_EmptyKeyword() {
        // Act
        List<String> suggestions = searchService.getSearchSuggestions("");

        // Assert
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
        verify(pinRepository, never()).findTitleSuggestions(anyString(), any(Pageable.class));
    }

    @Test
    void testGetSearchSuggestions_NullKeyword() {
        // Act
        List<String> suggestions = searchService.getSearchSuggestions(null);

        // Assert
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
        verify(pinRepository, never()).findTitleSuggestions(anyString(), any(Pageable.class));
    }

    @Test
    void testGetSearchSuggestions_ShortKeyword() {
        // Act
        List<String> suggestions = searchService.getSearchSuggestions("a");

        // Assert
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
        verify(pinRepository, never()).findTitleSuggestions(anyString(), any(Pageable.class));
    }

    @Test
    void testGetSearchSuggestions_WithDuplicates() {
        // Arrange
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList("Sunset", "Sunset Beach", "Sunset", "Sunset Mountain"));

        // Act
        List<String> suggestions = searchService.getSearchSuggestions("sun");

        // Assert
        assertNotNull(suggestions);
        assertEquals(3, suggestions.size());
    }

    @Test
    void testGetSearchSuggestions_LimitTo10() {
        // Arrange
        List<String> manySuggestions = Arrays.asList(
                "Item1", "Item2", "Item3", "Item4", "Item5",
                "Item6", "Item7", "Item8", "Item9", "Item10",
                "Item11", "Item12", "Item13"
        );
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(manySuggestions);

        // Act
        List<String> suggestions = searchService.getSearchSuggestions("item");

        // Assert
        assertNotNull(suggestions);
        assertEquals(10, suggestions.size());
    }

    @Test
    void testGetBoardSuggestions_Success() {
        // Arrange
        when(boardRepository.findBoardNameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList("Nature Photography", "Nature Art"));

        // Act
        List<String> suggestions = searchService.getBoardSuggestions("nat");

        // Assert
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        verify(boardRepository).findBoardNameSuggestions(eq("nat"), any(Pageable.class));
    }

    @Test
    void testGetBoardSuggestions_EmptyKeyword() {
        // Act
        List<String> suggestions = searchService.getBoardSuggestions("");

        // Assert
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
        verify(boardRepository, never()).findBoardNameSuggestions(anyString(), any(Pageable.class));
    }

    @Test
    void testGetUsernameSuggestions_Success() {
        // Arrange
        when(userRepository.findUsernameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList("testuser", "testuser2"));

        // Act
        List<String> suggestions = searchService.getUsernameSuggestions("test");

        // Assert
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        verify(userRepository).findUsernameSuggestions(eq("test"), any(Pageable.class));
    }

    @Test
    void testGetUsernameSuggestions_NullKeyword() {
        // Act
        List<String> suggestions = searchService.getUsernameSuggestions(null);

        // Assert
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
        verify(userRepository, never()).findUsernameSuggestions(anyString(), any(Pageable.class));
    }

    // ==================== POPULAR PINS TESTS ====================

    @Test
    void testGetPopularPins_Success() {
        // Arrange
        Pin popularPin = new Pin();
        popularPin.setPinId("pin-popular");
        popularPin.setUserId("user-123");
        popularPin.setBoardId("board-123");
        popularPin.setTitle("Viral Pin");
        popularPin.setDescription("Very popular");
        popularPin.setImageUrl("https://example.com/viral.jpg");
        popularPin.setVisibility(Pin.Visibility.PUBLIC);
        popularPin.setIsDraft(false);
        popularPin.setSaveCount(1000);
        popularPin.setLikeCount(500);

        List<Pin> pins = Arrays.asList(popularPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 10), 1);

        when(pinRepository.findPopularPins(any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));

        // Act
        PaginatedResponse<PinSearchResultDTO> result = searchService.getPopularPins(0, 10);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        
        PinSearchResultDTO pinResult = result.getData().get(0);
        assertEquals("pin-popular", pinResult.getPinId());
        assertEquals(1000, pinResult.getSaves());
        assertEquals(500, pinResult.getLikes());

        assertNotNull(result.getPagination());
        assertEquals(0, result.getPagination().getCurrentPage());
        assertEquals(1, result.getPagination().getTotalPages());

        verify(pinRepository).findPopularPins(any(Pageable.class));
    }

    @Test
    void testGetPopularPins_EmptyResults() {
        // Arrange
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);

        when(pinRepository.findPopularPins(any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PaginatedResponse<PinSearchResultDTO> result = searchService.getPopularPins(0, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPagination().getTotalPages());
    }

    @Test
    void testGetPopularPins_Pagination() {
        // Arrange
        Pin pin1 = new Pin();
        pin1.setPinId("pin-1");
        pin1.setUserId("user-123");
        pin1.setBoardId("board-123");
        pin1.setTitle("Pin 1");
        pin1.setImageUrl("https://example.com/1.jpg");
        pin1.setVisibility(Pin.Visibility.PUBLIC);
        pin1.setIsDraft(false);
        pin1.setSaveCount(100);
        pin1.setLikeCount(50);

        List<Pin> pins = Arrays.asList(pin1);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(1, 10), 25);

        when(pinRepository.findPopularPins(any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));

        // Act
        PaginatedResponse<PinSearchResultDTO> result = searchService.getPopularPins(1, 10);

        // Assert
        assertNotNull(result.getPagination());
        assertEquals(1, result.getPagination().getCurrentPage());
        assertEquals(3, result.getPagination().getTotalPages());
        assertTrue(result.getPagination().getHasNext());
        assertTrue(result.getPagination().getHasPrevious());
    }

    // ==================== RELEVANCE SCORE TESTS ====================

    @Test
    void testCalculateRelevanceScore_HighEngagement() {
        // Arrange
        Pin highEngagementPin = new Pin();
        highEngagementPin.setSaveCount(80);
        highEngagementPin.setLikeCount(70);

        // When converting this pin in actual searchPins method
        List<Pin> pins = Arrays.asList(highEngagementPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert - relevance score should be capped at 1.0
        assertNotNull(result);
        PinSearchResultDTO pinResult = result.getResults().get(0);
        assertNotNull(pinResult.getRelevanceScore());
        assertTrue(pinResult.getRelevanceScore() <= 1.0);
    }

    @Test
    void testCalculateRelevanceScore_LowEngagement() {
        // Arrange
        Pin lowEngagementPin = new Pin();
        lowEngagementPin.setPinId("pin-low");
        lowEngagementPin.setUserId("user-123");
        lowEngagementPin.setBoardId("board-123");
        lowEngagementPin.setTitle("Low Engagement");
        lowEngagementPin.setImageUrl("https://example.com/low.jpg");
        lowEngagementPin.setVisibility(Pin.Visibility.PUBLIC);
        lowEngagementPin.setIsDraft(false);
        lowEngagementPin.setSaveCount(5);
        lowEngagementPin.setLikeCount(3);

        List<Pin> pins = Arrays.asList(lowEngagementPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        PinSearchResultDTO pinResult = result.getResults().get(0);
        assertNotNull(pinResult.getRelevanceScore());
        assertTrue(pinResult.getRelevanceScore() < 0.1);
    }

    // ==================== EDGE CASES AND ERROR SCENARIOS ====================

    @Test
    void testSearchPins_MultipleResults() {
        // Arrange
        Pin pin1 = new Pin();
        pin1.setPinId("pin-1");
        pin1.setUserId("user-123");
        pin1.setBoardId("board-123");
        pin1.setTitle("Sunset 1");
        pin1.setImageUrl("https://example.com/1.jpg");
        pin1.setVisibility(Pin.Visibility.PUBLIC);
        pin1.setIsDraft(false);
        pin1.setSaveCount(100);
        pin1.setLikeCount(50);

        Pin pin2 = new Pin();
        pin2.setPinId("pin-2");
        pin2.setUserId("user-456");
        pin2.setBoardId("board-456");
        pin2.setTitle("Sunset 2");
        pin2.setImageUrl("https://example.com/2.jpg");
        pin2.setVisibility(Pin.Visibility.PUBLIC);
        pin2.setIsDraft(false);
        pin2.setSaveCount(80);
        pin2.setLikeCount(40);

        List<Pin> pins = Arrays.asList(pin1, pin2);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 2);

        User user2 = new User();
        user2.setUserId("user-456");
        user2.setUsername("user2");
        user2.setFullName("User Two");

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.findById("user-456")).thenReturn(Optional.of(user2));
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getResults().size());
        assertEquals(2L, result.getTotalResults());
        verify(userRepository, times(2)).findById(anyString());
    }

    @Test
    void testSearchBoards_WithWhitespaceInQuery() {
        // Arrange
        searchRequest.setQuery("  nature photography  ");
        List<Board> boards = Arrays.asList(testBoard);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 20), 1);

        when(boardRepository.searchBoards(anyString(), any(Pageable.class))).thenReturn(boardPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findBoardNameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<BoardSearchResultDTO> result = searchService.searchBoards(searchRequest);

        // Assert
        assertEquals("nature photography", result.getQuery());
        verify(boardRepository).searchBoards(eq("nature photography"), any(Pageable.class));
    }

    @Test
    void testSearchUsers_WithSpecialCharacters() {
        // Arrange
        searchRequest.setQuery("test@user");
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 1);

        when(userRepository.searchUsers(anyString(), any(Pageable.class))).thenReturn(userPage);
        when(userRepository.findUsernameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());
        when(pinRepository.countByUserId("user-123")).thenReturn(10L);

        // Act
        SearchResultDTO<UserSearchResultDTO> result = searchService.searchUsers(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals("test@user", result.getQuery());
        verify(userRepository).searchUsers(eq("test@user"), any(Pageable.class));
    }

    @Test
    void testSearchPins_LargePage() {
        // Arrange
        searchRequest.setPage(10);
        searchRequest.setSize(50);
        
        Page<Pin> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(10, 50), 0);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(emptyPage);
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getPagination().getCurrentPage());
        assertEquals(50, result.getPagination().getPageSize());
    }

    @Test
    void testGetSearchSuggestions_TrimWhitespace() {
        // Arrange
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList("Sunset"));

        // Act
        List<String> suggestions = searchService.getSearchSuggestions("  sun  ");

        // Assert
        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        verify(pinRepository).findTitleSuggestions(eq("sun"), any(Pageable.class));
    }

    @Test
    void testSearchPins_EmptyCategoryFilterIgnored() {
        // Arrange
        searchRequest.setCategory("");
        List<Pin> pins = Arrays.asList(testPin);
        Page<Pin> pinPage = new PageImpl<>(pins, PageRequest.of(0, 20), 1);

        when(pinRepository.searchPins(anyString(), any(Pageable.class))).thenReturn(pinPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(pinRepository.findTitleSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<PinSearchResultDTO> result = searchService.searchPins(searchRequest);

        // Assert
        assertNotNull(result);
        verify(pinRepository).searchPins(eq("sunset"), any(Pageable.class));
        verify(pinRepository, never()).searchPinsByCategory(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void testSearchBoards_EmptyCategoryFilterIgnored() {
        // Arrange
        searchRequest.setQuery("nature");
        searchRequest.setCategory("");
        List<Board> boards = Arrays.asList(testBoard);
        Page<Board> boardPage = new PageImpl<>(boards, PageRequest.of(0, 20), 1);

        when(boardRepository.searchBoards(anyString(), any(Pageable.class))).thenReturn(boardPage);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(boardRepository.findBoardNameSuggestions(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // Act
        SearchResultDTO<BoardSearchResultDTO> result = searchService.searchBoards(searchRequest);

        // Assert
        verify(boardRepository).searchBoards(eq("nature"), any(Pageable.class));
        verify(boardRepository, never()).searchBoardsByCategory(anyString(), anyString(), any(Pageable.class));
    }
}
