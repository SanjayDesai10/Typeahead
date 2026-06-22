import { useState, useEffect } from 'react';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';

export default function MetricsDashboard({ forceOpen }) {
  const [isOpen, setIsOpen] = useState(false);
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (forceOpen) {
      setIsOpen(true);
    }
  }, [forceOpen]);

  useEffect(() => {
    if (isOpen) {
      fetchMetrics();
      const interval = setInterval(fetchMetrics, 5000);
      return () => clearInterval(interval);
    }
  }, [isOpen]);

  const fetchMetrics = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/metrics`);
      const data = await res.json();
      setMetrics(data);
    } catch (err) {
      console.error('Metrics fetch failed:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="panel" id="metrics-dashboard">
      <div
        className={`panel-header ${isOpen ? 'open' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className="panel-header-left">
          <span className="section-icon">📊</span>
          <h3>System Metrics</h3>
        </div>
        <span className={`panel-chevron ${isOpen ? 'open' : ''}`}>▼</span>
      </div>

      {isOpen && (
        <div className="panel-body">
          {loading && !metrics ? (
            <div style={{ textAlign: 'center', padding: 12, color: 'var(--text-muted)' }}>
              <span className="spinner" /> Loading...
            </div>
          ) : !metrics ? (
            <div style={{ textAlign: 'center', padding: 12, color: 'var(--text-muted)' }}>
              No metrics available yet
            </div>
          ) : (
            <>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 8, fontWeight: 600 }}>
                Batch Writer
              </div>
              <div className="metrics-grid" style={{ marginBottom: 16 }}>
                <div className="metric-card">
                  <div className="metric-value">{metrics.totalSearchesReceived}</div>
                  <div className="metric-label">Searches Received</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{metrics.totalDbWrites}</div>
                  <div className="metric-label">DB Write Batches</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{metrics.writeReductionRatio.toFixed(1)}x</div>
                  <div className="metric-label">Write Reduction</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{metrics.flushCount}</div>
                  <div className="metric-label">Flush Count</div>
                </div>
              </div>

              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 8, fontWeight: 600 }}>
                Cache Performance
              </div>
              <div className="metrics-grid" style={{ marginBottom: 16 }}>
                <div className="metric-card">
                  <div className="metric-value">{metrics.totalCacheHits}</div>
                  <div className="metric-label">Cache Hits</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{metrics.totalCacheMisses}</div>
                  <div className="metric-label">Cache Misses</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{(metrics.overallHitRate * 100).toFixed(1)}%</div>
                  <div className="metric-label">Hit Rate</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{metrics.totalQueries.toLocaleString()}</div>
                  <div className="metric-label">Total Queries in DB</div>
                </div>
              </div>

              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 8, fontWeight: 600 }}>
                Latency (ms)
              </div>
              <div className="metrics-grid">
                <div className="metric-card">
                  <div className="metric-value">{metrics.avgLatencyMs.toFixed(2)}</div>
                  <div className="metric-label">Average</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{metrics.p95LatencyMs.toFixed(2)}</div>
                  <div className="metric-label">P95</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{metrics.p99LatencyMs.toFixed(2)}</div>
                  <div className="metric-label">P99</div>
                </div>
                <div className="metric-card">
                  <div className="metric-value">{metrics.totalDbReads}</div>
                  <div className="metric-label">DB Reads</div>
                </div>
              </div>

              {metrics.lastFlushAt && metrics.lastFlushAt !== 'never' && (
                <div style={{ marginTop: 12, fontSize: '0.7rem', color: 'var(--text-muted)', textAlign: 'center' }}>
                  Last flush: {metrics.lastFlushAt} · Buffer: {metrics.currentBufferSize} queries
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}
