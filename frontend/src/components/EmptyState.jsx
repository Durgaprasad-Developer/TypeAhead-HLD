import React from 'react';

/**
 * Renders when a search query yields zero suggestions.
 */
export default function EmptyState() {
  return (
    <div className="px-4 py-6 text-center text-sm" style={{ color: 'var(--color-muted)' }}>
      <p className="font-medium text-white mb-1">No suggestions found</p>
      <p className="text-xs">Try searching for something else or submit this query.</p>
    </div>
  );
}
