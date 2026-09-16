import React, { useState } from 'react';
import { OrdersAPI } from '../api/client';

// Simple form to create an order. On success, the parent gets the new
// orderId and (optionally) a fresh snapshot to display.
export default function CreateOrderForm({ onCreated }) {
  const [form, setForm] = useState({
    customerName: '',
    product: '',
    quantity: 1,
    totalAmount: 0,
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [created, setCreated] = useState(null);

  function update(field, value) {
    setForm(prev => ({ ...prev, [field]: value }));
  }

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setCreated(null);
    try {
      const order = await OrdersAPI.create({
        customerName: form.customerName.trim(),
        product: form.product.trim(),
        quantity: Number(form.quantity),
        totalAmount: Number(form.totalAmount),
      });
      setCreated(order);
      setForm({ customerName: '', product: '', quantity: 1, totalAmount: 0 });
      if (onCreated) onCreated(order);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="panel">
      <h2>Create Order</h2>
      {error && <div className="alert error">{error}</div>}
      {created && (
        <div className="alert success">
          Order created: <strong>{created.orderId}</strong> (status: {created.status})
        </div>
      )}
      <form onSubmit={submit}>
        <div className="form-row">
          <label>
            Customer name
            <input
              type="text"
              required
              value={form.customerName}
              onChange={e => update('customerName', e.target.value)}
              placeholder="e.g. Alice"
            />
          </label>
          <label>
            Product
            <input
              type="text"
              required
              value={form.product}
              onChange={e => update('product', e.target.value)}
              placeholder="e.g. Mechanical Keyboard"
            />
          </label>
        </div>
        <div className="form-row">
          <label>
            Quantity
            <input
              type="number"
              min="1"
              required
              value={form.quantity}
              onChange={e => update('quantity', e.target.value)}
            />
          </label>
          <label>
            Total amount
            <input
              type="number"
              min="0.01"
              step="0.01"
              required
              value={form.totalAmount}
              onChange={e => update('totalAmount', e.target.value)}
            />
          </label>
        </div>
        <div className="actions">
          <button type="submit" className="primary" disabled={busy}>
            {busy ? 'Creating…' : 'Create Order'}
          </button>
        </div>
      </form>
    </div>
  );
}
