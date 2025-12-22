import React from 'react';
import Header from './components/Header';
import Dashboard from './components/Dashboard';
import './index.css';

function App() {
  return (
    <div className="bg-[#0A0E17] min-h-screen p-4 text-white font-sans flex flex-col">
      <Header />
      <main className="flex-grow mt-4">
        <Dashboard />
      </main>
    </div>
  );
}

export default App;
