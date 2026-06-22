import { useState, useEffect } from 'react';

const API_BASE = 'http://localhost:8080/api';

export default function TrendingSearches({ onTrendingClick }) {
  const [trending, setTrending] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchTrending();
    const interval = setInterval(fetchTrending, 30000); // Refresh every 30s
    return () => clearInterval(interval);
  }, []);

  const fetchTrending = async () => {
    try {
      const res = await fetch(`${API_BASE}/suggest?q=&trending=true`);
      const data = await res.json();
      setTrending(data.suggestions || []);
    } catch (err) {
      console.error('Failed to fetch trending:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatCount = (count) => {
    if (count >= 1000000) return (count / 1000000).toFixed(1) + 'M';
    if (count >= 1000) return (count / 1000).toFixed(1) + 'K';
    return count.toString();
  };

  if (loading) {
    return (
      <div className="trending-section">
        <div className="section-header">
          <span className="section-icon">🔥</span>
          <h2>Trending Searches</h2>
        </div>
        <div style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)' }}>
          <span className="spinner" /> Loading...
        </div>
      </div>
    );
  }

  if (trending.length === 0) return null;

  return (
    <div className="trending-section" id="trending-searches">
      <div className="section-header">
        <span className="section-icon">🔥</span>
        <h2>Trending Searches</h2>
      </div>
      <div className="trending-chips">
        {trending.map((item, idx) => (
          <button
            key={item.query}
            className="trending-chip"
            onClick={() => onTrendingClick && onTrendingClick(item.query)}
            id={`trending-chip-${idx}`}
          >
            <span className="trending-chip-rank">#{idx + 1}</span>
            <span>{item.query}</span>
            <span className="trending-chip-count">{formatCount(item.count)}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
