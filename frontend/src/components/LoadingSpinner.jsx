import React from 'react';

/**
 * A sleek, modern loading spinner utilizing CSS animations.
 */
export default function LoadingSpinner() {
  return (
    <div className="flex justify-center items-center py-2" aria-label="Loading">
      <div className="w-5 h-5 border-2 border-t-transparent rounded-full animate-spin"
           style={{ borderColor: 'var(--color-accent) var(--color-accent) var(--color-accent) transparent' }} />
    </div>
  );
}
