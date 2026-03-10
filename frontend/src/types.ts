export interface Listing {
  id: string;
  address: string;
  price: number;
  bedrooms: number;
  bathrooms: number;
  latitude: number;
  longitude: number;
  nearestDogParkName: string;
  nearestDogParkRating: number;
  distanceToDogPark: number;
  imageUrl: string | null;
  nearestDogParkAnalysis?: DogParkAnalysis;
}

export interface DogParkAnalysis {
  parkingScore: number;
  crowdedScore: number;
  cleanlinessScore: number;
  dogFriendlinessScore: number;
  parkSizeScore: number;
}

export interface DogPark {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  rating: number;
  userRatingCount: number;
  reviews?: string[];
  analysis?: DogParkAnalysis;
}

export interface SearchResponse {
  listings: Listing[];
  dogParks: DogPark[];
}
