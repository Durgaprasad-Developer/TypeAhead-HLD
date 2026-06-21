import { useState, useEffect } from 'react';
import { fetchSuggestions } from '../api/suggestionApi';
import useDebounce from './useDebounce';

/**
 * Custom hook to manage fetching suggestions for a query string.
 * Uses useDebounce to debounce queries, and manages loading/error/data states.
 *
 * @param {string} initialQuery The initial input query value.
 * @returns {object} An object containing:
 *   - query: current query string
 *   - setQuery: function to update the query string
 *   - suggestions: list of suggestions from backend
 *   - loading: boolean state of fetch operation
 *   - error: string error message if any
 */
export default function useSuggestions(initialQuery = '') {
  const [query, setQuery] = useState(initialQuery);
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const debouncedQuery = useDebounce(query, 300);

  useEffect(() => {
    // If the query is empty or just whitespace, clear suggestions and don't make API calls
    if (!debouncedQuery.trim()) {
      setSuggestions([]);
      setLoading(false);
      setError(null);
      return;
    }

    let isMounted = true;
    const loadSuggestions = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchSuggestions(debouncedQuery);
        if (isMounted) {
          setSuggestions(data || []);
        }
      } catch (err) {
        if (isMounted) {
          logError(err);
          setError('Unable to load suggestions');
          setSuggestions([]);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    loadSuggestions();

    return () => {
      isMounted = false;
    };
  }, [debouncedQuery]);

  // Private helper to log errors safely
  function logError(err) {
    console.error('Error fetching suggestions:', err);
  }

  return {
    query,
    setQuery,
    suggestions,
    loading,
    error,
  };
}
