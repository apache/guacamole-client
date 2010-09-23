<?php

# End all output buffers
while (@ob_end_flush());

# Create socket
$socket = socket_create(AF_INET, SOCK_STREAM, 0);
if (!$socket) {
    die("error:Could not create socket.;");
}

# Open connection
$result = socket_connect($socket, "localhost", 2222); 
if (!$result) {
    die("error:Could not connect: " . socket_strerror(socket_last_error()) . ";");
}

# Socket should block
socket_set_block($socket);

# Write all data sent here
socket_write($socket, file_get_contents("php://input"));

# Read until EOF
while (($buffer = socket_read($socket, 8192))) {
    echo $buffer;
    flush();
}

echo ";";

# Close socket
socket_close($socket);

?>
