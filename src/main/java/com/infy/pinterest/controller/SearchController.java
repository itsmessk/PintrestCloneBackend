package com.infy.pinterest.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.pinterest.dto.ApiResponse;
import com.infy.pinterest.dto.BoardSearchResultDTO;
import com.infy.pinterest.dto.PaginatedResponse;
import com.infy.pinterest.dto.PinSearchResultDTO;
import com.infy.pinterest.dto.SearchRequestDTO;
import com.infy.pinterest.dto.SearchResultDTO;
import com.infy.pinterest.dto.UserSearchResultDTO;
import com.infy.pinterest.service.SearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/search")
@Tag(name = "Search", description = "Search and discovery APIs")
@Slf4j
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/pins")
    @Operation(summary = "Search pins")
    public ResponseEntity<ApiResponse<SearchResultDTO<PinSearchResultDTO>>> searchPins(
            @RequestParam String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /search/pins - Query: {}", q);

        SearchRequestDTO searchRequest = new SearchRequestDTO();
        searchRequest.setQuery(q);
        searchRequest.setCategory(category);
        searchRequest.setSortBy(sort);
        searchRequest.setPage(page);
        searchRequest.setSize(size);

        SearchResultDTO<PinSearchResultDTO> results = searchService.searchPins(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", results));
    }

    @GetMapping("/boards")
    @Operation(summary = "Search boards")
    public ResponseEntity<ApiResponse<SearchResultDTO<BoardSearchResultDTO>>> searchBoards(
            @RequestParam String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /search/boards - Query: {}", q);

        SearchRequestDTO searchRequest = new SearchRequestDTO();
        searchRequest.setQuery(q);
        searchRequest.setCategory(category);
        searchRequest.setPage(page);
        searchRequest.setSize(size);

        SearchResultDTO<BoardSearchResultDTO> results =
                searchService.searchBoards(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", results));}

    @GetMapping("/users")
    @Operation(summary = "Search users")
    public ResponseEntity<ApiResponse<SearchResultDTO<UserSearchResultDTO>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /search/users - Query: {}", q);

        SearchRequestDTO searchRequest = new SearchRequestDTO();
        searchRequest.setQuery(q);
        searchRequest.setPage(page);
        searchRequest.setSize(size);

        SearchResultDTO<UserSearchResultDTO> results = searchService.searchUsers(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", results));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getSearchSuggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /search/suggestions - Query: {}", q);

        List<String> suggestions = searchService.getSearchSuggestions(q);
        return ResponseEntity.ok(ApiResponse.success("Suggestions retrieved successfully",
                suggestions));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular/trending pins")
    public ResponseEntity<ApiResponse<PaginatedResponse<PinSearchResultDTO>>> getPopularPins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /search/popular - Fetching popular pins");

        PaginatedResponse<PinSearchResultDTO> results = searchService.getPopularPins(page,
                size);
        return ResponseEntity.ok(ApiResponse.success("Popular pins retrieved successfully",
                results));
    }

}
