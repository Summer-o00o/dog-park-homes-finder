package com.dogparkhomes.api.controller;

import com.dogparkhomes.api.dto.request.SearchRequestDto;
import com.dogparkhomes.api.dto.response.SearchFiltersDto;
import com.dogparkhomes.infrastructure.google.GooglePlacesService;
import com.dogparkhomes.infrastructure.nova.NovaService;
import com.dogparkhomes.infrastructure.realestate.RealEstateService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchControllerTest {

    @Test
    void usesInferredRadiusMilesWhenPresent() {
        NovaService novaService = mock(NovaService.class);
        GooglePlacesService googlePlacesService = mock(GooglePlacesService.class);
        RealEstateService realEstateService = mock(RealEstateService.class);

        SearchFiltersDto filters = new SearchFiltersDto();
        filters.setLocation("Austin");
        filters.setRadius_miles(4.0);
        when(novaService.parseUserQuery(eq("hello"))).thenReturn(filters);
        when(googlePlacesService.searchDogParks(eq("Austin"))).thenReturn(Collections.emptyList());
        when(realEstateService.searchHouses(any(), anyDouble(), any(), any())).thenReturn(Collections.emptyList());

        SearchController controller = new SearchController(novaService, googlePlacesService, realEstateService);

        SearchRequestDto request = new SearchRequestDto();
        request.setQuery("hello");

        controller.search(request);

        ArgumentCaptor<Double> radiusCaptor = ArgumentCaptor.forClass(Double.class);
        verify(realEstateService).searchHouses(any(), radiusCaptor.capture(), any(), any());
        assertThat(radiusCaptor.getValue()).isEqualTo(4.0);
    }

    @Test
    void defaultsToTwoMilesWhenNoRadiusProvidedOrInferred() {
        NovaService novaService = mock(NovaService.class);
        GooglePlacesService googlePlacesService = mock(GooglePlacesService.class);
        RealEstateService realEstateService = mock(RealEstateService.class);

        SearchFiltersDto filters = new SearchFiltersDto();
        filters.setLocation("Denver");
        filters.setRadius_miles(null);
        when(novaService.parseUserQuery(eq("hello"))).thenReturn(filters);
        when(googlePlacesService.searchDogParks(eq("Denver"))).thenReturn(Collections.emptyList());
        when(realEstateService.searchHouses(any(), anyDouble(), any(), any())).thenReturn(Collections.emptyList());

        SearchController controller = new SearchController(novaService, googlePlacesService, realEstateService);

        SearchRequestDto request = new SearchRequestDto();
        request.setQuery("hello");

        controller.search(request);

        ArgumentCaptor<Double> radiusCaptor = ArgumentCaptor.forClass(Double.class);
        verify(realEstateService).searchHouses(any(), radiusCaptor.capture(), any(), any());
        assertThat(radiusCaptor.getValue()).isEqualTo(2.0);
    }

    @Test
    void clampsInvalidInferredRadiusToDefault() {
        NovaService novaService = mock(NovaService.class);
        GooglePlacesService googlePlacesService = mock(GooglePlacesService.class);
        RealEstateService realEstateService = mock(RealEstateService.class);

        SearchFiltersDto filters = new SearchFiltersDto();
        filters.setLocation("Miami");
        filters.setRadius_miles(Double.NaN);
        when(novaService.parseUserQuery(eq("hello"))).thenReturn(filters);
        when(googlePlacesService.searchDogParks(eq("Miami"))).thenReturn(Collections.emptyList());
        when(realEstateService.searchHouses(any(), anyDouble(), any(), any())).thenReturn(Collections.emptyList());

        SearchController controller = new SearchController(novaService, googlePlacesService, realEstateService);

        SearchRequestDto request = new SearchRequestDto();
        request.setQuery("hello");

        controller.search(request);

        ArgumentCaptor<Double> radiusCaptor = ArgumentCaptor.forClass(Double.class);
        verify(realEstateService).searchHouses(any(), radiusCaptor.capture(), any(), any());
        assertThat(radiusCaptor.getValue()).isEqualTo(2.0);
    }
}

