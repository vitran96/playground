export function sendBlob(method, url, blob, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(method, url);
    xhr.setRequestHeader('Content-Type', 'application/octet-stream');
    if (onProgress) {
      xhr.upload.onprogress = e => onProgress(e.loaded);
    }
    xhr.onload = () => {
      if (xhr.status < 300) {
        try {
          resolve(xhr.responseText ? JSON.parse(xhr.responseText) : null);
        } catch {
          resolve(xhr.responseText);
        }
      } else {
        reject(new Error(`HTTP ${xhr.status}: ${xhr.statusText || 'request failed'}`));
      }
    };
    xhr.onerror = () => reject(new Error('network error'));
    xhr.send(blob);
  });
}

export async function postJson(url, data) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json().catch(() => ({}));
}

export const jsUpload = (file, progress) =>
  sendBlob('POST', `/api/files?name=${encodeURIComponent(file.name)}`, file,
           loaded => progress(loaded, file.size));

const CHUNK = 8 * 1024 * 1024;

export async function chunkUpload(file, progress) {
  const { id, totalChunks } = await postJson('/api/uploads',
    { name: file.name, size: file.size, chunkSize: CHUNK });
  let done = 0;
  for (let i = 0; i < totalChunks; i++) {
    const part = file.slice(i * CHUNK, Math.min(file.size, (i + 1) * CHUNK));
    await withRetry(3, () => sendBlob('PUT', `/api/uploads/${id}/chunks/${i}`, part,
                                      loaded => progress(done + loaded, file.size)));
    done += part.size;
  }
  return postJson(`/api/uploads/${id}/complete`, {});
}

export async function withRetry(times, fn) {
  for (let attempt = 1; ; attempt++) {
    try {
      return await fn();
    } catch (e) {
      if (attempt >= times) throw e;
      await new Promise(r => setTimeout(r, 500 * attempt));
    }
  }
}

export function saveBlob(blob, name) {
  const url = URL.createObjectURL(blob);
  Object.assign(document.createElement('a'), { href: url, download: name }).click();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

export async function jsDownload(f, progress) {
  const res = await fetch(`/api/files/${f.id}/content`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const reader = res.body.getReader();
  const parts = [];
  let loaded = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    parts.push(value);
    loaded += value.length;
    progress(loaded, f.size);
  }
  saveBlob(new Blob(parts), f.name);
}

export function browserDownload(f) {
  Object.assign(document.createElement('a'),
    { href: `/api/files/${f.id}/content?attachment=true` }).click();
}

const RANGE = 16 * 1024 * 1024, PARALLEL = 4;

export async function rangeDownload(f, progress) {
  const count = Math.ceil(f.size / RANGE);
  const parts = new Array(count);
  let next = 0, loaded = 0;
  async function worker() {
    while (next < count) {
      const i = next++;
      const start = i * RANGE, end = Math.min(f.size, start + RANGE) - 1;
      parts[i] = await withRetry(3, async () => {
        const res = await fetch(`/api/files/${f.id}/content`,
                                { headers: { Range: `bytes=${start}-${end}` } });
        if (res.status !== 206) throw new Error(`expected 206, got ${res.status}`);
        return res.blob();
      });
      loaded += end - start + 1;
      progress(loaded, f.size);
    }
  }
  await Promise.all(Array.from({ length: PARALLEL }, worker));
  saveBlob(new Blob(parts), f.name);
}

export async function fsDownload(f, progress) {
  const handle = await window.showSaveFilePicker({ suggestedName: f.name });
  const writable = await handle.createWritable();
  const res = await fetch(`/api/files/${f.id}/content`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  let loaded = 0;
  const counter = new TransformStream({
    transform(chunk, ctl) {
      loaded += chunk.length;
      progress(loaded, f.size);
      ctl.enqueue(chunk);
    },
  });
  await res.body.pipeThrough(counter).pipeTo(writable);
}

export const fsSupported = typeof window !== 'undefined' && 'showSaveFilePicker' in window;
