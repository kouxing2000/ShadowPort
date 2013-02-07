Why need this:
----
The motive power of this project is there is one problem we want to solve:
<br>
How could we make a C/S application (server application bind on a port, client application connect to that port) still work in an Internet environment, without any modification to existing program? The server is located in a machine of LAN A, while the client is located in a machine of LAN B, both the server machine and the client machine have no public ip, so they can not ping each other, but they have the access to the Internet, so they are physically connected with each other indirectly, so how could let the client can talk to the server directly just like in the same LAN?
<br>
There are several known solutions for this problem, like NAT, virtual LAN, etc.., they solve this solution in the underline network layers, have some requirement on the network devices or environment, is it possible to solve this problem in the application layer, less dependent?

What is the solution?
----
We create a shadow port for the client in the same LAN(or machine), this shadow port will act exactly as the real port which the server listen to, the client is just like connecting to a server locally. A pipe across the internet is build up between the shadow port and the real port.

That's enough for the users of this project to understand the solution, for people who care about the implementation, you can go ahead.

How to achieve that?
----
Here are our solution in a tecnical way:<br>
Set up:<br>
A mediator run on a machine with open IP, which can be accessed both by client machine and server machine.
On the client side (same LAN or machine), run a Virtual server, real client connect Virtual server.
On the server side (same LAN or machine), run a Virtual client, Virtual client connect to the real server
Virtual cleint, Virtual server build the connections with mediator.

Run time:<br>
When client send request, request go through Virtual server to mediator to Virtual client, fianlly to server,
after server processed, send back the response, it go through Virtual client to mediator to Virtual server，finally back to client.

NOTE&&TODO:<br>
All traffic go through the mediator as a proxy, a burden for the proxy server, we can use some technique like pole punching to let Virtual server and Virtual client communicate with each other directly after the hand shaking is over.

How to use?
----
Can use the project as a standalone application or be embed in your java application.<BR>
TODO wiki

Features:
----
Support several clients and severs connect to the same mediator.<BR>
Dynamic configuration for the port mappings.
