import React from 'react';
import SuggestionItem from './SuggestionItem';
import LoadingSpinner from './LoadingSpinner';
import EmptyState from './EmptyState';
import ErrorMessage from './ErrorMessage';

/**
 * Dropdown list containing the autocompleted search suggestions.
 * Renders loading, error, empty, and populated list states.
 *
 * @param {object} props
 * @param {string[]} props.suggestions List of suggestions.
 * @param {string} props.prefix Current input query.
 * @param {number} props.activeIndex Currently focused suggestion index.
 * @param {boolean} props.loading Loading state.
 * @param {string|null} props.error Error message.
 * @param {function} props.onSelect Handler when a suggestion is clicked.
 */
export default function SuggestionDropdown({
  suggestions,
  prefix,
  activeIndex,
  loading,
  error,
  onSelect,
}) {
  const hasSuggestions = suggestions && suggestions.length > 0;

  // Do not render anything if the query is empty and we aren't loading or in error state
  if (!prefix && !loading && !error) {
    return null;
  }

  return (
    <div
      className="absolute left-0 right-0 mt-2 z-50 rounded-xl shadow-2xl overflow-hidden border backdrop-blur-md"
      style={{
        background: 'rgba(26, 29, 39, 0.95)',
        borderColor: 'var(--color-border)',
      }}
    >
      {loading && (
        <div className="py-6 flex justify-center items-center">
          <LoadingSpinner />
        </div>
      )}

      {!loading && error && (
        <div className="p-3">
          <ErrorMessage message={error} />
        </div>
      )}

      {!loading && !error && !hasSuggestions && prefix.trim().length > 0 && (
        <EmptyState />
      )}

      {!loading && !error && hasSuggestions && (
        <ul
          role="listbox"
          aria-label="Suggestions"
          className="max-h-80 overflow-y-auto py-1"
        >
          {suggestions.map((suggestion, index) => (
            <SuggestionItem
              key={suggestion}
              suggestion={suggestion}
              prefix={prefix}
              isActive={index === activeIndex}
              onClick={() => onSelect(suggestion)}
            />
          ))}
        </ul>
      )}
    </div>
  );
}
