export default function SuggestionDropdown({ suggestions, loading, activeIndex, query, onSelect }) {
  if (loading && suggestions.length === 0) {
    return (
      <div className="suggestions-dropdown">
        <div className="suggestions-loading">
          <span className="spinner" /> Loading suggestions...
        </div>
      </div>
    );
  }

  if (suggestions.length === 0) {
    return (
      <div className="suggestions-dropdown">
        <div className="suggestions-empty">
          No suggestions found for "<strong>{query}</strong>"
        </div>
      </div>
    );
  }

  const formatCount = (count) => {
    if (count >= 1000000) return (count / 1000000).toFixed(1) + 'M';
    if (count >= 1000) return (count / 1000).toFixed(1) + 'K';
    return count.toString();
  };

  const highlightMatch = (text, prefix) => {
    const lowerText = text.toLowerCase();
    const lowerPrefix = prefix.toLowerCase();
    if (lowerText.startsWith(lowerPrefix)) {
      return (
        <>
          <span className="highlight">{text.substring(0, prefix.length)}</span>
          {text.substring(prefix.length)}
        </>
      );
    }
    return text;
  };

  return (
    <div className="suggestions-dropdown" id="suggestions-dropdown">
      {suggestions.map((item, index) => (
        <div
          key={item.query + index}
          className={`suggestion-item ${index === activeIndex ? 'active' : ''}`}
          onClick={() => onSelect(item)}
          onMouseEnter={() => {}}
          id={`suggestion-${index}`}
        >
          <span className="suggestion-icon">🔎</span>
          <span className="suggestion-text">
            {highlightMatch(item.query, query)}
          </span>
          <div className="suggestion-meta">
            <span className="suggestion-count">{formatCount(item.count)}</span>
            {item.trendingScore > 0 && (
              <span className="suggestion-score">⚡ {item.trendingScore.toFixed(0)}</span>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
