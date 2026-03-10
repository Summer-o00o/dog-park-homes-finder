package com.dogparkhomes.infrastructure.realestate;

import com.dogparkhomes.api.dto.response.DogParkDto;
import com.dogparkhomes.api.dto.response.ListingResponseDto;
import com.dogparkhomes.util.DistanceUtil;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class RealEstateService {

    private final RentCastClient rentCastClient;

    public RealEstateService(RentCastClient rentCastClient) {
        this.rentCastClient = rentCastClient;
    }

    public List<ListingResponseDto> searchHouses(List<DogParkDto> dogParks, double radiusMiles) {
        List<ListingResponseDto> allListings = new ArrayList<>();
        for (DogParkDto dogPark : dogParks) {
            List<ListingResponseDto> listings = rentCastClient.fetchListings(
                    dogPark.getLatitude(),
                    dogPark.getLongitude(),
                    radiusMiles
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
}
