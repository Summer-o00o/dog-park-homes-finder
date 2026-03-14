import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ListingCard from './ListingCard';
import type { Listing } from '../types';

const baseListing: Listing = {
  id: 'listing-1',
  address: '123 Main St, Seattle, WA',
  price: 750_000,
  bedrooms: 3,
  bathrooms: 2,
  latitude: 47.6,
  longitude: -122.3,
  nearestDogParkName: 'Puppy Park',
  nearestDogParkRating: 4.8,
  distanceToDogPark: 0.5,
  imageUrl: null,
};

describe('ListingCard', () => {
  it('renders address, price, beds/baths, and dog park info', () => {
    render(<ListingCard listing={baseListing} />);

    expect(screen.getByText('123 Main St, Seattle, WA')).toBeInTheDocument();
    expect(screen.getByText('$750,000')).toBeInTheDocument();
    expect(screen.getByText('3 bed · 2 bath')).toBeInTheDocument();
    expect(screen.getByText(/0\.5 mi to dog park/)).toBeInTheDocument();
    expect(screen.getByText(/★ 4\.8 · Puppy Park/)).toBeInTheDocument();
  });

  it('shows fallback when imageUrl is null', () => {
    render(<ListingCard listing={baseListing} />);
    expect(screen.getByText('No photo')).toBeInTheDocument();
  });

  it('shows image when imageUrl is set', () => {
    const withImage = { ...baseListing, imageUrl: '/images/123.jpg' };
    render(<ListingCard listing={withImage} />);
    const img = screen.getByRole('img', { name: baseListing.address });
    expect(img).toHaveAttribute('src', '/images/123.jpg');
    expect(screen.queryByText('No photo')).not.toBeInTheDocument();
  });

  it('calls onMouseEnter and onMouseLeave when hovering', () => {
    const onMouseEnter = vi.fn();
    const onMouseLeave = vi.fn();
    render(
      <ListingCard
        listing={baseListing}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
      />
    );
    const card = screen.getByRole('article');
    fireEvent.mouseEnter(card);
    expect(onMouseEnter).toHaveBeenCalledTimes(1);
    fireEvent.mouseLeave(card);
    expect(onMouseLeave).toHaveBeenCalledTimes(1);
  });

  it('renders AI analysis when nearestDogParkAnalysis is present', () => {
    const withAnalysis: Listing = {
      ...baseListing,
      nearestDogParkAnalysis: {
        parkingScore: 8,
        crowdedScore: 7,
        cleanlinessScore: 9,
        dogFriendlinessScore: 10,
        parkSizeScore: 6,
      },
    };
    render(<ListingCard listing={withAnalysis} />);
    expect(screen.getByText(/AI generated:/)).toBeInTheDocument();
    expect(screen.getByText(/Parking 8\/10/)).toBeInTheDocument();
    expect(screen.getByText(/Crowded 7\/10/)).toBeInTheDocument();
    expect(screen.getByText(/Clean 9\/10/)).toBeInTheDocument();
    expect(screen.getByText(/Dog friendliness 10\/10/)).toBeInTheDocument();
    expect(screen.getByText(/Park size 6\/10/)).toBeInTheDocument();
  });
});
