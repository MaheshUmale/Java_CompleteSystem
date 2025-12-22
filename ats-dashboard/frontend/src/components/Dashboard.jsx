import React from 'react';
import { useWebSocketBuffer } from '../hooks/useWebSocketBuffer';
import AuctionWidget from './AuctionWidget';
import HeavyweightWidget from './HeavyweightWidget';
import OptionChain from './OptionChain';
import TradePanel from './TradePanel';
import SentimentWidget from './SentimentWidget';
import ActiveTradesWidget from './ActiveTradesWidget';

const Dashboard = () => {
  const data = useWebSocketBuffer('ws://localhost:7070/data');

  return (
    <div className="grid grid-cols-3 grid-rows-2 gap-4 flex-grow">
      <AuctionWidget data={data} />
      <OptionChain data={data} />
      <TradePanel data={data} />
      <HeavyweightWidget data={data} />
      <SentimentWidget data={data} />
      <ActiveTradesWidget data={data} />
    </div>
  );
};

export default Dashboard;
