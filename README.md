ShadowPort
==========
The motive power of this project is there is one problem we want to solve:
How could we make a C/S application (server application bind on a port, client application connect to that port) still work in an Internet environment? The server is located in a machine of LAN A, while the client is located in a machine of LAN B, both the server machine and the client machine have no public ip, so they can not ping each other, but they have the access to the Internet, so they are physically connected with each other indirectly, so how could let the client can talk to the server directly just like in the same LAN?

There are several known solutions for this problem, like NAT, virtual LAN, etc.., they see the solution in the underline network layers, is it possible to solve this problem in the application layer in an easy understanding way?

This project goes the way like this:  
Create a shadow port for the client in the same LAN(or machine), this shadow port will act exactly as the real port which the server listen to, the client is just like connecting to a server locally. 

That's enough for the user of this project to understand the solution, for people who care about the implementation, you can go ahead.

Here are our solution:
Setup:
A mediator run on a machine with open ip, which can be accessed both by client machine and server machine.
On the client side (same LAN or machine), run a Virtual server, real client connect Virtual server.
On the server side (same LAN or machine), run a Virtual client, Virtual client connect to the real server
Virtual cleint, Virtual server build the connections with mediator.

Realtime:
When client send request, request go through Virtual server to mediator to Virtual client, fianlly to server,
after server processed, send back the response, it go through Virtual client to mediator to Virtual server，finally back to client.

NOTE&TODO:
All traffic go through the mediator as a proxy, a burden for the proxy server, we can use some technique like pole punching to let Virtual server and Virtual client communicate with each other directly after the hand shaking is over.
