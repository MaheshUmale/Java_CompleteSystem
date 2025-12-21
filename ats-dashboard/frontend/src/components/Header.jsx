import React, { useRef, useEffect, useState } from 'react';
import { Wifi } from 'lucide-react';

const Header = ({ data }) => {
  const prevSpot = useRef(null);
  const [spotColor, setSpotColor] = useState('text-green-400');

  useEffect(() => {
    if (data && data.spot) {
      if (prevSpot.current !== null) {
        if (data.spot > prevSpot.current) {
          setSpotColor('text-green-400');
        } else if (data.spot < prevSpot.current) {
          setSpotColor('text-red-400');
        }
      }
      prevSpot.current = data.spot;
    }
  }, [data]);

  const spot = data?.spot?.toFixed(2) || '0.00';
  const future = data?.future?.toFixed(2) || '0.00';
  const basis = (data?.future - data?.spot)?.toFixed(2) || '0.00';

  return (
    <div className="bg-[#161B22] border border-[#30363D] p-2 rounded-md flex justify-between items-center">
      <h1 className="text-xl font-bold text-white">JULES-HF-ATS</h1>
      <div className="flex items-center space-x-4">
        <div className="text-center">
          <p className="text-xs text-[#8B949E]">NIFTY SPOT</p>
          <p className={`font-mono transition-colors duration-100 ${spotColor}`}>{spot}</p>
        </div>
        <div className="text-center">
          <p className="text-xs text-[#8B949E]">NIFTY FUTURE</p>
          <p className="font-mono text-green-400">{future}</p>
        </div>
        <div className="text-center">
          <p className="text-xs text-[#8B949E]">BASIS</p>
          <p className={`font-mono ${basis > 15 ? 'text-amber-400' : 'text-gray-400'}`}>{basis}</p>
        </div>
      </div>
      <div className="flex items-center space-x-2 text-xs text-[#8B949E]">
        <Wifi size={16} className="text-green-500" />
        <span>System Health: OK</span>
        <span>WSS: 2ms</span>
      </div>
    </div>
  );
};

export default Header;
