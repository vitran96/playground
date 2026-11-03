<script setup>
import { ref, onMounted } from 'vue';

const files = ref([]);
const logs = ref([]);
const fileInput = ref(null);
const uploading = ref(false);

function log(action, details) {
  const time = new Date().toLocaleTimeString();
  const msg = typeof details === 'object' ? JSON.stringify(details, null, 2) : details;
  logs.value.unshift(`[${time}] ${action}\n${msg}`);
}

async function loadFiles() {
  try {
    const res = await fetch('/api/files');
    files.value = await res.json();
    log('GET /api/files', `Loaded ${files.value.length} files`);
  } catch (err) {
    log('ERROR GET /api/files', err.message);
  }
}

async function uploadViaBackend() {
  const file = fileInput.value?.files?.[0];
  if (!file) return alert('Select a file first');
  uploading.value = true;
  try {
    const fd = new FormData();
    fd.append('file', file);
    log('1. PROXY UPLOAD: POST /api/files', `Sending ${file.name} (${file.size} bytes) through Spring Boot`);
    const res = await fetch('/api/files', { method: 'POST', body: fd });
    const data = await res.json();
    log('PROXY UPLOAD SUCCESS', data);
    fileInput.value.value = '';
    await loadFiles();
  } catch (err) {
    log('PROXY UPLOAD FAILED', err.message);
  } finally {
    uploading.value = false;
  }
}

async function uploadDirect() {
  const file = fileInput.value?.files?.[0];
  if (!file) return alert('Select a file first');
  uploading.value = true;
  try {
    const contentType = file.type || 'application/octet-stream';
    log('4. DIRECT UPLOAD - Step 1: POST /api/files/upload-url', { name: file.name, contentType, size: file.size });
    const step1Res = await fetch('/api/files/upload-url', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: file.name, contentType, size: file.size })
    });
    const { id, url } = await step1Res.json();
    log('Step 1 response', { id, presignedPutUrl: url });

    log('4. DIRECT UPLOAD - Step 2: PUT straight to SeaweedFS', `PUT ${url} (bypassing Spring)`);
    const putRes = await fetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': contentType },
      body: file
    });
    if (!putRes.ok) throw new Error(`Direct PUT failed with status ${putRes.status}`);
    log('Step 2 response', `SeaweedFS returned HTTP ${putRes.status}`);

    log('4. DIRECT UPLOAD - Step 3: POST /api/files/' + id + '/confirm', 'Confirming upload with backend');
    const step3Res = await fetch(`/api/files/${id}/confirm`, { method: 'POST' });
    const fileRecord = await step3Res.json();
    log('Step 3 response', fileRecord);

    fileInput.value.value = '';
    await loadFiles();
  } catch (err) {
    log('DIRECT UPLOAD FAILED', err.message);
  } finally {
    uploading.value = false;
  }
}

async function openPresignedDownload(f) {
  try {
    log(`3. PRESIGNED LINK: GET /api/files/${f.id}/link`, 'Requesting signed download link from Spring');
    const res = await fetch(`/api/files/${f.id}/link`);
    const { url } = await res.json();
    log('Presigned download link received', url);
    window.location.href = url;
  } catch (err) {
    log('PRESIGNED LINK ERROR', err.message);
  }
}

async function deleteFile(f) {
  if (!confirm(`Delete ${f.originalName}?`)) return;
  try {
    log(`5. DELETE: DELETE /api/files/${f.id}`, 'Deleting object from SeaweedFS and row from Postgres');
    const res = await fetch(`/api/files/${f.id}`, { method: 'DELETE' });
    log('DELETE response', `HTTP ${res.status}`);
    await loadFiles();
  } catch (err) {
    log('DELETE ERROR', err.message);
  }
}

onMounted(() => {
  loadFiles();
});
</script>

<template>
  <main style="font-family: system-ui, sans-serif; max-width: 960px; margin: 2rem auto; padding: 0 1rem;">
    <h1>SeaweedFS Object Storage Playground</h1>
    <p style="color: #666;">
      Testing S3 upload/download patterns: Spring proxy vs direct SeaweedFS browser transfers.
    </p>

    <!-- Upload Controls -->
    <section style="background: #f4f4f5; padding: 1.25rem; border-radius: 8px; margin-bottom: 2rem;">
      <h2 style="margin-top: 0; font-size: 1.15rem;">Upload File</h2>
      <div style="display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap;">
        <input ref="fileInput" type="file" :disabled="uploading" />
        <button @click="uploadViaBackend" :disabled="uploading" style="padding: 0.5rem 1rem; cursor: pointer;">
          Upload via backend (Proxy)
        </button>
        <button @click="uploadDirect" :disabled="uploading" style="padding: 0.5rem 1rem; cursor: pointer; background: #0284c7; color: white; border: none; border-radius: 4px;">
          Upload direct (Presigned PUT)
        </button>
      </div>
      <div v-if="uploading" style="margin-top: 0.5rem; color: #0284c7; font-weight: bold;">
        Uploading...
      </div>
    </section>

    <!-- Files Table -->
    <section style="margin-bottom: 2rem;">
      <h2 style="font-size: 1.15rem;">Stored Files (Postgres metadata + SeaweedFS bytes)</h2>
      <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 0.95rem;">
        <thead>
          <tr style="border-bottom: 2px solid #ccc; background: #fafafa;">
            <th style="padding: 0.5rem;">Original Name</th>
            <th style="padding: 0.5rem;">Size</th>
            <th style="padding: 0.5rem;">Content-Type</th>
            <th style="padding: 0.5rem;">Status</th>
            <th style="padding: 0.5rem;">Object Key</th>
            <th style="padding: 0.5rem;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="files.length === 0">
            <td colspan="6" style="padding: 1rem; text-align: center; color: #888;">No files uploaded yet.</td>
          </tr>
          <tr v-for="f in files" :key="f.id" style="border-bottom: 1px solid #eee;">
            <td style="padding: 0.5rem; font-weight: 500;">{{ f.originalName }}</td>
            <td style="padding: 0.5rem;">{{ f.sizeBytes != null ? (f.sizeBytes / 1024).toFixed(1) + ' KB' : '-' }}</td>
            <td style="padding: 0.5rem; font-family: monospace; font-size: 0.85rem;">{{ f.contentType || '-' }}</td>
            <td style="padding: 0.5rem;">
              <span :style="{ color: f.status === 'READY' ? 'green' : 'orange', fontWeight: 'bold' }">
                {{ f.status }}
              </span>
            </td>
            <td style="padding: 0.5rem; font-family: monospace; font-size: 0.8rem; color: #555;">{{ f.objectKey }}</td>
            <td style="padding: 0.5rem; display: flex; gap: 0.5rem; align-items: center;">
              <a :href="`/api/files/${f.id}/content`" style="color: #2563eb; text-decoration: underline;" @click="log('2. PROXY DOWNLOAD', `/api/files/${f.id}/content`)">
                Download (proxy)
              </a>
              <button @click="openPresignedDownload(f)" style="padding: 0.25rem 0.5rem; cursor: pointer;">
                Get link (direct)
              </button>
              <button @click="deleteFile(f)" style="padding: 0.25rem 0.5rem; cursor: pointer; color: red;">
                Delete
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- Activity Log -->
    <section>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h2 style="font-size: 1.15rem; margin: 0;">Activity Log (Inspect network paths)</h2>
        <button @click="logs = []" style="padding: 0.25rem 0.5rem; cursor: pointer;">Clear log</button>
      </div>
      <div style="margin-top: 0.5rem; background: #1e1e1e; color: #d4d4d4; padding: 1rem; border-radius: 6px; height: 260px; overflow-y: auto; font-family: monospace; font-size: 0.85rem; white-space: pre-wrap;">
        <div v-if="logs.length === 0" style="color: #777;">No activity recorded yet. Perform an upload or download above.</div>
        <div v-for="(l, i) in logs" :key="i" style="margin-bottom: 0.75rem; border-bottom: 1px solid #333; padding-bottom: 0.5rem;">
          {{ l }}
        </div>
      </div>
    </section>
  </main>
</template>
