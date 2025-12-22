import React, { useEffect, useRef, useState } from 'react';
import { createChart } from 'lightweight-charts';

const AuctionWidget = ({ data }) => {
  const chartContainerRef = useRef();
  const chartRef = useRef();
  const seriesRef = useRef();

  useEffect(() => {
    if (chartContainerRef.current) {
      chartRef.current = createChart(chartContainerRef.current, {
        layout: {
          background: { color: '#0A0E17' },
          textColor: '#8B949E',
          fontFamily: "'JetBrains Mono', monospace",
        },
        grid: {
          vertLines: { color: '#1C212E' },
          horzLines: { color: '#1C212E' },
        },
        rightPriceScale: {
          borderColor: '#30363D',
        },
        timeScale: {
          borderColor: '#30363D',
          timeVisible: true,
          secondsVisible: false,
        },
        crosshair: {
          mode: 0, // Magnet
        },
      });

      seriesRef.current = chartRef.current.addLineSeries({
        color: '#2962FF',
        lineWidth: 2,
      });

      const createPriceLine = (price, color, title) => ({
        price,
        color,
        lineWidth: 1,
        lineStyle: 2, // Dashed
        axisLabelVisible: true,
        title,
        axisLabelColor: '#fff',
        axisLabelTextColor: '#000',
      });

      seriesRef.current.createPriceLine(createPriceLine(24580, '#EF5350', 'VAH'));
      seriesRef.current.createPriceLine(createPriceLine(24550, '#FFCA28', 'POC'));
      seriesRef.current.createPriceLine(createPriceLine(24500, '#66BB6A', 'VAL'));

      chartRef.current.timeScale().fitContent();

      return () => {
        chartRef.current.remove();
      };
    }
  }, []);

  useEffect(() => {
    if (seriesRef.current && data?.timestamp && data?.spot) {
      seriesRef.current.update({
        time: data.timestamp / 1000,
        value: data.spot,
      });
    }
  }, [data]);


  return (
    <div className="bg-[#1C212E] p-4 rounded-lg shadow-lg h-full flex flex-col">
      <h2 className="text-xl font-bold mb-2">Auction Profile & Hea...</h2>
      <div className="flex-grow" ref={chartContainerRef} />
       <div className="flex justify-between text-xs pt-2 font-mono">
        <span>Total Vol: 1.2M</span>
        <span className="text-yellow-400">POC: 24550</span>
      </div>
    </div>
  );
};

export default AuctionWidget;
