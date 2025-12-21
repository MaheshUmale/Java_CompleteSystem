import { useState, useEffect, useRef } from 'react';

export const useWebSocketBuffer = (url) => {
  const [data, setData] = useState(null);
  const buffer = useRef([]);
  const socket = useRef(null);
  const frameId = useRef(null);

  useEffect(() => {
    socket.current = new WebSocket(url);

    socket.current.onopen = () => {
      console.log('WebSocket connection opened');
    };

    socket.current.onmessage = (event) => {
      buffer.current.push(JSON.parse(event.data));
    };

    socket.current.onclose = () => {
      console.log('WebSocket connection closed');
    };

    const animate = () => {
      if (buffer.current.length > 0) {
        setData(buffer.current[buffer.current.length - 1]);
        buffer.current = [];
      }
      frameId.current = requestAnimationFrame(animate);
    };

    frameId.current = requestAnimationFrame(animate);

    return () => {
      if (socket.current) {
        socket.current.close();
      }
      cancelAnimationFrame(frameId.current);
    };
  }, [url]);

  return data;
};