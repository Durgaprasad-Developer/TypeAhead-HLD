import React from 'react';

/**
 * Renders a single suggestion item in the dropdown.
 * Highlights the matched prefix in the suggestion text.
 *
 * @param {object} props
 * @param {string} props.suggestion The suggestion text.
 * @param {string} props.prefix The prefix query typed by the user.
 * @param {boolean} props.isActive Whether this item is currently selected (keyboard nav).
 * @param {function} props.onClick Click handler.
 */
export default function SuggestionItem({ suggestion, prefix, isActive, onClick }) {
  // Highlight prefix match if it matches at the beginning
  const getHighlightedText = () => {
    if (!prefix) return <span>{suggestion}</span>;
    const lowerSuggestion = suggestion.toLowerCase();
    const lowerPrefix = prefix.toLowerCase();
    
    if (lowerSuggestion.startsWith(lowerPrefix)) {
      const match = suggestion.slice(0, prefix.length);
      const rest = suggestion.slice(prefix.length);
      return (
        <>
          <span className="text-white font-medium">{match}</span>
          <span style={{ color: 'var(--color-muted)' }}>{rest}</span>
        </>
      );
    }
    return <span>{suggestion}</span>;
  };

  return (
    <li
      role="option"
      aria-selected={isActive}
      onClick={onClick}
      className="flex items-center gap-3 px-4 py-2.5 text-sm cursor-pointer select-none transition-colors duration-150 rounded-lg mx-1 my-0.5"
      style={{
        background: isActive ? 'rgba(108, 99, 255, 0.15)' : 'transparent',
      }}
    >
      <svg className="w-4 h-4 shrink-0" style={{ color: isActive ? 'var(--color-accent)' : 'var(--color-muted)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <div className="flex-1 truncate">
        {getHighlightedText()}
      </div>
      {isActive && (
        <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded" style={{ background: 'var(--color-border)', color: 'var(--color-muted)' }}>
          Enter
        </span>
      )}
    </li>
  );
}
