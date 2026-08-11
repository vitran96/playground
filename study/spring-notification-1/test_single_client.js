// Single client integration test for Distributed Notification System
// Run with: node test_single_client.js

async function run() {
    console.log("Connecting single WebSocket client to ws://localhost:8080/ws...");
    const ws = new WebSocket('ws://localhost:8080/ws');

    ws.onopen = () => {
        console.log("✅ WebSocket connection opened");
    };

    ws.onmessage = async (event) => {
        let data;
        try {
            data = JSON.parse(event.data);
        } catch (e) {
            data = event.data;
        }

        if (data && data.type === 'CONNECTED' && data.clientId) {
            const clientId = data.clientId;
            console.log(`Received initial greeting with clientId: ${clientId}`);

            // Send notification via HTTP POST
            const postUrl = `http://localhost:8080/notification/${clientId}`;
            const payload = JSON.stringify({ 
                message: "Single client notification test payload", 
                timestamp: Date.now() 
            });

            console.log(`Sending HTTP POST to ${postUrl}...`);
            const res = await fetch(postUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: payload
            });
            const resText = await res.text();
            console.log(`HTTP POST response (${res.status}): ${resText}`);
        } else {
            console.log("Received notification message over WebSocket:", event.data);
            console.log("\n✅ SUCCESS: Single client notification test PASSED!");
            ws.close();
            process.exit(0);
        }
    };

    ws.onerror = (err) => {
        console.error("❌ WebSocket error:", err);
        process.exit(1);
    };

    setTimeout(() => {
        console.error("❌ Test timed out after 10 seconds");
        process.exit(1);
    }, 10000);
}

run();
