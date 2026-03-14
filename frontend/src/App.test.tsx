import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import * as searchApi from './api/search';
import type { SearchResponse } from './types';

vi.mock('./api/search');

describe('App', () => {
  beforeEach(() => {
    vi.mocked(searchApi.searchListings).mockReset();
  });

  it('renders header and search form', () => {
    render(<App />);
    expect(screen.getByRole('heading', { name: /Dog Park Homes Finder/i })).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Where are you looking/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument();
  });

  it('shows results on successful search', async () => {
    const user = userEvent.setup();
    const response: SearchResponse = {
      listings: [
        {
          id: '1',
          address: '456 Oak St',
          price: 600_000,
          bedrooms: 2,
          bathrooms: 1,
          latitude: 47.6,
          longitude: -122.3,
          nearestDogParkName: 'Bark Park',
          nearestDogParkRating: 4.9,
          distanceToDogPark: 0.3,
          imageUrl: null,
        },
      ],
      dogParks: [
        {
          name: 'Bark Park',
          address: '789 Pine Rd',
          latitude: 47.61,
          longitude: -122.31,
          rating: 4.9,
          userRatingCount: 50,
        },
      ],
    };
    vi.mocked(searchApi.searchListings).mockResolvedValueOnce(response);

    render(<App />);
    await user.type(screen.getByRole('textbox', { name: 'Search prompt' }), 'Seattle');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    await waitFor(() => {
      expect(searchApi.searchListings).toHaveBeenCalledWith('Seattle');
    });
    await waitFor(() => {
      expect(screen.getByText('1 home found')).toBeInTheDocument();
    });
    expect(screen.getByText('456 Oak St')).toBeInTheDocument();
    expect(screen.getByText('$600,000')).toBeInTheDocument();
  });

  it('shows error message when search fails', async () => {
    const user = userEvent.setup();
    vi.mocked(searchApi.searchListings).mockRejectedValueOnce(new Error('Network error'));

    render(<App />);
    await user.type(screen.getByRole('textbox', { name: 'Search prompt' }), 'Seattle');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('trims prompt when submitting', async () => {
    const user = userEvent.setup();
    vi.mocked(searchApi.searchListings).mockResolvedValueOnce({ listings: [], dogParks: [] });

    render(<App />);
    await user.type(screen.getByRole('textbox', { name: 'Search prompt' }), '  Portland  ');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    await waitFor(() => {
      expect(searchApi.searchListings).toHaveBeenCalledWith('Portland');
    });
  });
});
