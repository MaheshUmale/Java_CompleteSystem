import React from 'react';

const ActiveTradesWidget = ({ data }) => {

  const trades = data?.active_trades || [];

  const getPnlColor = (pnl) => pnl >= 0 ? 'text-green-400' : 'text-red-400';

  return (
    <div className="bg-[#1C212E] p-4 rounded-lg shadow-lg h-full">
      <h2 className="text-xl font-bold mb-4">Active Trades & Strategy</h2>
      <table className="w-full text-sm text-left font-mono">
        <thead>
          <tr className="text-gray-400 border-b border-gray-700">
            <th className="py-2">Symbol</th>
            <th className="py-2">Entry / LTP</th>
            <th className="py-2">Qty</th>
            <th className="py-2">P/L</th>
            <th className="py-2 text-right">Exit Reason</th>
          </tr>
        </thead>
        <tbody>
          {trades.map((trade) => (
            <tr key={trade.symbol} className="border-b border-gray-800">
              <td className="py-2">{trade.symbol}</td>
              <td className="py-2">{trade.entry.toFixed(2)} / {trade.ltp.toFixed(2)}</td>
              <td className="py-2">{trade.qty}</td>
              <td className={`py-2 ${getPnlColor(trade.pnl)}`}>{trade.pnl > 0 ? '+' : ''}{trade.pnl.toLocaleString()}</td>
              <td className="py-2 text-right text-yellow-400">{trade.reason}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="text-sm text-center mt-4">
        <span className="text-gray-400">Strategy: </span>
        <span className="font-bold">Responsive Buy (VAL Rejection)</span>
      </div>
    </div>
  );
};

export default ActiveTradesWidget;
