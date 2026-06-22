import { useState, useRef } from 'react';
import SearchBar from './components/SearchBar';
import SearchResponse from './components/SearchResponse';
import TrendingSearches from './components/TrendingSearches';
import CacheDebugPanel from './components/CacheDebugPanel';
import MetricsDashboard from './components/MetricsDashboard';
import './index.css';

function App() {
  const [searchResponse, setSearchResponse] = useState(null);
  const [lastPrefix, setLastPrefix] = useState('');
  const [showDevDashboard, setShowDevDashboard] = useState(false);
  const searchBarRef = useRef(null);

  const handleSearchResponse = (response) => {
    setSearchResponse(response);
    // Auto-hide after 5 seconds
    setTimeout(() => setSearchResponse(null), 5000);
  };

  const handleQueryChange = (prefix, data) => {
    setLastPrefix(prefix);
  };

  const handleTrendingClick = (query) => {
    const input = document.getElementById('search-input');
    if (input) {
      const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
        window.HTMLInputElement.prototype, 'value'
      ).set;
      nativeInputValueSetter.call(input, query);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.focus();
    }
  };

  return (
    <div className={`app ${showDevDashboard ? 'dashboard-visible' : 'minimal-mode'}`}>
      <div className="app-content">
        <main className="main-content">
          <div className="search-section-wrapper">
            <div className="logo-container">
              <h1 className="logo-title">TypeAhead</h1>
            </div>

            <div className="search-box-card">
              <SearchBar
                ref={searchBarRef}
                onSearchResponse={handleSearchResponse}
                onQueryChange={handleQueryChange}
              />
              <SearchResponse response={searchResponse} />
            </div>

            <TrendingSearches onTrendingClick={handleTrendingClick} />
          </div>

          {showDevDashboard && (
            <div className="panels-grid">
              <CacheDebugPanel lastPrefix={lastPrefix} forceOpen={showDevDashboard} />
              <MetricsDashboard forceOpen={showDevDashboard} />
            </div>
          )}

          <div className="dashboard-toggle-container">
            <button
              className={`dashboard-toggle-btn ${showDevDashboard ? 'active' : ''}`}
              onClick={() => setShowDevDashboard(!showDevDashboard)}
            >
              {showDevDashboard ? '📊 Hide Analytics & Caching' : '📊 Show Analytics & Caching'}
            </button>
          </div>
        </main>
      </div>
    </div>
  );
}

export default App;
