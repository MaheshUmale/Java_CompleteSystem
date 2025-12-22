import React from 'react';
import { useWebSocketBuffer } from './hooks/useWebSocketBuffer';
import Header from './components/Header';
import AuctionWidget from './components/AuctionWidget';
import HeavyweightWidget from './components/HeavyweightWidget';
import OptionChain from './components/OptionChain';
import TradePanel from './components/TradePanel';
import SentimentWidget from './components/SentimentWidget';
import ActiveTradesWidget from './components/ActiveTradesWidget';
import './index.css';

function App() {
  const data = useWebSocketBuffer('ws://localhost:7070/data');

  return (
    <div className="bg-[#0A0E17] min-h-screen p-4 text-white font-sans">
      <div className="grid grid-cols-12 gap-4">
        {/* Full-width Header */}
        <div className="col-span-12">
          <Header data={data} />
        </div>

        {/* Column 1 */}
        <div className="col-span-12 lg:col-span-4 space-y-4">
          <AuctionWidget data={data} />
          <HeavyweightWidget data={data} />
        </div>

        {/* Column 2 */}
        <div className="col-span-12 lg:col-span-4 space-y-4">
          <OptionChain data={data} />
          <SentimentWidget data={data} />
        </div>

        {/* Column 3 */}
        <div className="col-span-12 lg:col-span-4 space-y-4">
          <TradePanel data={data} />
          <ActiveTradesWidget data={data} />
        </div>
      </div>
    </div>
  );
}

export default App;
