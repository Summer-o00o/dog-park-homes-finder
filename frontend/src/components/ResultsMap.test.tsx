import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import ResultsMap from './ResultsMap';
import type { Listing, DogPark } from '../types';

const mockListing: Listing = {
  id: '1',
  address: '123 Main St',
  price: 500_000,
  bedrooms: 2,
  bathrooms: 1,
  latitude: 47.6,
  longitude: -122.3,
  nearestDogParkName: 'Park',
  nearestDogParkRating: 4.8,
  distanceToDogPark: 0.5,
  imageUrl: null,
};

const mockDogPark: DogPark = {
  name: 'Puppy Park',
  address: '456 Oak Ave',
  latitude: 47.61,
  longitude: -122.31,
  rating: 4.8,
  userRatingCount: 100,
};

const mockUseJsApiLoader = vi.fn();
vi.mock('@react-google-maps/api', () => ({
  useJsApiLoader: (opts: unknown) => mockUseJsApiLoader(opts),
  GoogleMap: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="google-map">{children}</div>
  ),
  Marker: () => null,
}));

describe('ResultsMap', () => {
  beforeEach(() => {
    mockUseJsApiLoader.mockReset();
  });

  it('shows loading placeholder when script is not yet loaded', () => {
    mockUseJsApiLoader.mockReturnValue({ isLoaded: false, loadError: null });
    render(
      <ResultsMap listings={[mockListing]} dogParks={[mockDogPark]} />
    );
    expect(screen.getByText('Loading map…')).toBeInTheDocument();
  });

  it('shows error message when loader fails', () => {
    mockUseJsApiLoader.mockReturnValue({
      isLoaded: false,
      loadError: new Error('Network error'),
    });
    render(
      <ResultsMap listings={[mockListing]} dogParks={[mockDogPark]} />
    );
    expect(screen.getByText('Map failed to load: Network error')).toBeInTheDocument();
  });

  it('renders map when loaded', () => {
    mockUseJsApiLoader.mockReturnValue({ isLoaded: true, loadError: null });
    render(
      <ResultsMap listings={[mockListing]} dogParks={[mockDogPark]} />
    );
    expect(screen.getByTestId('google-map')).toBeInTheDocument();
  });
});
