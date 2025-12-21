import React from 'react';

const SentimentWidget = ({ data }) => {
  const auctionState = data?.auction_state || 'ROTATION';
  const pcr = data?.pcr || 1.1;

  const getStateColor = (state) => {
    switch (state) {
      case 'INITIATIVE_BUY':
        return 'text-green-400';
      case 'INITIATIVE_SELL':
        return 'text-red-400';
      default:
        return 'text-gray-400';
    }
  };

  return (
    <div className="bg-[#161B22] border border-[#30363D] p-4 rounded-md">
      <h2 className="text-white font-bold mb-2">Sentiment & Trigger Alerts</h2>
      <div className="space-y-4">
        <div className="text-center">
            <p className="text-xs text-gray-400">AUCTION STATE</p>
            <p className={`font-mono text-lg font-bold ${getStateColor(auctionState)}`}>
                {auctionState.replace('_', ' ')}
            </p>
        </div>
        <div className="text-center">
            <p className="text-xs text-gray-400">GLOBAL PCR</p>
            <p className="font-mono text-lg font-bold text-white">{pcr.toFixed(2)}</p>
        </div>
        <div className="text-left text-xs text-gray-400 space-y-1">
            <p className="font-bold text-gray-300">Alert Feed:</p>
            <p className="text-green-400">[ALERT]: PCR Divergence Detected - Bullish.</p>
            <p className="text-amber-400">[SIGNAL]: Price approaching Call Wall @ 24,700.</p>
        </div>
      </div>
    </div>
  );
};

export default SentimentWidget;
