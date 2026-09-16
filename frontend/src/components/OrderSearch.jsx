import React, { useState } from 'react';
import { TrackingAPI } from '../api/client';
import OrderDetail from './OrderDetail';

// Search box for a tracking snapshot. Parent passes the query so the same
// component can be used to display search history and result panels.
export default function OrderSearch({ initialOrderId, autoLoad, setStats, stats }) {
  const [orderId, setOrderId] = useState(initialOrderId || '');
  const [loading, setLoading] = useState(false);
  const [tracking, setTracking] = useState(null);
  const [error, setError] = useState(null);

  async function search(e) {
    e.preventDefault();
    if (!orderId.trim()) return;
    setLoading(true);
    setError(null);
    setTracking(null);
    try {
      const result = await TrackingAPI.get(orderId.trim());
      setTracking(result);
    } catch (err) {
      setError(err.message);
      // also remove from known orders so stats stay accurate
      if (stats && setStats) {
        const next = new Set(stats.knownOrders);
        next.delete(orderId.trim());
        setStats({ ...stats, knownOrders: next });
      }
    } finally {
      setLoading(false);
    }
  }

  async function refresh() {
    if (!tracking) return;
    setLoading(true);
    try {
      const result = await TrackingAPI.get(tracking.orderId);
      setTracking(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="panel">
      <h2>Search Order Tracking</h2>
      <form className="search-row" onSubmit={search}>
        <input
          type="text"
          placeholder="Enter order ID, e.g. ORD-ABCD1234"
          value={orderId}
          onChange={e => setOrderId(e.target.value)}
        />
        <button type="submit" className="primary" disabled={loading}>
          {loading ? 'Searching…' : 'Search'}
        </button>
        {tracking && (
          <button type="button" onClick={refresh} disabled={loading}>
            Refresh
          </button>
        )}
      </form>

      {error && <div className="alert error">{error}</div>}
      {tracking && (
        <OrderDetail tracking={tracking} onUpdated={refresh} />
      )}
      {!tracking && !error && (
        <div className="empty">No order loaded. Search for one above.</div>
      )}
    </div>
  );
}
