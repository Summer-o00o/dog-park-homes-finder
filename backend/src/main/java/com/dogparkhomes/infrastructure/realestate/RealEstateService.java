package com.dogparkhomes.infrastructure.realestate;

import com.dogparkhomes.api.dto.response.DogParkDto;
import com.dogparkhomes.api.dto.response.ListingResponseDto;
import com.dogparkhomes.infrastructure.google.StreetViewService;
import com.dogparkhomes.util.DistanceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class RealEstateService {

    @Value("${realestate.api.api-key:}")
    private String apiKey;

    private static final String LISTING_CACHE_NAME = "listing-cache";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StreetViewService streetViewService;
    private final CacheManager cacheManager;

    @Autowired
    public RealEstateService(RestTemplate restTemplate, StreetViewService streetViewService,
                             @Autowired(required = false) CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.streetViewService = streetViewService;
        this.cacheManager = cacheManager;
    }

    public List<ListingResponseDto> searchHouses(List<DogParkDto> dogParks, double radiusMiles, Double priceMin, Double priceMax) {
        List<ListingResponseDto> allListings = new ArrayList<>();
        for (DogParkDto dogPark : dogParks) {
            List<ListingResponseDto> listings = fetchListings(
                    dogPark.getLatitude(),
                    dogPark.getLongitude(),
                    radiusMiles,
                    priceMin,
                    priceMax
            );
            for (ListingResponseDto listing : listings) {
                double distanceMiles = DistanceUtil.haversineMiles(
                        listing.getLatitude(), listing.getLongitude(),
                        dogPark.getLatitude(), dogPark.getLongitude()
                );
                if (distanceMiles <= radiusMiles) {
                    listing.setNearestDogParkName(dogPark.getName());
                    listing.setNearestDogParkRating(dogPark.getRating());
                    listing.setDistanceToDogPark(distanceMiles);
                    listing.setNearestDogParkAnalysis(dogPark.getAnalysis());
                    allListings.add(listing);
                }
            }
        }
        return allListings;
    }

    public List<ListingResponseDto> fetchListings(double latitude, double longitude, double radiusMiles, Double priceMin, Double priceMax) {
        String cacheKey = buildListingCacheKey(latitude, longitude, radiusMiles, priceMin, priceMax);
        Cache cache = cacheManager != null ? cacheManager.getCache(LISTING_CACHE_NAME) : null;
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                List<ListingResponseDto> cached = (List<ListingResponseDto>) wrapper.get();
                if (cached != null) return cached;
            }
        }

        String url = buildListingsUrl(latitude, longitude, radiusMiles, priceMin, priceMax);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        String body = response.getBody();
        List<ListingResponseDto> listings = parseListings(body);
        System.out.println("call rentcast api success");
        if (cache != null) cache.put(cacheKey, listings);
        return listings;
    }

    private static String buildListingCacheKey(double latitude, double longitude, double radiusMiles, Double priceMin, Double priceMax) {
        return latitude + "," + longitude + "," + radiusMiles + "," + priceMin + "," + priceMax;
    }

    private String buildListingsUrl(double latitude, double longitude, double radiusMiles, Double priceMin, Double priceMax) {
        String base = String.format(
                "https://api.rentcast.io/v1/listings/sale?latitude=%f&longitude=%f&radius=%f",
                latitude,
                longitude,
                radiusMiles
        );
        if (priceMin != null || priceMax != null) {
            long min = priceMin != null ? priceMin.longValue() : 0L;
            long max = priceMax != null ? priceMax.longValue() : 999_999_999L;
            return base + "&price=" + min + ":" + max;
        }
        return base;
    }

    private List<ListingResponseDto> parseListings(String json) {
        List<ListingResponseDto> listings = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode node : root) {
                ListingResponseDto dto = new ListingResponseDto();
                dto.setId(node.path("id").asText());
                dto.setAddress(node.path("formattedAddress").asText());
                dto.setPrice(node.path("price").asDouble());
                dto.setBedrooms(node.path("bedrooms").asInt());
                dto.setBathrooms(node.path("bathrooms").asDouble());
                dto.setLatitude(node.path("latitude").asDouble());
                dto.setLongitude(node.path("longitude").asDouble());

                String localImageUrl = streetViewService.getStreetViewImage(dto.getId(), dto.getLatitude(), dto.getLongitude());
                dto.setImageUrl(localImageUrl);
                listings.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RentCast response", e);
        }
        return listings;
    }
}
