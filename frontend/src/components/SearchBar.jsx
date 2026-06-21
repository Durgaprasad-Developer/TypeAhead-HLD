import React, { useState, useRef, useEffect } from 'react';
import useSuggestions from '../hooks/useSuggestions';
import SuggestionDropdown from './SuggestionDropdown';
import SearchButton from './SearchButton';

/**
 * A search input component with built-in autocomplete suggestion dropdown.
 * Handles keyboard navigation (Arrow Up/Down, Enter, Escape) and click outside.
 *
 * @param {object} props
 * @param {function} props.onSearch Callback triggered when search is submitted.
 * @param {string} props.placeholder Optional placeholder text.
 */
export default function SearchBar({ onSearch, placeholder = 'Search or type a query...' }) {
  const { query, setQuery, suggestions, loading, error } = useSuggestions();
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const containerRef = useRef(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Reset active index when suggestions change
  useEffect(() => {
    setActiveIndex(-1);
  }, [suggestions]);

  const handleKeyDown = (e) => {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        setIsOpen(true);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setActiveIndex((prev) => 
          prev < suggestions.length - 1 ? prev + 1 : 0
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setActiveIndex((prev) => 
          prev > 0 ? prev - 1 : suggestions.length - 1
        );
        break;
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        setActiveIndex(-1);
        break;
      case 'Enter':
        e.preventDefault();
        if (activeIndex >= 0 && activeIndex < suggestions.length) {
          const selected = suggestions[activeIndex];
          setQuery(selected);
          setIsOpen(false);
          onSearch(selected);
        } else if (query.trim()) {
          setIsOpen(false);
          onSearch(query);
        }
        break;
      default:
        break;
    }
  };

  const handleSelectSuggestion = (suggestion) => {
    setQuery(suggestion);
    setIsOpen(false);
    onSearch(suggestion);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (query.trim()) {
      setIsOpen(false);
      onSearch(query);
    }
  };

  return (
    <div ref={containerRef} className="w-full max-w-2xl relative">
      <form onSubmit={handleSubmit} className="flex gap-2 w-full">
        <div className="relative flex-1">
          <input
            type="text"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setIsOpen(true);
            }}
            onFocus={() => setIsOpen(true)}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            className="w-full h-12 px-5 py-3 rounded-xl text-sm border outline-none transition-all duration-150 text-white placeholder-slate-500"
            style={{
              background: 'var(--color-surface)',
              borderColor: isOpen ? 'var(--color-accent)' : 'var(--color-border)',
              boxShadow: isOpen ? '0 0 0 2px rgba(108, 99, 255, 0.2)' : 'none',
            }}
            aria-autocomplete="list"
            aria-controls="suggestions-listbox"
            aria-expanded={isOpen}
          />
          {query && (
            <button
              type="button"
              onClick={() => {
                setQuery('');
                setIsOpen(false);
              }}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white transition-colors"
              aria-label="Clear input"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}
        </div>
        <SearchButton onClick={handleSubmit} disabled={!query.trim() || loading} />
      </form>

      {isOpen && (
        <SuggestionDropdown
          suggestions={suggestions}
          prefix={query}
          activeIndex={activeIndex}
          loading={loading}
          error={error}
          onSelect={handleSelectSuggestion}
        />
      )}
    </div>
  );
}
