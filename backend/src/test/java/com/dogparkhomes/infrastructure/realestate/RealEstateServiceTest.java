package com.dogparkhomes.infrastructure.realestate;

import com.dogparkhomes.api.dto.response.DogParkDto;
import com.dogparkhomes.api.dto.response.ListingResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealEstateServiceTest {

    @Mock
    private RentCastClient rentCastClient;

    @InjectMocks
    private RealEstateService realEstateService;

    @Test
    void searchHouses_callsClientAndPopulatesNearestFields_forListingsWithinRadius() {
        DogParkDto park = new DogParkDto();
        park.setName("Great Dog Park");
        park.setRating(4.9);
        park.setLatitude(37.0);
        park.setLongitude(-122.0);

        ListingResponseDto within = new ListingResponseDto();
        within.setId("within");
        within.setLatitude(37.0);
        within.setLongitude(-122.0); // same point => 0.0 miles

        when(rentCastClient.fetchListings(37.0, -122.0, 2.0)).thenReturn(List.of(within));

        List<ListingResponseDto> result = realEstateService.searchHouses(List.of(park), 2.0);

        assertEquals(1, result.size());
        ListingResponseDto listing = result.get(0);
        assertEquals("within", listing.getId());
        assertEquals("Great Dog Park", listing.getNearestDogParkName());
        assertEquals(4.9, listing.getNearestDogParkRating(), 0.000001);
        assertEquals(0.0, listing.getDistanceToDogPark(), 0.0);

        verify(rentCastClient, times(1)).fetchListings(37.0, -122.0, 2.0);
        verifyNoMoreInteractions(rentCastClient);
    }

    @Test
    void searchHouses_filtersOutListingsOutsideRadius() {
        DogParkDto park = new DogParkDto();
        park.setName("Park");
        park.setRating(5.0);
        park.setLatitude(37.0);
        park.setLongitude(-122.0);

        ListingResponseDto outside = new ListingResponseDto();
        outside.setId("outside");
        outside.setLatitude(37.05); // ~3.45 miles north
        outside.setLongitude(-122.0);

        when(rentCastClient.fetchListings(37.0, -122.0, 2.0)).thenReturn(List.of(outside));

        List<ListingResponseDto> result = realEstateService.searchHouses(List.of(park), 2.0);

        assertTrue(result.isEmpty(), "Expected listings beyond radius to be filtered out");
        assertNull(outside.getNearestDogParkName(), "Filtered listings should not be mutated with nearest park fields");

        verify(rentCastClient, times(1)).fetchListings(37.0, -122.0, 2.0);
        verifyNoMoreInteractions(rentCastClient);
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

        ListingResponseDto a1 = new ListingResponseDto();
        a1.setId("a1");
        a1.setLatitude(10.0);
        a1.setLongitude(20.0);

        ListingResponseDto b1 = new ListingResponseDto();
        b1.setId("b1");
        b1.setLatitude(11.0);
        b1.setLongitude(21.0);

        when(rentCastClient.fetchListings(10.0, 20.0, 2.0)).thenReturn(List.of(a1));
        when(rentCastClient.fetchListings(11.0, 21.0, 2.0)).thenReturn(List.of(b1));

        List<ListingResponseDto> result = realEstateService.searchHouses(List.of(parkA, parkB), 2.0);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(l -> "a1".equals(l.getId()) && "Park A".equals(l.getNearestDogParkName())));
        assertTrue(result.stream().anyMatch(l -> "b1".equals(l.getId()) && "Park B".equals(l.getNearestDogParkName())));

        verify(rentCastClient, times(1)).fetchListings(10.0, 20.0, 2.0);
        verify(rentCastClient, times(1)).fetchListings(11.0, 21.0, 2.0);
        verifyNoMoreInteractions(rentCastClient);
    }
}

