import React from 'react';

const AuctionWidget = ({ data }) => {
  return (
    <div className="bg-[#161B22] border border-[#30363D] p-4 rounded-md h-full">
      <h2 className="text-white font-bold mb-2">Auction Profile (AMT Core)</h2>
      <div className="bg-[#0A0E17] h-full flex items-center justify-center">
        <p className="text-[#8B949E]">Volume Profile Chart (Canvas)</p>
      </div>
    </div>
  );
};

export default AuctionWidget;