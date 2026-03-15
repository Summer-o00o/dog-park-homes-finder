package com.dogparkhomes.api.controller;

import com.dogparkhomes.api.dto.request.SearchRequestDto;
import com.dogparkhomes.api.dto.response.ListingResponseDto;
import com.dogparkhomes.api.dto.response.SearchFiltersDto;
import com.dogparkhomes.api.dto.response.SearchResponseDto;
import com.dogparkhomes.infrastructure.nova.NovaService;
import com.dogparkhomes.infrastructure.google.GooglePlacesService;
import com.dogparkhomes.infrastructure.realestate.RealEstateService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dogparkhomes.api.dto.response.DogParkDto;
import com.dogparkhomes.api.exception.InvalidSearchQueryException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final NovaService novaService;
    private final GooglePlacesService googlePlacesService;
    private final RealEstateService realEstateService;
    private static final double DEFAULT_RADIUS_MILES = 2.0;

    public SearchController(NovaService novaService, GooglePlacesService googlePlacesService, RealEstateService realEstateService) {
        this.novaService = novaService;
        this.googlePlacesService = googlePlacesService;
        this.realEstateService = realEstateService;
    }

    @PostMapping("/search")
    public SearchResponseDto search(@RequestBody SearchRequestDto request) {
        String query = request.getQuery();
        if (query == null || query.isBlank()) {
            throw new InvalidSearchQueryException("Please enter a search query (e.g. city, ZIP code, or address).");
        }
        SearchFiltersDto filters = novaService.parseUserQuery(query);

        // Use Nova's valid flag and message for semantic validity (e.g. gibberish, no place name)
        if (Boolean.FALSE.equals(filters.getValid())) {
            String msg = filters.getMessage() != null && !filters.getMessage().isBlank()
                    ? filters.getMessage()
                    : "We couldn't identify a location. Please enter a city, ZIP code, or address.";
            throw new InvalidSearchQueryException(msg);
        }
        String location = filters.getLocation();
        if (location == null || location.isBlank()) {
            throw new InvalidSearchQueryException("We couldn't identify a location. Please enter a city, ZIP code, or address.");
        }

        // call Google Places API
        List<DogParkDto> parks = googlePlacesService.searchDogParks(location);

        double radiusMiles = resolveRadiusMiles(filters);

        // call Real Estate API
        List<ListingResponseDto> listings = realEstateService.searchHouses(parks, radiusMiles);
        return new SearchResponseDto(listings, parks);
    }

    private double resolveRadiusMiles(SearchFiltersDto filters) {
        Double inferredRadius = filters.getRadius_miles();
        if (inferredRadius != null) {
            return clampRadiusMiles(inferredRadius);
        }

        return DEFAULT_RADIUS_MILES;
    }

    private double clampRadiusMiles(Double radiusMiles) {
        if (radiusMiles == null || radiusMiles.isNaN() || radiusMiles.isInfinite()) {
            return DEFAULT_RADIUS_MILES;
        }
        // Keep within sane bounds to avoid huge API calls / accidental abuse
        double r = radiusMiles;
        if (r < 0.1) return 0.1;
        if (r > 50) return 50;
        return r;
    }
}
