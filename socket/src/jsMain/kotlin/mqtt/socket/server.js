var net = require('net');

var server = net.createServer(function (socket) {
    socket.write('Echo server\r\n');
    socket.pipe(socket);
    socket.on('data', function (data) {
        console.log(data);
        textChunk = data.toString('utf8');
        console.log(textChunk);
        socket.write(textChunk);
    });
});

server.listen(8080, '127.0.0.1');