from bluetooth import *
import mraa
import time


server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

advertise_service( server_sock, "BeagleBone",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ])

print("Waiting for connection on RFCOMM channel %d" % port)

client_sock, client_info = server_sock.accept()
print("Accepted connection from ", client_info)

t_end = time.time() + 15
beats = 0
first = True
while(True):
    x = mraa.Aio(0)
    try:
        pulse = x.read()
        if pulse and first:
            beats += 1
            first = False
        elif not pulse:
            first = True
            
    except:
        print("doesn't work")


    if time.time() > t_end:
        print(beats*4)
        try:
            client_sock.send(str(beats*4))
        except IOError:
            pass
        beats = 0
        t_end = t_end + 15
            
print("disconnected")

client_sock.close()
server_sock.close()
print("all done")

