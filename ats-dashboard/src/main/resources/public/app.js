document.addEventListener('DOMContentLoaded', function () {
    const chartOptions = {
        series: [{
            name: 'VolumeBar',
            data: []
        }],
        chart: {
            type: 'candlestick',
            height: 350
        },
        title: {
            text: 'Volume Bars',
            align: 'left'
        },
        xaxis: {
            type: 'datetime'
        },
        yaxis: {
            tooltip: {
                enabled: true
            }
        }
    };

    const chart = new ApexCharts(document.querySelector("#chart"), chartOptions);
    chart.render();

    const obiValueElement = document.getElementById('obi-value');

    const socket = new WebSocket('ws://localhost:7070/data');

    socket.onopen = function(event) {
        console.log("WebSocket connection established.");
    };

    socket.onmessage = function(event) {
        const volumeBar = JSON.parse(event.data);

        // Update OBI display
        obiValueElement.textContent = volumeBar.orderBookImbalance.toFixed(4);

        // Update chart
        const newDataPoint = {
            x: new Date(volumeBar.timestamp),
            y: [volumeBar.open, volumeBar.high, volumeBar.low, volumeBar.close]
        };

        chart.appendData([{
            data: [newDataPoint]
        }]);
    };

    socket.onclose = function(event) {
        console.log("WebSocket connection closed.");
    };

    socket.onerror = function(error) {
        console.error("WebSocket error:", error);
    };
});
