import React from 'react';
import type { Listing } from '../types';

interface ListingCardProps {
  listing: Listing;
  onMouseEnter?: () => void;
  onMouseLeave?: () => void;
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(price);
}

const ListingCard: React.FC<ListingCardProps> = ({ listing, onMouseEnter, onMouseLeave }) => {
  return (
    <article
      className="listing-card"
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
    >
      <div className="listing-card-image">
        {listing.imageUrl ? (
          <img src={listing.imageUrl} alt={listing.address} />
        ) : (
          <div className="listing-card-image-fallback">No photo</div>
        )}
      </div>
      <div className="listing-card-body">
        <p className="listing-card-address">{listing.address}</p>
        <p className="listing-card-price">{formatPrice(listing.price)}</p>
        <p className="listing-card-details">
          {listing.bedrooms} bed · {listing.bathrooms} bath
        </p>
        <div className="listing-card-scores">
          {listing.nearestDogParkName && (
            <span className="listing-card-score" title="Distance to nearest dog park">
              {listing.distanceToDogPark.toFixed(1)} mi to dog park
            </span>
          )}
          {listing.nearestDogParkName && (
            <span className="listing-card-park" title="Nearest dog park rating and name">
              ★ {listing.nearestDogParkRating.toFixed(1)} · {listing.nearestDogParkName}
            </span>
          )}
          {listing.nearestDogParkAnalysis && (
            <div
              className="listing-card-score listing-card-analysis"
              title="Dog park analysis (parking / crowded / cleanliness / dog-friendliness / park size)"
            >
              <strong>AI generated: </strong>
              <span>Parking {listing.nearestDogParkAnalysis.parkingScore}/10</span>
              <span> · Crowded {listing.nearestDogParkAnalysis.crowdedScore}/10</span>
              <span> · Clean {listing.nearestDogParkAnalysis.cleanlinessScore}/10</span>
              <span> · Dog friendliness {listing.nearestDogParkAnalysis.dogFriendlinessScore}/10</span>
              <span> · Park size {listing.nearestDogParkAnalysis.parkSizeScore}/10</span>
            </div>
          )}
        </div>
      </div>
    </article>
  );
};

export default ListingCard;
