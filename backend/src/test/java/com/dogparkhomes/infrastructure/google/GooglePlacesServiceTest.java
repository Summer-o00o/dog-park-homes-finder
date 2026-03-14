package com.dogparkhomes.infrastructure.google;

import com.dogparkhomes.api.dto.response.DogParkAnalysisDto;
import com.dogparkhomes.api.dto.response.DogParkDto;
import com.dogparkhomes.infrastructure.nova.NovaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GooglePlacesServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NovaService novaService;

    private GooglePlacesService googlePlacesService;

    @BeforeEach
    void setUp() {
        googlePlacesService = new GooglePlacesService(restTemplate, novaService);
        ReflectionTestUtils.setField(googlePlacesService, "apiKey", "test-api-key");
    }

    @Test
    void getReviews_returnsReviewTexts_whenResponseIsValid() {
        String placeId = "places/ChIJN1t_tDeuEmsRUsoyG83frY4";
        String responseBody = """
                {
                  "reviews": [
                    { "text": { "text": "Great park for dogs!" } },
                    { "text": { "text": "Very clean and spacious." } }
                  ]
                }
                """;

        when(restTemplate.exchange(
                eq("https://places.googleapis.com/v1/places/" + placeId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        List<String> result = googlePlacesService.getReviews(placeId);

        assertEquals(2, result.size());
        assertEquals("Great park for dogs!", result.get(0));
        assertEquals("Very clean and spacious.", result.get(1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), captor.capture(), eq(String.class));
        HttpHeaders headers = captor.getValue().getHeaders();
        assertEquals("test-api-key", headers.getFirst("X-Goog-Api-Key"));
        assertTrue(headers.getFirst("X-Goog-FieldMask").contains("reviews.text"));
    }

    @Test
    void getReviews_returnsEmptyList_whenNoReviews() {
        String placeId = "place-123";
        String responseBody = """
                { "reviews": [] }
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        List<String> result = googlePlacesService.getReviews(placeId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getReviews_throwsRuntimeException_whenResponseIsInvalidJson() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("not valid json {"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> googlePlacesService.getReviews("place-1"));
        assertTrue(thrown.getMessage().contains("Failed to parse reviews"));
        assertNotNull(thrown.getCause());
    }

    @Test
    void getReviews_throwsRuntimeException_whenExchangeFails() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        assertThrows(RuntimeException.class, () -> googlePlacesService.getReviews("place-1"));
    }

    @Test
    void searchDogParks_returnsParksWithRatingAtLeast48_andCallsNovaForAnalysis() {
        String searchResponseBody = """
                {
                  "places": [
                    {
                      "id": "places/park-1",
                      "displayName": { "text": "Happy Tails Park" },
                      "formattedAddress": "123 Dog St, Seattle",
                      "location": { "latitude": 47.6, "longitude": -122.3 },
                      "rating": 4.9,
                      "userRatingCount": 120
                    }
                  ]
                }
                """;
        String reviewsResponseBody = """
                {
                  "reviews": [
                    { "text": { "text": "Amazing park!" } }
                  ]
                }
                """;

        DogParkAnalysisDto analysis = new DogParkAnalysisDto();
        analysis.setParkingScore(8);
        analysis.setDogFriendlinessScore(10);

        when(restTemplate.exchange(
                eq("https://places.googleapis.com/v1/places:searchText"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok(searchResponseBody));

        when(restTemplate.exchange(
                contains("/places/places/park-1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok(reviewsResponseBody));

        when(novaService.analyzeDogParkReviews(eq("places/park-1"), anyList()))
                .thenReturn(analysis);

        List<DogParkDto> result = googlePlacesService.searchDogParks("Seattle");

        assertEquals(1, result.size());
        DogParkDto park = result.get(0);
        assertEquals("Happy Tails Park", park.getName());
        assertEquals("123 Dog St, Seattle", park.getAddress());
        assertEquals(47.6, park.getLatitude(), 0.0001);
        assertEquals(-122.3, park.getLongitude(), 0.0001);
        assertEquals(4.9, park.getRating(), 0.0001);
        assertEquals(120, park.getUserRatingCount());
        assertEquals(List.of("Amazing park!"), park.getReviews());
        assertSame(analysis, park.getAnalysis());

        verify(novaService).analyzeDogParkReviews(eq("places/park-1"), eq(List.of("Amazing park!")));
    }

    @Test
    void searchDogParks_filtersOutPlacesWithRatingBelow48() {
        String searchResponseBody = """
                {
                  "places": [
                    {
                      "id": "places/low-rating",
                      "displayName": { "text": "Low Rating Park" },
                      "formattedAddress": "456 Ave",
                      "location": { "latitude": 47.0, "longitude": -122.0 },
                      "rating": 4.5,
                      "userRatingCount": 50
                    }
                  ]
                }
                """;

        when(restTemplate.exchange(
                eq("https://places.googleapis.com/v1/places:searchText"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok(searchResponseBody));

        List<DogParkDto> result = googlePlacesService.searchDogParks("Seattle");

        assertTrue(result.isEmpty());
        verify(novaService, never()).analyzeDogParkReviews(anyString(), anyList());
    }

    @Test
    void searchDogParks_returnsAtMostTwoParks() {
        String searchResponseBody = """
                {
                  "places": [
                    {
                      "id": "places/park-1",
                      "displayName": { "text": "Park One" },
                      "formattedAddress": "Addr 1",
                      "location": { "latitude": 47.0, "longitude": -122.0 },
                      "rating": 4.9,
                      "userRatingCount": 10
                    },
                    {
                      "id": "places/park-2",
                      "displayName": { "text": "Park Two" },
                      "formattedAddress": "Addr 2",
                      "location": { "latitude": 47.1, "longitude": -122.1 },
                      "rating": 4.85,
                      "userRatingCount": 20
                    },
                    {
                      "id": "places/park-3",
                      "displayName": { "text": "Park Three" },
                      "formattedAddress": "Addr 3",
                      "location": { "latitude": 47.2, "longitude": -122.2 },
                      "rating": 5.0,
                      "userRatingCount": 30
                    }
                  ]
                }
                """;
        String reviewsBody = """
                { "reviews": [] }
                """;
        DogParkAnalysisDto analysis = new DogParkAnalysisDto();

        when(restTemplate.exchange(
                eq("https://places.googleapis.com/v1/places:searchText"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok(searchResponseBody));

        when(restTemplate.exchange(contains("/places/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(reviewsBody));

        when(novaService.analyzeDogParkReviews(anyString(), anyList())).thenReturn(analysis);

        List<DogParkDto> result = googlePlacesService.searchDogParks("Seattle");

        assertEquals(2, result.size());
        assertEquals("Park One", result.get(0).getName());
        assertEquals("Park Two", result.get(1).getName());
        verify(novaService, times(2)).analyzeDogParkReviews(anyString(), anyList());
    }

    @Test
    void searchDogParks_throwsRuntimeException_whenSearchResponseIsInvalidJson() {
        when(restTemplate.exchange(
                eq("https://places.googleapis.com/v1/places:searchText"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("invalid {"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> googlePlacesService.searchDogParks("Seattle"));
        assertTrue(thrown.getMessage().contains("Failed to parse Google Places response"));
        assertNotNull(thrown.getCause());
    }

    @Test
    void searchDogParks_sendsLocationInTextQuery() {
        when(restTemplate.exchange(
                eq("https://places.googleapis.com/v1/places:searchText"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"places\": []}"));

        googlePlacesService.searchDogParks("Portland, OR");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://places.googleapis.com/v1/places:searchText"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(String.class));
        assertTrue(captor.getValue().getBody().contains("dog park in Portland, OR"));
        assertEquals("test-api-key", captor.getValue().getHeaders().getFirst("X-Goog-Api-Key"));
    }
}
