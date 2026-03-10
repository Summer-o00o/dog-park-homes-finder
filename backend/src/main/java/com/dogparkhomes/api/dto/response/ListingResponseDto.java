package com.dogparkhomes.api.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class ListingResponseDto implements Serializable {

    private String id;

    private String address;

    private double price;

    private int bedrooms;

    private double bathrooms;

    private double latitude;

    private double longitude;

    private String nearestDogParkName;

    private double nearestDogParkRating;

    private double distanceToDogPark;
    
    private String imageUrl;

    /**
     * Optional analysis scores for the nearest dog park, as produced by Nova.
     */
    private DogParkAnalysisDto nearestDogParkAnalysis;
}
