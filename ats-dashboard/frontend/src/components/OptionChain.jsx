import React from 'react';

const OptionChain = ({ data }) => {
  const isCallFocus = data?.auction_state === 'INITIATIVE_BUY';
  const isPutFocus = data?.auction_state === 'INITIATIVE_SELL';

  const getRowStyle = (type) => {
    if ((type === 'CE' && isPutFocus) || (type === 'PE' && isCallFocus)) {
      return 'opacity-30 filter grayscale';
    }
    return 'opacity-100';
  };

  const getHighlightStyle = (type) => {
    if ((type === 'CE' && isCallFocus)) {
        return 'bg-green-500/10 border border-green-500 shadow-[0_0_10px_#00FF41]';
    }
    if ((type === 'PE' && isPutFocus)) {
        return 'bg-red-500/10 border border-red-500 shadow-[0_0_10px_#FF3131]';
    }
    return '';
  }

  return (
    <div className="bg-[#161B22] border border-[#30363D] p-4 rounded-md">
      <h2 className="text-white font-bold mb-2">Dynamic Option Chain (The Sniper)</h2>
      <div className="space-y-2">
        {data?.option_window?.map((option) => (
          <div
            key={`${option.strike}-${option.type}`}
            className={`flex justify-between items-center p-2 rounded-md transition-all duration-300 ${getRowStyle(option.type)} ${getHighlightStyle(option.type)}`}
          >
            <span className={`font-mono font-bold ${option.type === 'CE' ? 'text-green-400' : 'text-red-400'}`}>
              {option.strike} {option.type}
            </span>
            <span className="font-mono text-white">{option.ltp.toFixed(2)}</span>
            <span className={`font-mono text-xs ${option.oi_chg > 0 ? 'text-green-300' : 'text-red-300'}`}>
              {option.oi_chg > 0 ? '+' : ''}{option.oi_chg.toFixed(1)}%
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default OptionChain;