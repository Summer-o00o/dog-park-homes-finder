import React, { useState } from 'react';
import { searchListings } from './api/search';
import type { Listing, DogPark } from './types';
import ListingCard from './components/ListingCard';
import ResultsMap from './components/ResultsMap';

const App: React.FC = () => {
  const [prompt, setPrompt] = useState('');
  const [listings, setListings] = useState<Listing[] | null>(null);
  const [dogParks, setDogParks] = useState<DogPark[] | null>(null);
  const [hoveredListingId, setHoveredListingId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setListings(null);
    setDogParks(null);
    setLoading(true);
    try {
      const result = await searchListings(prompt.trim());
      setListings(result.listings);
      setDogParks(result.dogParks ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>Dog Park Homes Finder</h1>
        <p>Find homes your dog will love.</p>
      </header>
      <main className="app-main">
        <section className="chat-card">
          <form className="chat-form" onSubmit={handleSubmit}>
            <textarea
              className="chat-input"
              placeholder="e.g. House in Bellevue, the dog park is within 1 mile"
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              onInput={(e) => {
                const target = e.currentTarget;
                target.style.height = 'auto';
                target.style.height = `${target.scrollHeight}px`;
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  e.currentTarget.form?.requestSubmit();
                }
              }}
              rows={1}
              aria-label="Search prompt"
            />
            <button type="submit" className="chat-submit">
              Search
            </button>
          </form>
        </section>
        {loading && <p className="results-status">Searching…</p>}
        {error && <p className="results-error">{error}</p>}
        {listings && listings.length > 0 && (
          <div className="results-layout">
            <p className="results-count">
              {listings.length} home{listings.length !== 1 ? 's' : ''} found
            </p>
            <div className="results-list-inner">
              {listings.map((listing) => (
                <ListingCard
                  key={listing.id}
                  listing={listing}
                  onMouseEnter={() => setHoveredListingId(listing.id)}
                  onMouseLeave={() => setHoveredListingId(null)}
                />
              ))}
            </div>
            <aside className="results-map">
              <ResultsMap
                listings={listings}
                dogParks={dogParks ?? []}
                hoveredListingId={hoveredListingId}
              />
            </aside>
          </div>
        )}
      </main>
    </div>
  );
};

export default App;

