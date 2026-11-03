<script setup>
import { ref, onMounted } from 'vue';
import * as t from './transfer.js';

const files = ref([]);
const selectedId = ref('');
const selected = ref(null);
const picked = ref(null);
const busy = ref(false);
const progress = ref(null);
const message = ref('');

const mb = b => (b / 1048576).toFixed(1);

async function refresh() {
  try {
    const res = await fetch('/api/files');
    if (res.ok) {
      files.value = await res.json();
      if (!files.value.some(f => f.id === selectedId.value)) {
        selectedId.value = files.value[0]?.id || '';
      }
      onSelectChange();
    }
  } catch (e) {
    message.value = `Failed to load files: ${e.message}`;
  }
}

function onSelectChange() {
  selected.value = files.value.find(f => f.id === selectedId.value) || null;
}

async function run(label, task) {
  busy.value = true;
  progress.value = null;
  message.value = `${label} running…`;
  const t0 = performance.now();
  try {
    await task((loaded, total) => (progress.value = { loaded, total }));
    const secs = ((performance.now() - t0) / 1000).toFixed(1);
    message.value = `${label} done in ${secs} s`;
    await refresh();
  } catch (e) {
    message.value = `${label} failed: ${e.message}`;
  } finally {
    busy.value = false;
  }
}

function runBrowserDownload() {
  if (!selected.value) return;
  t.browserDownload(selected.value);
  message.value = `Browser download started, see the browser's download bar.`;
}

async function deleteSelected() {
  if (!selected.value) return;
  busy.value = true;
  message.value = `Deleting ${selected.value.name}…`;
  try {
    const res = await fetch(`/api/files/${selected.value.id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    message.value = `Deleted ${selected.value.name}`;
    await refresh();
  } catch (e) {
    message.value = `Delete failed: ${e.message}`;
  } finally {
    busy.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <main class="container">
    <h1>Big File Upload &amp; Download Demo</h1>
    <p class="subtitle">Stream multi-GB files with flat server and client memory.</p>

    <!-- Upload section -->
    <section class="card">
      <h2>1. Upload</h2>
      <div class="field">
        <label for="file-picker">Choose file:</label>
        <input
          id="file-picker"
          type="file"
          :disabled="busy"
          @change="e => (picked = e.target.files?.[0] || null)"
        />
        <span v-if="picked" class="file-info">({{ mb(picked.size) }} MB)</span>
      </div>

      <div class="actions">
        <div class="btn-group">
          <button
            :disabled="busy || !picked"
            @click="run('JS upload', p => t.jsUpload(picked, p))"
          >
            JS upload
          </button>
          <small>Whole file in one request. Fails? Start over.</small>
        </div>

        <div class="btn-group">
          <button
            :disabled="busy || !picked"
            @click="run('Chunk upload', p => t.chunkUpload(picked, p))"
          >
            Chunk upload
          </button>
          <small>8 MB slices uploaded with retry. Survives network dropouts.</small>
        </div>
      </div>
    </section>

    <!-- Files section -->
    <section class="card">
      <h2>2. Files on server</h2>
      <div class="field" v-if="files.length > 0">
        <select
          v-model="selectedId"
          :disabled="busy"
          @change="onSelectChange"
        >
          <option v-for="f in files" :key="f.id" :value="f.id">
            {{ f.name }} ({{ mb(f.size) }} MB)
          </option>
        </select>
        <button
          class="btn-danger"
          :disabled="busy || !selected"
          @click="deleteSelected"
        >
          Delete
        </button>
      </div>
      <p v-else class="empty-note">No files uploaded yet.</p>
    </section>

    <!-- Download section -->
    <section class="card">
      <h2>3. Download</h2>
      <div class="actions">
        <div class="btn-group">
          <button
            :disabled="busy || !selected"
            @click="run('JS download', p => t.jsDownload(selected, p))"
          >
            JS download
          </button>
          <small>Reads stream into memory Blob. Tab memory scales with file size.</small>
        </div>

        <div class="btn-group">
          <button
            :disabled="busy || !selected"
            @click="runBrowserDownload"
          >
            Browser download
          </button>
          <small>Standard link download. Managed by browser download manager.</small>
        </div>

        <div class="btn-group">
          <button
            :disabled="busy || !selected"
            @click="run('HTTP range download', p => t.rangeDownload(selected, p))"
          >
            HTTP range request
          </button>
          <small>16 MB parallel chunks via Range header. Fast multi-stream.</small>
        </div>

        <div class="btn-group">
          <button
            :disabled="busy || !selected || !t.fsSupported"
            @click="run('File System Access', p => t.fsDownload(selected, p))"
          >
            File System Access
          </button>
          <small v-if="t.fsSupported">Writes straight to a file you pick without tab memory blowup.</small>
          <small v-else>Writes straight to a file you pick. Chromium only.</small>
        </div>
      </div>
    </section>

    <!-- Status section -->
    <section class="card status-card">
      <h2>4. Status</h2>
      <div v-if="progress" class="progress-box">
        <progress :value="progress.loaded" :max="progress.total"></progress>
        <div class="progress-label">
          {{ mb(progress.loaded) }} / {{ mb(progress.total) }} MB
          ({{ Math.round((progress.loaded / progress.total) * 100) }}%)
        </div>
      </div>
      <p class="status-message" v-if="message">{{ message }}</p>
      <p class="status-message text-muted" v-else>Ready.</p>
    </section>
  </main>
</template>

<style>
:root {
  --bg: #f8fafc;
  --card-bg: #ffffff;
  --border: #e2e8f0;
  --text: #0f172a;
  --text-muted: #64748b;
  --primary: #2563eb;
  --primary-hover: #1d4ed8;
  --danger: #dc2626;
  --danger-hover: #b91c1c;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  background-color: var(--bg);
  color: var(--text);
  line-height: 1.5;
}

.container {
  max-width: 760px;
  margin: 2rem auto;
  padding: 0 1rem;
}

h1 {
  font-size: 1.75rem;
  margin-bottom: 0.25rem;
}

.subtitle {
  color: var(--text-muted);
  margin-top: 0;
  margin-bottom: 1.5rem;
}

.card {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 1.25rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.card h2 {
  font-size: 1.15rem;
  margin-top: 0;
  margin-bottom: 1rem;
}

.field {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.file-info {
  color: var(--text-muted);
  font-size: 0.9rem;
}

.actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.btn-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.btn-group small {
  color: var(--text-muted);
  font-size: 0.8rem;
  line-height: 1.25;
}

button {
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.6rem 1rem;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

button:hover:not(:disabled) {
  background: var(--primary-hover);
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-danger {
  background: var(--danger);
}

.btn-danger:hover:not(:disabled) {
  background: var(--danger-hover);
}

select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 0.95rem;
  flex: 1;
}

.empty-note {
  color: var(--text-muted);
  font-style: italic;
  margin: 0;
}

.status-card {
  border-left: 4px solid var(--primary);
}

.progress-box {
  margin-bottom: 0.75rem;
}

progress {
  width: 100%;
  height: 12px;
  border-radius: 6px;
  overflow: hidden;
}

.progress-label {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.status-message {
  margin: 0;
  font-weight: 500;
}

.text-muted {
  color: var(--text-muted);
}
</style>
