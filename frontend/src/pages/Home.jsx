import React, { useState } from 'react';
import SearchBar from '../components/SearchBar';
import { submitSearch } from '../api/searchApi';

/**
 * Home page — Search page (/).
 * Renders the main search landing interface with the SearchBar.
 * Displays submission feedback wired to POST /search.
 */
export default function Home() {
  const [lastSearch, setLastSearch] = useState(null);

  const handleSearch = async (query) => {
    console.log('Search submitted:', query);
    try {
      await submitSearch(query);
      setLastSearch(query);
      // Automatically clear the search notification after 4 seconds
      setTimeout(() => {
        setLastSearch((prev) => (prev === query ? null : prev));
      }, 4000);
    } catch (err) {
      console.error('Failed to submit search:', err);
    }
  };

  return (
    <div className="relative min-h-[80vh] flex flex-col items-center justify-center px-4 overflow-hidden">
      {/* Dynamic background light flares */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] rounded-full blur-[120px] opacity-10 pointer-events-none"
           style={{ background: 'var(--color-accent)' }} />

      <div className="w-full max-w-4xl flex flex-col items-center text-center gap-8 relative z-10">
        {/* Brand header with gradient accent */}
        <div className="flex flex-col gap-3">
          <div className="inline-flex items-center justify-center self-center px-3 py-1 rounded-full text-xs font-semibold tracking-wider uppercase border"
               style={{ background: 'rgba(108, 99, 255, 0.08)', borderColor: 'rgba(108, 99, 255, 0.2)', color: 'var(--color-accent)' }}>
            HLD Assignment System
          </div>
          <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight text-white">
            Distributed <span className="bg-gradient-to-r from-indigo-400 to-purple-500 bg-clip-text text-transparent">TypeAhead</span>
          </h1>
          <p className="text-base md:text-lg max-w-lg mx-auto" style={{ color: 'var(--color-muted)' }}>
            A high-performance search autocompletion engine backed by a custom consistent hash ring and batching pipeline.
          </p>
        </div>

        {/* Core Search Bar component */}
        <div className="w-full flex justify-center mt-2">
          <SearchBar onSearch={handleSearch} placeholder="Search anything (e.g. google, yahoo, news)..." />
        </div>

        {/* Search Submission Notification (Milestone 7 Placeholder) */}
        {lastSearch && (
          <div
            className="mt-6 px-5 py-3.5 rounded-xl border flex items-center gap-3 animate-fade-in max-w-md w-full"
            style={{
              background: 'rgba(26, 29, 39, 0.85)',
              borderColor: 'var(--color-border)',
            }}
          >
            <div className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                 style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--color-success)' }}>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <div className="text-left flex-1 min-w-0">
              <p className="text-xs font-semibold uppercase tracking-wider" style={{ color: 'var(--color-muted)' }}>
                Query Submitted
              </p>
              <p className="text-sm font-medium text-white truncate">"{lastSearch}"</p>
            </div>
          </div>
        )}

        {/* Quick Help / Info Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 w-full mt-12 text-left">
          <div className="p-5 rounded-xl border" style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
            <h3 className="text-white font-semibold mb-2 flex items-center gap-2">
              <span className="text-indigo-400">⚡</span> O(L) Prefix Search
            </h3>
            <p className="text-xs leading-relaxed" style={{ color: 'var(--color-muted)' }}>
              Searches are fast independent of dataset size by traversing a custom in-memory character Trie.
            </p>
          </div>
          <div className="p-5 rounded-xl border" style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
            <h3 className="text-white font-semibold mb-2 flex items-center gap-2">
              <span className="text-indigo-400">🌀</span> Consistent Hashing
            </h3>
            <p className="text-xs leading-relaxed" style={{ color: 'var(--color-muted)' }}>
              Prefix results are sharded across virtual nodes in the hash ring for perfect cache balance.
            </p>
          </div>
          <div className="p-5 rounded-xl border" style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
            <h3 className="text-white font-semibold mb-2 flex items-center gap-2">
              <span className="text-indigo-400">📦</span> Batch Writes
            </h3>
            <p className="text-xs leading-relaxed" style={{ color: 'var(--color-muted)' }}>
              Search count updates are enqueued, aggregated, and written to PostgreSQL on a background scheduler.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
