mediator 127.0.0.2 9999 127.0.0.2 7777

client vc1 127.0.0.2 9999

server vs2 127.0.0.2 9999

Mbean match

{"peerID":"vs1","host":"127.0.0.3","port":1234}

{"peerID":"vc1","host":"127.0.0.1","port":80}

or 

mediator 127.0.0.2 9999 127.0.0.2 7777 124.205.4.53 7777 ./sample.mediator.config



======================
for proxy
======================

proxy 127.0.0.2 9999 127.0.0.2 7777