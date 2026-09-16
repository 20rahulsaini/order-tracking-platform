import React, { useEffect, useState } from 'react';
import { TrackingAPI } from '../api/client';

// Lightweight stats panel: takes the set of recently-seen order IDs and
// polls the tracking endpoint for each one to compute the per-status
// distribution. This is intentionally simple (no server-side aggregation)
// to keep the backend small.
export default function OrderStats({ knownOrders }) {
  const [stats, setStats] = useState({
    placed: 0,
    confirmed: 0,
    packed: 0,
    shipped: 0,
    out_for_delivery: 0,
    delivered: 0,
    unknown: 0,
    total: 0,
  });

  useEffect(() => {
    let cancelled = false;

    async function refresh() {
      const next = {
        placed: 0, confirmed: 0, packed: 0,
        shipped: 0, out_for_delivery: 0, delivered: 0,
        unknown: 0, total: 0,
      };
      await Promise.all([...knownOrders].map(async (orderId) => {
        try {
          const t = await TrackingAPI.get(orderId);
          const s = (t.currentStatus || 'unknown').toLowerCase();
          next.total += 1;
          if (next[s] === undefined) next.unknown += 1;
          else next[s] += 1;
        } catch (_) {
          // ignore individual failures
        }
      }));
      if (!cancelled) setStats(next);
    }

    refresh();
    const id = setInterval(refresh, 3000);
    return () => { cancelled = true; clearInterval(id); };
  }, [knownOrders]);

  return (
    <div className="panel">
      <h2>Order Statistics</h2>
      <div className="stats">
        <Stat label="Total" value={stats.total} />
        <Stat label="Placed" value={stats.placed} tone="placed" />
        <Stat label="Confirmed" value={stats.confirmed} tone="confirmed" />
        <Stat label="Packed" value={stats.packed} tone="packed" />
        <Stat label="Shipped" value={stats.shipped} tone="shipped" />
        <Stat label="Out for Delivery" value={stats.out_for_delivery} tone="out_for_delivery" />
        <Stat label="Delivered" value={stats.delivered} tone="delivered" />
        <Stat label="Unknown" value={stats.unknown} />
        <Stat label="Pending" value={stats.placed + stats.confirmed} />
        <Stat label="In Transit" value={stats.packed + stats.shipped + stats.out_for_delivery} />
        <Stat label="Completed" value={stats.delivered} />
        <Stat label="Active Orders" value={stats.total - stats.delivered} />
      </div>
      <div className="legend">
        Auto-refreshes every 3 seconds. Stats are computed from orders
        created in this session (no server-side aggregation).
      </div>
    </div>
  );
}

function Stat({ label, value, tone }) {
  return (
    <div className={`stat ${tone || ''}`}>
      <div className="label">{label}</div>
      <div className="value">{value}</div>
    </div>
  );
}
