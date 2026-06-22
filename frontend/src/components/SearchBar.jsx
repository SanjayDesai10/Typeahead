import { useState, useRef, useEffect, useCallback } from 'react';
import { useDebounce } from '../hooks/useDebounce';
import SuggestionDropdown from './SuggestionDropdown';

const API_BASE = 'http://localhost:8080/api';

export default function SearchBar({ onSearchResponse, onQueryChange }) {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const inputRef = useRef(null);
  const wrapperRef = useRef(null);

  const debouncedQuery = useDebounce(query, 300);

  // Fetch suggestions when debounced query changes
  useEffect(() => {
    if (debouncedQuery.trim() === '') {
      setSuggestions([]);
      setShowDropdown(false);
      return;
    }

    const fetchSuggestions = async () => {
      setLoading(true);
      try {
        const res = await fetch(`${API_BASE}/suggest?q=${encodeURIComponent(debouncedQuery)}&trending=true`);
        const data = await res.json();
        setSuggestions(data.suggestions || []);
        setShowDropdown(true);
        setActiveIndex(-1);
        if (onQueryChange) onQueryChange(debouncedQuery, data);
      } catch (err) {
        console.error('Failed to fetch suggestions:', err);
        setSuggestions([]);
      } finally {
        setLoading(false);
      }
    };

    fetchSuggestions();
  }, [debouncedQuery]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClick = (e) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const submitSearch = useCallback(async (searchQuery) => {
    const q = searchQuery.trim();
    if (!q) return;

    setShowDropdown(false);
    try {
      const res = await fetch(`${API_BASE}/search`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: q }),
      });
      const data = await res.json();
      if (onSearchResponse) onSearchResponse(data);
    } catch (err) {
      console.error('Search failed:', err);
      if (onSearchResponse) onSearchResponse({ message: 'Error', error: err.message });
    }
  }, [onSearchResponse]);

  const handleKeyDown = (e) => {
    if (!showDropdown || suggestions.length === 0) {
      if (e.key === 'Enter') {
        submitSearch(query);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setActiveIndex(prev => Math.min(prev + 1, suggestions.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setActiveIndex(prev => Math.max(prev - 1, -1));
        break;
      case 'Enter':
        e.preventDefault();
        if (activeIndex >= 0 && activeIndex < suggestions.length) {
          const selected = suggestions[activeIndex].query;
          setQuery(selected);
          submitSearch(selected);
        } else {
          submitSearch(query);
        }
        break;
      case 'Escape':
        setShowDropdown(false);
        setActiveIndex(-1);
        break;
    }
  };

  const handleSuggestionClick = (suggestion) => {
    setQuery(suggestion.query);
    setShowDropdown(false);
    submitSearch(suggestion.query);
  };

  const fillQuery = (text) => {
    setQuery(text);
    inputRef.current?.focus();
  };

  return (
    <div className="search-bar-wrapper" ref={wrapperRef}>
      <div className="search-bar">
        <span className="search-icon">🔍</span>
        <input
          ref={inputRef}
          type="text"
          className="search-input"
          placeholder="Search for anything..."
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            if (e.target.value.trim() === '') setShowDropdown(false);
          }}
          onKeyDown={handleKeyDown}
          onFocus={() => {
            if (suggestions.length > 0 && query.trim()) setShowDropdown(true);
          }}
          id="search-input"
          autoComplete="off"
        />
        {loading && <span className="spinner" style={{ marginRight: 12 }} />}
        <button
          className="search-button"
          onClick={() => submitSearch(query)}
          id="search-button"
        >
          Search
        </button>
      </div>

      {showDropdown && (
        <SuggestionDropdown
          suggestions={suggestions}
          loading={loading}
          activeIndex={activeIndex}
          query={query}
          onSelect={handleSuggestionClick}
        />
      )}
    </div>
  );
}

// Expose fillQuery via ref (used by TrendingSearches)
SearchBar.fillQuery = null;
