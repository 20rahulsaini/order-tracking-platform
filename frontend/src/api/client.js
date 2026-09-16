// Centralized REST API client. All calls go through this module so the
// rest of the app never sees fetch() boilerplate.

const BACKEND = {
  orders: '/api/orders',
  tracking: '/api/tracking',
  delivery: '/api/delivery',
};

async function handle(res) {
  if (!res.ok) {
    let detail = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      detail = body.message || body.error || detail;
    } catch (_) {
      // ignore parse failure
    }
    throw new Error(detail);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const OrdersAPI = {
  async create({ customerName, product, quantity, totalAmount }) {
    const res = await fetch(`${BACKEND.orders}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ customerName, product, quantity, totalAmount }),
    });
    return handle(res);
  },
  async get(orderId) {
    const res = await fetch(`${BACKEND.orders}/${encodeURIComponent(orderId)}`);
    return handle(res);
  },
};

export const TrackingAPI = {
  async get(orderId) {
    const res = await fetch(`${BACKEND.tracking}/orders/${encodeURIComponent(orderId)}`);
    return handle(res);
  },
};

export const DeliveryAPI = {
  async ship(orderId) {
    const res = await fetch(`${BACKEND.delivery}/${encodeURIComponent(orderId)}/ship`, { method: 'POST' });
    return handle(res);
  },
  async outForDelivery(orderId) {
    const res = await fetch(`${BACKEND.delivery}/${encodeURIComponent(orderId)}/out-for-delivery`, { method: 'POST' });
    return handle(res);
  },
  async deliver(orderId) {
    const res = await fetch(`${BACKEND.delivery}/${encodeURIComponent(orderId)}/deliver`, { method: 'POST' });
    return handle(res);
  },
};
