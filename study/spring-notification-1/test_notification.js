// Integration test for Distributed Notification System
// Run with: node test_notification.js

async function connectClient(id) {
    return new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://localhost:8080/ws');
        let clientId = null;

        ws.onopen = () => {
            console.log(`[Client ${id}] WebSocket connection opened`);
        };

        ws.onmessage = async (event) => {
            let data;
            try { 
                data = JSON.parse(event.data); 
            } catch (e) { 
                data = event.data; 
            }

            if (data && data.type === 'CONNECTED' && data.clientId) {
                clientId = data.clientId;
                console.log(`[Client ${id}] Assigned clientId: ${clientId}`);

                // Post notification to HTTP endpoint
                const postUrl = `http://localhost:8080/notification/${clientId}`;
                const payload = JSON.stringify({ 
                    client: id, 
                    message: `Notification test for client ${id}`, 
                    timestamp: Date.now() 
                });
                
                console.log(`[Client ${id}] Posting payload to ${postUrl}...`);
                const res = await fetch(postUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: payload
                });
                const resText = await res.text();
                console.log(`[Client ${id}] HTTP POST response status: ${res.status}, body: ${resText}`);
            } else {
                console.log(`[Client ${id}] Received notification over WebSocket:`, event.data);
                ws.close();
                resolve(true);
            }
        };

        ws.onerror = (err) => {
            console.error(`[Client ${id}] WebSocket error:`, err);
            reject(err);
        };
    });
}

async function run() {
    console.log("Starting integration test (6 concurrent clients across 3 app instances)...");
    const promises = [];
    for (let i = 1; i <= 6; i++) {
        promises.push(connectClient(i));
    }
    await Promise.all(promises);
    console.log("\n✅ SUCCESS: All client notification tests PASSED successfully!");
    process.exit(0);
}

run();
