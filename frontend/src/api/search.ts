import type { SearchResponse } from '../types';

export async function searchListings(query: string): Promise<SearchResponse> {
  const res = await fetch('/api/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  });

  if (!res.ok) {
    const text = await res.text();
    let message = `Search failed: ${res.status}`;
    try {
      const body = JSON.parse(text) as { message?: string };
      if (body?.message) message = body.message;
    } catch {
      if (text) message = text;
    }
    throw new Error(message);
  }

  return res.json();
}
