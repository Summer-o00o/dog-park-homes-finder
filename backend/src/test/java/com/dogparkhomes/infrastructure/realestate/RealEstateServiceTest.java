package com.dogparkhomes.infrastructure.realestate;

import com.dogparkhomes.api.dto.response.DogParkDto;
import com.dogparkhomes.api.dto.response.ListingResponseDto;
import com.dogparkhomes.infrastructure.google.StreetViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealEstateServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StreetViewService streetViewService;

    @Mock
    private CacheManager cacheManager;

    private RealEstateService realEstateService;

    private static String listingJson(String id, double lat, double lon) {
        return """
                [{"id":"%s","formattedAddress":"123 Main St","price":500000,"bedrooms":2,"bathrooms":1.5,"latitude":%f,"longitude":%f}]
                """.formatted(id, lat, lon);
    }

    @BeforeEach
    void setUp() {
        realEstateService = new RealEstateService(restTemplate, streetViewService, cacheManager);
        when(streetViewService.getStreetViewImage(anyString(), anyDouble(), anyDouble())).thenReturn("/images/placeholder.jpg");
    }

    @Test
    void searchHouses_callsClientAndPopulatesNearestFields_forListingsWithinRadius() {
        DogParkDto park = new DogParkDto();
        park.setName("Great Dog Park");
        park.setRating(4.9);
        park.setLatitude(37.0);
        park.setLongitude(-122.0);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(listingJson("within", 37.0, -122.0)));

        List<ListingResponseDto> result = realEstateService.searchHouses(List.of(park), 2.0, null, null);

        assertEquals(1, result.size());
        ListingResponseDto listing = result.get(0);
        assertEquals("within", listing.getId());
        assertEquals("Great Dog Park", listing.getNearestDogParkName());
        assertEquals(4.9, listing.getNearestDogParkRating(), 0.000001);
        assertEquals(0.0, listing.getDistanceToDogPark(), 0.0);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class));
    }

    @Test
    void searchHouses_filtersOutListingsOutsideRadius() {
        DogParkDto park = new DogParkDto();
        park.setName("Park");
        park.setRating(5.0);
        park.setLatitude(37.0);
        park.setLongitude(-122.0);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(listingJson("outside", 37.05, -122.0)));

        List<ListingResponseDto> result = realEstateService.searchHouses(List.of(park), 2.0, null, null);

        assertTrue(result.isEmpty(), "Expected listings beyond radius to be filtered out");
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class));
    }

    @Test
    void searchHouses_aggregatesAcrossMultipleParks() {
        DogParkDto parkA = new DogParkDto();
        parkA.setName("Park A");
        parkA.setRating(4.8);
        parkA.setLatitude(10.0);
        parkA.setLongitude(20.0);

        DogParkDto parkB = new DogParkDto();
        parkB.setName("Park B");
        parkB.setRating(4.95);
        parkB.setLatitude(11.0);
        parkB.setLongitude(21.0);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(listingJson("a1", 10.0, 20.0)))
                .thenReturn(ResponseEntity.ok(listingJson("b1", 11.0, 21.0)));

        List<ListingResponseDto> result = realEstateService.searchHouses(List.of(parkA, parkB), 2.0, null, null);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(l -> "a1".equals(l.getId()) && "Park A".equals(l.getNearestDogParkName())));
        assertTrue(result.stream().anyMatch(l -> "b1".equals(l.getId()) && "Park B".equals(l.getNearestDogParkName())));
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class));
    }
}
