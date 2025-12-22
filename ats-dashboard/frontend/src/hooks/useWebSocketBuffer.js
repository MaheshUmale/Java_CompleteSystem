import { useState, useEffect, useRef } from 'react';

export const useWebSocketBuffer = (url) => {
  const [data, setData] = useState(null);
  const buffer = useRef([]);
  const socket = useRef(null);
  const frameId = useRef(null);

  useEffect(() => {
    socket.current = new WebSocket(url);

    socket.current.onopen = () => {
      // WebSocket connection opened
    };

    socket.current.onmessage = (event) => {
      const receivedData = JSON.parse(event.data);
      buffer.current.push(receivedData);
    };

    socket.current.onclose = (event) => {
      // WebSocket connection closed
    };

    socket.current.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    const animate = () => {
      if (buffer.current.length > 0) {
        // Process all messages in the buffer, but only render the last one.
        const lastMessage = buffer.current[buffer.current.length - 1];
        setData(lastMessage);
        buffer.current = []; // Clear buffer after processing
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