package com.dogparkhomes.infrastructure.nova;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dogparkhomes.api.dto.response.SearchFiltersDto;
import java.util.List;
import com.dogparkhomes.api.dto.response.DogParkAnalysisDto;
import java.nio.charset.StandardCharsets;
import org.springframework.cache.annotation.Cacheable;

@Service
public class NovaService {

    private final BedrockRuntimeClient bedrockRuntimeClient;

    public NovaService(BedrockRuntimeClient bedrockRuntimeClient) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
    }

    //analyze the user query and return the search filters
    public SearchFiltersDto parseUserQuery(String query) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            String prompt = """
                    You are a real estate search assistant. Extract structured search filters from the user query. \
                    Return ONLY valid JSON with exactly these field names (no other names): location, property_type, amenities, price_range, radius_miles. \
                    Use "amenities" for any amenity list (not nearby_amenities or similar). Do not explain. \
                    If the user specifies a search radius (e.g., "within 2 miles", "5 km"), set radius_miles to a NUMBER in miles (convert km to miles). \
                    If no radius is requested, set radius_miles to null. \
                    User query: %s
                    """.formatted(query);

            // JSON-escape the prompt so that any quotes/newlines in the user query don't break the request body
            String promptJsonString = mapper.writeValueAsString(prompt);

            String requestBody = """
            {
              "messages": [
                {
                  "role": "user",
                  "content": [
                    {
                      "text": %s
                    }
                  ]
                }
              ],
              "inferenceConfig": {
                "maxTokens": 200,
                "temperature": 0.1
              }
            }
            """.formatted(promptJsonString);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("us.amazon.nova-2-lite-v1:0")
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);

            String raw = response.body().asUtf8String();

            JsonNode root = mapper.readTree(raw);

            String text = root
                    .path("output")
                    .path("message")
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

            text = text.replace("```json", "")
                       .replace("```", "")
                       .trim();

            ObjectMapper mapper2 = new ObjectMapper();
            SearchFiltersDto filters = mapper2.readValue(text, SearchFiltersDto.class);
            return filters;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error calling Nova", e);
        }
    }


    //analyze the dog park reviews and return the scores
    @Cacheable(
    value = "dogpark-analysis",
    key = "#placeId"
    )
    public DogParkAnalysisDto analyzeDogParkReviews(String placeId, List<String> reviews) {
        System.out.println("Calling Nova API..." + placeId);
        try {
    
            ObjectMapper mapper = new ObjectMapper();
    
            String reviewsText = String.join("\n", reviews);
    
            String prompt = """
                    You are analyzing dog park reviews.
    
                    Based on the reviews, give scores from 1 to 10 for:
    
                    parkingScore
                    crowdedScore
                    cleanlinessScore
                    dogFriendlinessScore
                    parkSizeScore
    
                    Return ONLY valid JSON.
    
                    Reviews:
                    %s
                    """.formatted(reviewsText);
    
            String promptJsonString = mapper.writeValueAsString(prompt);
    
            String requestBody = """
            {
              "messages": [
                {
                  "role": "user",
                  "content": [
                    {
                      "text": %s
                    }
                  ]
                }
              ],
              "inferenceConfig": {
                "maxTokens": 300,
                "temperature": 0.1
              }
            }
            """.formatted(promptJsonString);
    
    
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("us.amazon.nova-2-lite-v1:0")
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
                    .build();
    
    
            InvokeModelResponse response =
                    bedrockRuntimeClient.invokeModel(request);
    
    
            String raw = response.body().asUtf8String();
    
    
            JsonNode root = mapper.readTree(raw);
    
    
            String text = root
                    .path("output")
                    .path("message")
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();
    
    
            text = text.replace("```json", "")
                    .replace("```", "")
                    .trim();
    
    
            DogParkAnalysisDto dto =
                    mapper.readValue(text, DogParkAnalysisDto.class);
    
    
            return dto;
    
        }
        catch (Exception e) {
    
            throw new RuntimeException("Nova dog park analysis failed", e);
        }
    }
}
