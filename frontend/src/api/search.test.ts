import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { searchListings } from './search';
import type { SearchResponse } from '../types';

describe('searchListings', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('POSTs to /api/search with query and returns parsed response', async () => {
    const response: SearchResponse = {
      listings: [
        {
          id: '1',
          address: '123 Main St',
          price: 500_000,
          bedrooms: 3,
          bathrooms: 2,
          latitude: 37.77,
          longitude: -122.42,
          nearestDogParkName: 'Puppy Park',
          nearestDogParkRating: 4.8,
          distanceToDogPark: 0.5,
          imageUrl: null,
        },
      ],
      dogParks: [
        {
          name: 'Puppy Park',
          address: '456 Oak Ave',
          latitude: 37.78,
          longitude: -122.43,
          rating: 4.8,
          userRatingCount: 100,
        },
      ],
    };

    (fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(response),
    });

    const result = await searchListings('Seattle dog parks');

    expect(fetch).toHaveBeenCalledWith('/api/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: 'Seattle dog parks' }),
    });
    expect(result).toEqual(response);
    expect(result.listings).toHaveLength(1);
    expect(result.dogParks).toHaveLength(1);
  });

  it('throws with response text when response is not ok', async () => {
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: false,
      status: 500,
      text: () => Promise.resolve('Internal Server Error'),
    });

    await expect(searchListings('bad query')).rejects.toThrow('Internal Server Error');
  });

  it('throws generic message when response text is empty', async () => {
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: false,
      status: 502,
      text: () => Promise.resolve(''),
    });

    await expect(searchListings('x')).rejects.toThrow('Search failed: 502');
  });
});
