import React from 'react';

const CircularProgress = ({ percentage, color }) => {
    const radius = 50;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference - (percentage / 100) * circumference;

    return (
        <svg className="w-24 h-24" viewBox="0 0 120 120">
            <circle
                className="text-gray-700"
                strokeWidth="10"
                stroke="currentColor"
                fill="transparent"
                r={radius}
                cx="60"
                cy="60"
            />
            <circle
                className={color}
                strokeWidth="10"
                strokeDasharray={circumference}
                strokeDashoffset={offset}
                strokeLinecap="round"
                stroke="currentColor"
                fill="transparent"
                r={radius}
                cx="60"
                cy="60"
                transform="rotate(-90 60 60)"
            />
        </svg>
    );
};


const TradePanel = ({ data }) => {
    const totalSeconds = 1200; // 20 minutes
    const remainingSeconds = data?.theta_guard_sec || 0;
    const percentage = (remainingSeconds / totalSeconds) * 100;

    let color = 'text-green-500';
    if (percentage < 50) color = 'text-yellow-500';
    if (percentage < 25) color = 'text-red-500 animate-pulse';

    const minutes = Math.floor(remainingSeconds / 60);
    const seconds = remainingSeconds % 60;

    return (
        <div className="bg-[#161B22] border border-[#30363D] p-4 rounded-md">
            <h2 className="text-white font-bold mb-2">Trade Panel & Theta-Guard</h2>
            <div className="flex flex-col items-center justify-center space-y-4">
                <div className="relative">
                    <CircularProgress percentage={percentage} color={color} />
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                        <span className="text-2xl font-mono font-bold">{`${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`}</span>
                        <span className="text-xs text-gray-400">REMAINING</span>
                    </div>
                </div>
                <button className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-2 px-4 rounded">
                    PANIC EXIT
                </button>
            </div>
        </div>
    );
};

export default TradePanel;
