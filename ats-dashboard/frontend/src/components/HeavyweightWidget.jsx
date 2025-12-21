import React, { useState, useEffect } from 'react';

const HeavyweightWidget = ({ data }) => {
  const [pulse, setPulse] = useState('');

  useEffect(() => {
    if (data) {
      setPulse('animate-pulse');
      const timer = setTimeout(() => setPulse(''), 500);
      return () => clearTimeout(timer);
    }
  }, [data?.weighted_delta]);

  return (
    <div className={`bg-[#161B22] border border-[#30363D] p-4 rounded-md ${pulse}`}>
      <h2 className="text-white font-bold mb-2">Weighted Heavyweights (The Engine)</h2>
      <div className="space-y-2">
        {data?.heavyweights?.map((stock) => (
          <div key={stock.name} className="flex justify-between text-sm">
            <span className="font-mono text-white">{stock.name}</span>
            <span className={`font-mono ${stock.delta > 0 ? 'text-green-400' : 'text-red-400'}`}>
              {stock.delta > 0 ? '+' : ''}{stock.delta}
            </span>
          </div>
        ))}
      </div>
      <div className="border-t border-[#30363D] mt-4 pt-2">
        <div className="flex justify-between text-sm font-bold">
          <span className="text-white">Total Index Pressure</span>
          <span className={`font-mono ${data?.weighted_delta > 0 ? 'text-green-400' : 'text-red-400'}`}>
            {data?.weighted_delta > 0 ? '+' : ''}{data?.weighted_delta}
          </span>
        </div>
      </div>
    </div>
  );
};

export default HeavyweightWidget;