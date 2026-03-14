import React, { useMemo, useCallback } from 'react';
import { useJsApiLoader, GoogleMap, Marker } from '@react-google-maps/api';
import type { Listing, DogPark } from '../types';

const MAP_CONTAINER_STYLE: React.CSSProperties = { width: '100%', height: '100%', borderRadius: '0.75rem', minHeight: 300 };
const DEFAULT_CENTER = { lat: 37.77, lng: -122.42 };
const DEFAULT_ZOOM = 11;

function getDogParkIconUrl(): string {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36">
      <circle cx="18" cy="18" r="16" fill="#22c55e" stroke="#15803d" stroke-width="2.5"/>
      <text x="18" y="23" text-anchor="middle" font-size="18" font-family="Arial, sans-serif">🐶</text>
    </svg>
  `.trim();
  return 'data:image/svg+xml,' + encodeURIComponent(svg);
}

function getHouseIconUrl(backgroundColor: string, strokeColor: string, strokeWidth = 2.5): string {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36">
      <circle cx="18" cy="18" r="16" fill="${backgroundColor}" stroke="${strokeColor}" stroke-width="${strokeWidth}"/>
      <text x="18" y="23" text-anchor="middle" font-size="18" font-family="Arial, sans-serif">🏠</text>
    </svg>
  `.trim();
  return 'data:image/svg+xml,' + encodeURIComponent(svg);
}

const HOUSE_ICON_DEFAULT: google.maps.Icon = {
  url: getHouseIconUrl('#ef4444', '#b91c1c'),
  scaledSize: { width: 36, height: 36 } as google.maps.Size,
  anchor: { x: 18, y: 18 } as google.maps.Point,
};

const HOUSE_ICON_HOVERED: google.maps.Icon = {
  url: getHouseIconUrl('#eab308', '#ca8a04'),
  scaledSize: { width: 36, height: 36 } as google.maps.Size,
  anchor: { x: 18, y: 18 } as google.maps.Point,
};

interface ResultsMapProps {
  listings: Listing[];
  dogParks: DogPark[];
  hoveredListingId?: string | null;
}

function getListingMarkerIcon(listing: Listing, hoveredListingId: string | null): google.maps.Icon {
  const isHovered = listing.id === hoveredListingId;
  return isHovered ? HOUSE_ICON_HOVERED : HOUSE_ICON_DEFAULT;
}

const ResultsMap: React.FC<ResultsMapProps> = ({ listings, dogParks, hoveredListingId = null }) => {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY ?? '';

  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey: apiKey,
  });

  const mapRef = React.useRef<google.maps.Map | null>(null);
  const onMapLoad = useCallback((map: google.maps.Map) => {
    mapRef.current = map;
    const bounds = new google.maps.LatLngBounds();
    listings.forEach((l) => bounds.extend({ lat: l.latitude, lng: l.longitude }));
    dogParks.forEach((p) => bounds.extend({ lat: p.latitude, lng: p.longitude }));
    if (!bounds.isEmpty()) {
      map.fitBounds(bounds, 48);
    }
  }, [listings, dogParks]);

  const center = useMemo(() => {
    if (listings.length > 0) {
      return { lat: listings[0].latitude, lng: listings[0].longitude };
    }
    if (dogParks.length > 0) {
      return { lat: dogParks[0].latitude, lng: dogParks[0].longitude };
    }
    return DEFAULT_CENTER;
  }, [listings, dogParks]);

  const dogParkIcon = useMemo(
    (): google.maps.Icon => ({
      url: getDogParkIconUrl(),
      scaledSize: { width: 36, height: 36 } as google.maps.Size,
      anchor: { x: 18, y: 18 } as google.maps.Point,
    }),
    []
  );

  if (!apiKey) {
    return (
      <div className="map-placeholder">
        Set VITE_GOOGLE_MAPS_API_KEY in .env to show the map
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="map-placeholder">
        Map failed to load: {loadError.message}
      </div>
    );
  }

  if (!isLoaded) {
    return <div className="map-placeholder">Loading map…</div>;
  }

  return (
    <GoogleMap
      mapContainerStyle={MAP_CONTAINER_STYLE}
      center={center}
      zoom={DEFAULT_ZOOM}
      onLoad={onMapLoad}
      options={{
        mapTypeControl: true,
        streetViewControl: false,
        fullscreenControl: true,
        zoomControl: true,
        zoomControlOptions: {
          position: typeof google !== 'undefined' ? google.maps.ControlPosition.RIGHT_TOP : undefined,
        },
      }}
    >
      {listings.map((listing) => (
        <Marker
          key={listing.id}
          position={{ lat: listing.latitude, lng: listing.longitude }}
          title={listing.address}
          icon={getListingMarkerIcon(listing, hoveredListingId)}
          zIndex={listing.id === hoveredListingId ? 3 : 1}
        />
      ))}
      {dogParks.map((park, i) => (
        <Marker
          key={park.name + i}
          position={{ lat: park.latitude, lng: park.longitude }}
          title={park.name}
          icon={dogParkIcon}
          zIndex={2}
        />
      ))}
    </GoogleMap>
  );
};

export default ResultsMap;
