import { useState, useEffect } from 'react';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';

export default function CacheDebugPanel({ lastPrefix, forceOpen }) {
  const [isOpen, setIsOpen] = useState(false);
  const [debugInfo, setDebugInfo] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (forceOpen) {
      setIsOpen(true);
    }
  }, [forceOpen]);

  useEffect(() => {
    if (isOpen && lastPrefix) {
      fetchDebug(lastPrefix);
    }
  }, [isOpen, lastPrefix]);

  const fetchDebug = async (prefix) => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/cache/debug?prefix=${encodeURIComponent(prefix)}`);
      const data = await res.json();
      setDebugInfo(data);
    } catch (err) {
      console.error('Cache debug failed:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="panel" id="cache-debug-panel">
      <div
        className={`panel-header ${isOpen ? 'open' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className="panel-header-left">
          <span className="section-icon">🗄️</span>
          <h3>Cache Debug</h3>
        </div>
        <span className={`panel-chevron ${isOpen ? 'open' : ''}`}>▼</span>
      </div>

      {isOpen && (
        <div className="panel-body">
          {loading ? (
            <div style={{ textAlign: 'center', padding: 12, color: 'var(--text-muted)' }}>
              <span className="spinner" /> Loading...
            </div>
          ) : !debugInfo ? (
            <div style={{ textAlign: 'center', padding: 12, color: 'var(--text-muted)' }}>
              Type a search query to see cache routing info
            </div>
          ) : (
            <>
              <div className="cache-debug-info">
                <div className="debug-item">
                  <div className="debug-item-label">Prefix</div>
                  <div className="debug-item-value">"{debugInfo.prefix}"</div>
                </div>
                <div className="debug-item">
                  <div className="debug-item-label">Assigned Node</div>
                  <div className="debug-item-value" style={{ color: 'var(--accent-primary)' }}>
                    {debugInfo.assignedNode}
                  </div>
                </div>
                <div className="debug-item">
                  <div className="debug-item-label">Hash Value</div>
                  <div className="debug-item-value" style={{ fontSize: '0.7rem' }}>
                    {debugInfo.hashValue}
                  </div>
                </div>
                <div className="debug-item">
                  <div className="debug-item-label">Cache Status</div>
                  <div className={`debug-item-value ${debugInfo.cacheHit ? 'hit' : 'miss'}`}>
                    {debugInfo.cacheHit ? '✅ HIT' : '❌ MISS'}
                    {debugInfo.cacheHit && ` (${debugInfo.cachedEntries} entries)`}
                  </div>
                </div>
              </div>

              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 8, fontWeight: 600 }}>
                Node Statistics
              </div>
              <div className="node-stats-list">
                {debugInfo.allNodes?.map(node => (
                  <div key={node.id} className="node-stat">
                    <span className="node-stat-id">{node.id}</span>
                    <div className="node-stat-values">
                      <span>Hits: {node.hits}</span>
                      <span>Misses: {node.misses}</span>
                      <span>Rate: {(node.hitRate * 100).toFixed(1)}%</span>
                      <span>Keys: {node.keyCount}</span>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
