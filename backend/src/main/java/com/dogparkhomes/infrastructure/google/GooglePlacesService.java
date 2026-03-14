package com.dogparkhomes.infrastructure.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.dogparkhomes.api.dto.response.DogParkDto;
import com.dogparkhomes.infrastructure.nova.NovaService;
import java.util.ArrayList;
import java.util.List;

@Service
public class GooglePlacesService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final NovaService novaService;

    @Value("${google.places.api-key}")
    private String apiKey;

    public GooglePlacesService(NovaService novaService) {
        this.novaService = novaService;
    }

    public List<DogParkDto> searchDogParks(String location) {

        String url = "https://places.googleapis.com/v1/places:searchText";

        String requestBody = """
                {
                  "textQuery": "dog park in %s"
                }
                """.formatted(location);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount");

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        try {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            List<DogParkDto> parks = new ArrayList<>();

            for (JsonNode place : root.path("places")) {
                if (parks.size() >= 2) {
                    break;
                }

                DogParkDto dto = new DogParkDto();

                dto.setRating(
                    place.path("rating").asDouble(0)
                );
                // filter rating >= 4.8
                if (dto.getRating() < 4.8) {
                    continue;
                }

                String placeId = place.path("id").asText();
                List<String> reviews = getReviews(placeId);

                dto.setName(
                    place.path("displayName").path("text").asText()
                );

                dto.setAddress(
                    place.path("formattedAddress").asText()
                );

                dto.setLatitude(
                    place.path("location").path("latitude").asDouble()
                );

                dto.setLongitude(
                    place.path("location").path("longitude").asDouble()
                );

                dto.setUserRatingCount(
                    place.path("userRatingCount").asInt(0)
                );
                dto.setReviews(reviews);
                dto.setAnalysis(
                    novaService.analyzeDogParkReviews(placeId, reviews)
                );

                parks.add(dto);
            }

            return parks.stream().toList();

        } catch (Exception e) {

            throw new RuntimeException("Failed to parse Google Places response", e);
        }
    }

    public List<String> getReviews(String placeId) {

        String url = "https://places.googleapis.com/v1/places/" + placeId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", apiKey);

        headers.set(
            "X-Goog-FieldMask",
            "reviews.text,reviews.rating"
        );

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

        try {

            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(response.getBody());

            List<String> reviews = new ArrayList<>();

            for (JsonNode review : root.path("reviews")) {

                String text =
                    review
                        .path("text")
                        .path("text")
                        .asText();

                reviews.add(text);
            }

            return reviews;

        } catch (Exception e) {

            throw new RuntimeException(
                "Failed to parse reviews",
                e
            );
        }
    }
}
