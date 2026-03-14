package com.dogparkhomes.infrastructure.google;

import com.dogparkhomes.infrastructure.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreetViewServiceTest {

    @Mock
    private ImageService imageService;

    private StreetViewService streetViewService;

    @BeforeEach
    void setUp() {
        streetViewService = new StreetViewService(imageService);
        ReflectionTestUtils.setField(streetViewService, "apiKey", "test-api-key");
    }

    @Test
    void getStreetViewImage_returnsLocalUrl_whenStreetViewSucceeds() {
        String listingId = "listing-123";
        double lat = 37.7749;
        double lng = -122.4194;
        String expectedLocalUrl = "/images/123456.jpg";

        when(imageService.getOrDownloadImage(anyString(), anyString())).thenReturn(expectedLocalUrl);

        String result = streetViewService.getStreetViewImage(listingId, lat, lng);

        assertEquals(expectedLocalUrl, result);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageService).getOrDownloadImage(idCaptor.capture(), urlCaptor.capture());

        String capturedUrl = urlCaptor.getValue();
        assertTrue(capturedUrl.startsWith("https://maps.googleapis.com/maps/api/streetview"));
        assertTrue(capturedUrl.contains("size=800x600"));
        assertTrue(capturedUrl.contains("location=37.7749,-122.4194"));
        assertTrue(capturedUrl.contains("fov=80"));
        assertTrue(capturedUrl.contains("pitch=10"));
        assertTrue(capturedUrl.contains("key=test-api-key"));
    }

    @Test
    void getStreetViewImage_returnsFallbackUrl_whenStreetViewReturnsNull() {
        String listingId = "listing-456";
        double lat = 40.7128;
        double lng = -74.0060;
        String fallbackUrl = "/images/789012_p.jpg";

        when(imageService.getOrDownloadImage(anyString(), contains("maps.googleapis.com")))
                .thenReturn(null);
        when(imageService.getOrDownloadImage(anyString(), contains("picsum.photos")))
                .thenReturn(fallbackUrl);

        String result = streetViewService.getStreetViewImage(listingId, lat, lng);

        assertEquals(fallbackUrl, result);
        verify(imageService, times(2)).getOrDownloadImage(anyString(), anyString());
    }

    @Test
    void getStreetViewImage_returnsFallbackUrl_whenStreetViewThrows() {
        String listingId = "listing-789";
        double lat = 34.0522;
        double lng = -118.2437;
        String fallbackUrl = "/images/111222_p.jpg";

        when(imageService.getOrDownloadImage(anyString(), contains("maps.googleapis.com")))
                .thenThrow(new RuntimeException("Network error"));
        when(imageService.getOrDownloadImage(anyString(), contains("picsum.photos")))
                .thenReturn(fallbackUrl);

        String result = streetViewService.getStreetViewImage(listingId, lat, lng);

        assertEquals(fallbackUrl, result);
        verify(imageService, times(2)).getOrDownloadImage(anyString(), anyString());
    }

    @Test
    void getStreetViewImage_returnsNull_whenBothStreetViewAndFallbackFail() {
        String listingId = "listing-fail";
        double lat = 0.0;
        double lng = 0.0;

        when(imageService.getOrDownloadImage(anyString(), anyString()))
                .thenReturn(null)
                .thenReturn(null);

        String result = streetViewService.getStreetViewImage(listingId, lat, lng);

        assertNull(result);
        verify(imageService, times(2)).getOrDownloadImage(anyString(), anyString());
    }

    @Test
    void getStreetViewImage_returnsNull_whenStreetViewFailsAndFallbackThrows() {
        String listingId = "listing-fail";
        double lat = 0.0;
        double lng = 0.0;

        when(imageService.getOrDownloadImage(anyString(), contains("maps.googleapis.com")))
                .thenReturn(null);
        when(imageService.getOrDownloadImage(anyString(), contains("picsum.photos")))
                .thenThrow(new RuntimeException("Fallback failed"));

        String result = streetViewService.getStreetViewImage(listingId, lat, lng);

        assertNull(result);
        verify(imageService, times(2)).getOrDownloadImage(anyString(), anyString());
    }

    @Test
    void getStreetViewImage_usesStableBaseIdFromListingId() {
        String listingId = "same-listing";
        double lat = 1.0;
        double lng = 2.0;
        long expectedBaseId = Math.abs((long) listingId.hashCode());

        when(imageService.getOrDownloadImage(eq(String.valueOf(expectedBaseId)), anyString()))
                .thenReturn("/images/" + expectedBaseId + ".jpg");

        String result = streetViewService.getStreetViewImage(listingId, lat, lng);

        assertNotNull(result);
        verify(imageService).getOrDownloadImage(eq(String.valueOf(expectedBaseId)), anyString());
    }

    @Test
    void getStreetViewImage_fallbackUsesBaseIdWithSuffix() {
        String listingId = "listing";
        long baseId = Math.abs((long) listingId.hashCode());
        String expectedFallbackId = baseId + "_p";

        when(imageService.getOrDownloadImage(anyString(), contains("maps.googleapis.com")))
                .thenReturn(null);
        when(imageService.getOrDownloadImage(eq(expectedFallbackId), contains("picsum.photos")))
                .thenReturn("/images/" + expectedFallbackId + ".jpg");

        streetViewService.getStreetViewImage(listingId, 0.0, 0.0);

        verify(imageService).getOrDownloadImage(eq(expectedFallbackId), anyString());
    }
}
