import React, { useState } from 'react';
import { OrdersAPI, DeliveryAPI } from '../api/client';

const STATUS_FLOW = ['PLACED', 'CONFIRMED', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED'];

// Pretty rendering of an individual tracking snapshot:
//   - current status badge
//   - last updated timestamp
//   - full status timeline
//   - delivery-simulation action buttons
export default function OrderDetail({ tracking, onUpdated }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  const currentStatus = (tracking.currentStatus || '').toUpperCase();
  const timeline = tracking.timeline || [];
  const currentIndex = STATUS_FLOW.indexOf(currentStatus);

  // The delivery service buttons are only useful for orders that have
  // already passed the PACKED state, since the demo only simulates the
  // SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED transitions.
  const canShip = currentStatus === 'PACKED' || currentStatus === 'CONFIRMED';
  const canOutForDelivery = currentStatus === 'SHIPPED';
  const canDeliver = currentStatus === 'OUT_FOR_DELIVERY';

  async function callDelivery(fn, label) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const result = await fn(tracking.orderId);
      setMessage(`Emitted ${label} event.`);
      if (onUpdated) await onUpdated();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <div className="detail-grid">
        <div className="key">Order ID</div>
        <div className="val">{tracking.orderId}</div>
        <div className="key">Current status</div>
        <div className="val">
          <span className={`badge ${currentStatus.toLowerCase()}`}>{currentStatus || '—'}</span>
        </div>
        <div className="key">Last updated</div>
        <div className="val">{fmtTime(tracking.lastUpdated)}</div>
        <div className="key">Timeline events</div>
        <div className="val">{timeline.length}</div>
      </div>

      {error && <div className="alert error">{error}</div>}
      {message && <div className="alert info">{message}</div>}

      {timeline.length === 0 ? (
        <div className="empty">No timeline events recorded yet.</div>
      ) : (
        <ul className="timeline">
          {timeline.map(ev => (
            <li key={ev.eventId}>
              <div className="ev-status">
                {ev.status} <span className="ev-meta">— {ev.eventType}</span>
              </div>
              <div className="ev-meta">
                {fmtTime(ev.occurredAt)} · eventId {ev.eventId}
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* Delivery simulation actions.
          These publish real Kafka events via the Delivery Service; the
          Tracking Service then consumes them and the next /refresh shows
          the new state. */}
      <div className="delivery-actions">
        <button
          className="success"
          disabled={busy || !canShip}
          onClick={() => callDelivery(DeliveryAPI.ship, 'SHIPPED')}
          title={canShip ? '' : 'Order must be in PACKED state to ship'}
        >
          Mark Shipped
        </button>
        <button
          className="warn"
          disabled={busy || !canOutForDelivery}
          onClick={() => callDelivery(DeliveryAPI.outForDelivery, 'OUT_FOR_DELIVERY')}
          title={canOutForDelivery ? '' : 'Order must be SHIPPED first'}
        >
          Out for Delivery
        </button>
        <button
          disabled={busy || !canDeliver}
          onClick={() => callDelivery(DeliveryAPI.deliver, 'DELIVERED')}
          title={canDeliver ? '' : 'Order must be OUT_FOR_DELIVERY first'}
        >
          Mark Delivered
        </button>
      </div>
      <div className="legend">
        Delivery actions publish Kafka events through the Delivery Service.
        Click <strong>Refresh</strong> in ~1s to see the new state.
      </div>
    </div>
  );
}

function fmtTime(iso) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString();
  } catch (_) {
    return iso;
  }
}
