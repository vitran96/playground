<script setup>
import { ref, onMounted } from 'vue'

const ALL_EVENTS = ['ADD_ITEM', 'CHECKOUT', 'PAY', 'SHIP', 'DELIVER', 'CANCEL']

const orders = ref([])
const showAllButtons = ref(false)
const errorMessage = ref('')
let errorTimeout = null

function showError(msg) {
  errorMessage.value = msg
  if (errorTimeout) clearTimeout(errorTimeout)
  errorTimeout = setTimeout(() => {
    errorMessage.value = ''
  }, 3000)
}

async function loadOrders() {
  try {
    const res = await fetch('/api/orders')
    if (res.ok) {
      orders.value = await res.json()
    }
  } catch (err) {
    showError(err.message)
  }
}

async function createCart() {
  try {
    const res = await fetch('/api/orders', { method: 'POST' })
    if (res.ok) {
      await loadOrders()
    } else {
      const body = await res.json()
      showError(body.error || 'Failed to create cart')
    }
  } catch (err) {
    showError(err.message)
  }
}

async function sendEvent(orderId, event) {
  try {
    const res = await fetch(`/api/orders/${orderId}/events/${event}`, { method: 'POST' })
    const data = await res.json()
    if (res.ok) {
      const idx = orders.value.findIndex(o => o.id === orderId)
      if (idx !== -1) {
        orders.value[idx] = data
      }
    } else if (res.status === 409) {
      showError(data.error || 'Conflict')
    } else {
      showError(data.error || 'Error')
    }
  } catch (err) {
    showError(err.message)
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<template>
  <main class="container">
    <h1>Cart State Machine Spike</h1>

    <div class="toolbar">
      <button class="primary-btn" @click="createCart">New cart</button>
      <label class="toggle-label">
        <input type="checkbox" v-model="showAllButtons">
        Show all buttons (exercise 409 rejections)
      </label>
    </div>

    <div v-if="errorMessage" class="error-banner">
      {{ errorMessage }}
    </div>

    <table class="order-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>State</th>
          <th>Item Count</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="orders.length === 0">
          <td colspan="4" class="empty-state">No orders yet. Click "New cart" to create one.</td>
        </tr>
        <tr v-for="order in orders" :key="order.id">
          <td>#{{ order.id }}</td>
          <td>
            <span :class="['badge', `badge-${order.state.toLowerCase()}`]">
              {{ order.state }}
            </span>
          </td>
          <td>{{ order.itemCount }}</td>
          <td class="action-buttons">
            <template v-if="showAllButtons">
              <button
                v-for="ev in ALL_EVENTS"
                :key="ev"
                :class="['event-btn', { 'event-btn-allowed': order.allowedEvents.includes(ev) }]"
                @click="sendEvent(order.id, ev)"
              >
                {{ ev }}
              </button>
            </template>
            <template v-else>
              <button
                v-for="ev in order.allowedEvents"
                :key="ev"
                class="event-btn event-btn-allowed"
                @click="sendEvent(order.id, ev)"
              >
                {{ ev }}
              </button>
              <span v-if="order.allowedEvents.length === 0" class="terminal-text">Terminal state</span>
            </template>
          </td>
        </tr>
      </tbody>
    </table>
  </main>
</template>

<style scoped>
.container {
  max-width: 900px;
  margin: 2rem auto;
  padding: 0 1rem;
  font-family: system-ui, -apple-system, sans-serif;
  color: #222;
}

h1 {
  margin-bottom: 1.5rem;
  font-size: 1.75rem;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 1.25rem;
}

.primary-btn {
  background: #2563eb;
  color: #fff;
  border: none;
  padding: 0.5rem 1rem;
  font-size: 0.95rem;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
}
.primary-btn:hover {
  background: #1d4ed8;
}

.toggle-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.9rem;
  color: #4b5563;
}

.error-banner {
  background: #fee2e2;
  border: 1px solid #f87171;
  color: #b91c1c;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  margin-bottom: 1rem;
  font-weight: 500;
}

.order-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 0.5rem;
}

.order-table th,
.order-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
}

.order-table th {
  background: #f9fafb;
  font-size: 0.85rem;
  text-transform: uppercase;
  color: #6b7280;
}

.empty-state {
  text-align: center;
  color: #9ca3af;
  padding: 2rem !important;
}

.badge {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}
.badge-cart { background: #e0e7ff; color: #3730a3; }
.badge-checked_out { background: #fef3c7; color: #92400e; }
.badge-paid { background: #dbeafe; color: #1e40af; }
.badge-shipped { background: #f3e8ff; color: #6b21a8; }
.badge-delivered { background: #dcfce7; color: #166534; }
.badge-cancelled { background: #fee2e2; color: #991b1b; }

.action-buttons {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.event-btn {
  background: #f3f4f6;
  border: 1px solid #d1d5db;
  color: #374151;
  border-radius: 4px;
  padding: 0.3rem 0.6rem;
  font-size: 0.8rem;
  cursor: pointer;
}
.event-btn-allowed {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #065f46;
  font-weight: 500;
}
.event-btn:hover {
  filter: brightness(0.95);
}

.terminal-text {
  font-size: 0.8rem;
  color: #9ca3af;
  font-style: italic;
}
</style>
