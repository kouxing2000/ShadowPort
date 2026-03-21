# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ShadowPort is a Java networking tool that creates "shadow ports" to tunnel TCP traffic between machines in different LANs through an intermediary server. It allows client/server applications to work across the internet without modification, operating entirely at the application layer.

## Build and Test

```bash
# Build (from ShadowPortJavaProject/)
cd ShadowPortJavaProject && mvn clean package

# Run all tests
cd ShadowPortJavaProject && mvn test

# Run a single test
cd ShadowPortJavaProject && mvn test -Dtest=UnitTest#testClient2VirtualPeer2Mediator2VirtualPeer2Server

# Package distribution zip (includes bin scripts and dependencies)
cd ShadowPortJavaProject && mvn package
```

Maven project lives in `ShadowPortJavaProject/`. Java 1.6 source/target. Uses Netty 3.5.4 for NIO networking, XStream for serialization, Gson for JSON config parsing, SLF4J+Log4j for logging.

## Architecture

Three components form the tunneling system:

**Mediator** (`com.pipe.mediator.Mediator`) - Runs on a publicly accessible server. Accepts signal connections (control plane) and data connections (data plane) on separate ports. Manages peer registration, port mapping between virtual and real ports, and pipes data connections between paired peers. Exposes JMX MBean for runtime port mapping via JSON. Configured via JSON file (`MediatorConfiguration`).

**VirtualPeer** (`com.pipe.virtual.peer.VirtualPeer`) - Runs on both client and server sides. Connects to the Mediator's signal port, receives a `JoinResponse` with data connection parameters, then pre-establishes a pool of data connections. On the server side, opens a local "shadow port" that real clients connect to. On the client side, connects to the real server when traffic arrives. Peer type is `PEER` (bidirectional) by default.

**Proxy** (`com.pipe.virtual.proxy.Proxy`) - Simpler alternative for when the proxy can directly reach the real server. Just bridges incoming connections on a local port to the remote server, no mediator needed.

### Traffic Flow (full tunneling)
```
Real Client -> VirtualPeer(server-side, shadow port) -> Mediator -> VirtualPeer(client-side) -> Real Server
```

### Key Abstractions

- `Service` interface (`com.pipe.common.service.Service`) - `start()`/`stop()` lifecycle for all components
- `ChannelHolder` - Wraps a Netty Channel with a connection ID and optional piped channel reference
- `PeerHolder` - Mediator-side representation of a connected peer (signal channel + data connection pool)
- `PeerPortEntry` - Identifies a port on a specific peer (peerID, host, port)
- `MappingEntry` - Pairs a virtual port to a real port for the mediator config
- `DataConnectionEntry` - Identifies a specific data connection (peerID + connectionID)

### Signal Protocol Messages (`com.pipe.common.net.message`)

- `JoinRequest/JoinResponse` - Peer registration handshake
- `DataConnectionRegisterMessage` - Associates a data connection with its peer
- `DataConnectionEmployedMessage` - Signals a data connection is being used for real traffic (carries virtual/real host:port)
- `DataConnectionReleasedMessage` - Signals connection release for recycling
- `OpenVirtualPortMessage` - Instructs a VirtualPeer to open a shadow port

### Wire Format

Messages are serialized via XStream (XML) with length-field framing (`LengthFieldPrepender`/`LengthFieldBasedFrameDecoder`, 4-byte length prefix, max frame 1MB). The `Utils` class manages codec pipeline setup and channel bridging. SSL is optional for data connections (configured per-mediator).

## Running Standalone

Shell scripts in `ShadowPortJavaProject/bin/` run each component:
- `mediator.sh <config_file>` - Entry: `com.pipe.mediator.Mediator`
- `stub.sh <id> <mediator_host> <mediator_port>` - Entry: `com.pipe.virtual.peer.VirtualPeer`
- `proxy.sh <real_host> <real_port> <proxy_host> <proxy_port>` - Entry: `com.pipe.virtual.proxy.Proxy`

See `bin/sample.mediator.config` for mediator JSON configuration format.
