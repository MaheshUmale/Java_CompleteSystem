const WebSocket = require('ws');

const wss = new WebSocket.Server({ port: 8080 });

// Simulation Variables
let spot = 24650.00;
let timeRemaining = 1200; // 20 minutes in seconds for Theta-Guard
let mode = 'NORMAL'; // Switch to 'VOLATILE' to test RBI Event

console.log("JULES-HF-ATS Mock Server started on ws://localhost:8080");

wss.on('connection', (ws) => {
  const interval = setInterval(() => {
    // 1. Simulate Price Movement
    const volatility = mode === 'VOLATILE' ? 5.0 : 0.5;
    spot += (Math.random() - 0.48) * volatility; // Slight upward bias

    // 2. Simulate Weighted Delta (The Engine)
    const weightedDelta = (spot - 24650) * 1500 + (Math.random() * 500);

    // 3. Determine Auction State
    let state = "ROTATION";
    if (weightedDelta > 5000) state = "INITIATIVE_BUY";
    if (weightedDelta < -5000) state = "INITIATIVE_SELL";

    // 4. Update Theta-Guard
    timeRemaining = timeRemaining > 0 ? timeRemaining - 0.1 : 1200;

    // 5. Construct JSON Packet
    const packet = {
      timestamp: Date.now(),
      spot: parseFloat(spot.toFixed(2)),
      future: parseFloat((spot + 15).toFixed(2)),
      weighted_delta: Math.floor(weightedDelta),
      auction_state: state,
      pcr: parseFloat((1.1 + (Math.random() * 0.2)).toFixed(2)),
      theta_guard_sec: Math.floor(timeRemaining),
      heavyweights: [
        { name: "RELIANCE", delta: Math.floor(Math.random() * 5000), weight: "10.2%" },
        { name: "HDFC BANK", delta: Math.floor(Math.random() * 4500), weight: "9.1%" }
      ],
      option_window: [
        { strike: 24600, type: "CE", ltp: 110 + (Math.random() * 5), oi_chg: 5.2 },
        { strike: 24650, type: "CE", ltp: 75 + (Math.random() * 5), oi_chg: 12.5 },
        { strike: 24650, type: "PE", ltp: 80 + (Math.random() * 5), oi_chg: -2.1 }
      ]
    };

    ws.send(JSON.stringify(packet));
  }, 100); // 10Hz update rate

  ws.on('close', () => clearInterval(interval));
});
