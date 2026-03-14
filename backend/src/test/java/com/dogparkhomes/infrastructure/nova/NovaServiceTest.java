package com.dogparkhomes.infrastructure.nova;

import com.dogparkhomes.api.dto.response.DogParkAnalysisDto;
import com.dogparkhomes.api.dto.response.SearchFiltersDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NovaServiceTest {

    @Mock
    private BedrockRuntimeClient bedrockRuntimeClient;

    @InjectMocks
    private NovaService novaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseUserQuery_returnsSearchFilters_whenBedrockReturnsValidJson() throws Exception {
        SearchFiltersDto expected = new SearchFiltersDto();
        expected.setLocation("Seattle");
        expected.setProperty_type("house");
        expected.setAmenities(List.of("yard", "fence"));
        expected.setPrice_range("under 2000");
        expected.setRadius_miles(2.0);

        String filtersJson = objectMapper.writeValueAsString(expected);
        String responseBody = buildBedrockResponseBody(filtersJson);

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(InvokeModelResponse.builder()
                        .body(SdkBytes.fromString(responseBody, StandardCharsets.UTF_8))
                        .build());

        SearchFiltersDto result = novaService.parseUserQuery("houses with yard in Seattle within 2 miles");

        assertNotNull(result);
        assertEquals("Seattle", result.getLocation());
        assertEquals("house", result.getProperty_type());
        assertEquals(List.of("yard", "fence"), result.getAmenities());
        assertEquals("under 2000", result.getPrice_range());
        assertEquals(2.0, result.getRadius_miles());

        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        verify(bedrockRuntimeClient, times(1)).invokeModel(captor.capture());
        assertEquals("us.amazon.nova-2-lite-v1:0", captor.getValue().modelId());
        assertTrue(captor.getValue().body().asUtf8String().contains("Seattle"));
    }

    @Test
    void parseUserQuery_stripsMarkdownCodeBlock_fromBedrockResponse() throws Exception {
        SearchFiltersDto expected = new SearchFiltersDto();
        expected.setLocation("Portland");
        expected.setProperty_type("apartment");
        expected.setRadius_miles(null);

        String filtersJson = objectMapper.writeValueAsString(expected);
        String wrappedInMarkdown = "```json\n" + filtersJson + "\n```";
        String responseBody = buildBedrockResponseBody(wrappedInMarkdown);

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(InvokeModelResponse.builder()
                        .body(SdkBytes.fromString(responseBody, StandardCharsets.UTF_8))
                        .build());

        SearchFiltersDto result = novaService.parseUserQuery("apartment in Portland");

        assertNotNull(result);
        assertEquals("Portland", result.getLocation());
        assertEquals("apartment", result.getProperty_type());
        assertNull(result.getRadius_miles());
    }

    @Test
    void parseUserQuery_throwsRuntimeException_whenBedrockFails() {
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(new RuntimeException("Bedrock unavailable"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> novaService.parseUserQuery("houses in Seattle"));
        assertTrue(thrown.getMessage().contains("Error calling Nova"));
        assertNotNull(thrown.getCause());
    }

    @Test
    void analyzeDogParkReviews_returnsScores_whenBedrockReturnsValidJson() throws Exception {
        DogParkAnalysisDto expected = new DogParkAnalysisDto();
        expected.setParkingScore(8);
        expected.setCrowdedScore(6);
        expected.setCleanlinessScore(9);
        expected.setDogFriendlinessScore(10);
        expected.setParkSizeScore(7);

        String analysisJson = objectMapper.writeValueAsString(expected);
        String responseBody = buildBedrockResponseBody(analysisJson);

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(InvokeModelResponse.builder()
                        .body(SdkBytes.fromString(responseBody, StandardCharsets.UTF_8))
                        .build());

        List<String> reviews = List.of("Great parking", "Can get crowded", "Very clean");
        DogParkAnalysisDto result = novaService.analyzeDogParkReviews("place-123", reviews);

        assertNotNull(result);
        assertEquals(8, result.getParkingScore());
        assertEquals(6, result.getCrowdedScore());
        assertEquals(9, result.getCleanlinessScore());
        assertEquals(10, result.getDogFriendlinessScore());
        assertEquals(7, result.getParkSizeScore());

        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        verify(bedrockRuntimeClient, times(1)).invokeModel(captor.capture());
        assertEquals("us.amazon.nova-2-lite-v1:0", captor.getValue().modelId());
        assertTrue(captor.getValue().body().asUtf8String().contains("Great parking"));
    }

    @Test
    void analyzeDogParkReviews_throwsRuntimeException_whenBedrockFails() {
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(new RuntimeException("Bedrock error"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> novaService.analyzeDogParkReviews("place-1", List.of("review one")));
        assertTrue(thrown.getMessage().contains("Nova dog park analysis failed"));
        assertNotNull(thrown.getCause());
    }

    private String buildBedrockResponseBody(String textContent) throws Exception {
        Map<String, Object> contentItem = Map.of("text", textContent);
        Map<String, Object> message = Map.of("content", List.of(contentItem));
        Map<String, Object> output = Map.of("message", message);
        Map<String, Object> root = Map.of("output", output);
        return objectMapper.writeValueAsString(root);
    }
}
