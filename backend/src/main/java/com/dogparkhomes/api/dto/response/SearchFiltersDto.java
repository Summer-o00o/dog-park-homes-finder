package com.dogparkhomes.api.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchFiltersDto {

    private String location;

    private String property_type;

    @JsonAlias({"amenities", "nearby_amenities", "close_amenities"})
    private List<String> amenities;

    private String price_range;

    /**
     * Optional inferred radius in miles (from natural language query).
     */
    private Double radius_miles;

}
