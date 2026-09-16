import React, { useState } from 'react';
import CreateOrderForm from './components/CreateOrderForm.jsx';
import OrderSearch from './components/OrderSearch.jsx';
import OrderStats from './components/OrderStats.jsx';

export default function App() {
  // When a new order is created, we add its ID to a Set so that the stats
  // panel can poll its tracking state and keep the per-status distribution
  // up to date.
  const [knownOrders, setKnownOrders] = useState(() => new Set());
  const [selected, setSelected] = useState(null);

  function handleCreated(order) {
    setKnownOrders(prev => new Set(prev).add(order.orderId));
    setSelected(order.orderId);
  }

  return (
    <div className="app">
      <header className="header">
        <div>
          <h1>Real-Time Order &amp; Tracking Platform</h1>
          <div className="sub">
            Order Service → Kafka → Tracking Service → MySQL + Redis → React
          </div>
        </div>
      </header>

      <OrderStats knownOrders={knownOrders} />

      <div className="grid" style={{ marginTop: 20 }}>
        <CreateOrderForm onCreated={handleCreated} />
        <OrderSearch
          initialOrderId={selected}
          autoLoad={!!selected}
        />
      </div>
    </div>
  );
}
