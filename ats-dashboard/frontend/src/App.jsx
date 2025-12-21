import React from 'react';
import { useWebSocketBuffer } from './hooks/useWebSocketBuffer';
import Header from './components/Header';
import AuctionWidget from './components/AuctionWidget';
import HeavyweightWidget from './components/HeavyweightWidget';
import OptionChain from './components/OptionChain';
import TradePanel from './components/TradePanel';
import SentimentWidget from './components/SentimentWidget';
import './index.css';

function App() {
  const data = useWebSocketBuffer('ws://localhost:8080');

  return (
    <div className="bg-[#0A0E17] min-h-screen p-4 text-white">
      <div className="grid grid-cols-12 gap-4">
        {/* Full-width Header */}
        <div className="col-span-12">
          <Header data={data} />
        </div>

        {/* Left Column */}
        <div className="col-span-3 space-y-4">
          <HeavyweightWidget data={data} />
          <SentimentWidget data={data} />
        </div>

        {/* Center Column */}
        <div className="col-span-6">
          <AuctionWidget data={data} />
        </div>

        {/* Right Column */}
        <div className="col-span-3 space-y-4">
          <OptionChain data={data} />
          <TradePanel data={data} />
        </div>
      </div>
    </div>
  );
}

export default App;