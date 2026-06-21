import React from 'react';

/**
 * Custom Search Button component with premium hover transitions.
 *
 * @param {object} props
 * @param {function} props.onClick
 * @param {boolean} props.disabled
 */
export default function SearchButton({ onClick, disabled }) {
  return (
    <button
      type="submit"
      onClick={onClick}
      disabled={disabled}
      className="px-6 h-12 rounded-xl font-medium cursor-pointer transition-all duration-150 flex items-center justify-center gap-2 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
      style={{
        background: 'var(--color-accent)',
        color: '#ffffff',
      }}
      onMouseOver={(e) => (e.currentTarget.style.background = 'var(--color-accent-h)')}
      onMouseOut={(e) => (e.currentTarget.style.background = 'var(--color-accent)')}
    >
      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <span>Search</span>
    </button>
  );
}
