# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ShadowPort is a Java networking tool that creates "shadow ports" to tunnel TCP traffic between machines in different LANs through an intermediary server. It allows client/server applications to work across the internet without modification, operating entirely at the application layer.

## Build and Test

```bash
# Build
cd ShadowPortJavaProject && mvn clean package

# Run all tests
cd ShadowPortJavaProject && mvn test

# Run a single test
cd ShadowPortJavaProject && mvn test -Dtest=UnitTest#testClient2VirtualPeer2Mediator2VirtualPeer2Server
```

Maven project in `ShadowPortJavaProject/`. Java 1.6 target. Dependencies: Netty 3.5.4 (NIO), XStream 1.4.3 (serialization), Gson 1.4 (config), SLF4J+Log4j (logging), JUnit 4.8.2.

## Architecture

Three components form the tunneling system:

**Mediator** (`com.pipe.mediator.Mediator`) - Runs on a publicly accessible server. Accepts signal connections (control plane) and data connections (data plane) on separate ports. Manages peer registration and port mappings. Supports SSL for data connections. Exposes JMX MBean for runtime port mapping.

**VirtualPeer** (`com.pipe.virtual.peer.VirtualPeer`) - Also called "stub" in scripts. Runs on both client and server sides. Connects to the Mediator's signal port and pre-establishes a pool of data connections. On server side, opens a shadow port for real clients. On client side, connects to real server when traffic arrives.

**Proxy** (`com.pipe.virtual.proxy.Proxy`) - Simpler alternative when direct access to the real server is available. Bridges incoming connections to a remote server without needing a mediator.

### Traffic Flow (full tunneling)
```
Real Client -> VirtualPeer(server-side, shadow port) -> Mediator -> VirtualPeer(client-side) -> Real Server
```

### Key Classes

- `Service` (`com.pipe.common.service.Service`) - `start()`/`stop()` lifecycle interface for all components
- `ChannelHolder` - Wraps a Netty Channel with connection ID and piped channel reference
- `PeerHolder` - Mediator-side peer representation (signal channel + data connection pool)
- `PeerPortEntry` / `MappingEntry` / `DataConnectionEntry` - Port mapping and connection identity

### Signal Protocol (`com.pipe.common.net.message`)

- `JoinRequest/JoinResponse` - Peer registration handshake
- `DataConnectionRegisterMessage` - Associates data connection with peer
- `DataConnectionEmployedMessage/ReleasedMessage` - Connection lifecycle for traffic
- `OpenVirtualPortMessage` - Instructs VirtualPeer to open shadow port

### Wire Format

XStream (XML) serialization with length-field framing (4-byte prefix, max 1MB frame). `Utils` class manages codec pipeline setup. SSL optional for data connections.

## Running Standalone

Shell scripts in `ShadowPortJavaProject/bin/`:
- `mediator.sh <config_file>` - Start mediator
- `stub.sh <id> <mediator_host> <mediator_port>` - Start VirtualPeer
- `proxy.sh <real_host> <real_port> <proxy_host> <proxy_port>` - Start Proxy

### Mediator Configuration

JSON config file (see `bin/sample.mediator.config`):
- `signalHost/signalPort` - Control plane listener
- `dataHost/dataPort` - Data plane listener
- `publicDataHost/publicDataPort` - Public-facing address for NAT scenarios
- `portMappings` - Array of `{virtualPort, realPort}` entries linking peers
- `minimalIdleDataConnections` - Pool size per peer
- `usingSSLForDataConnection` - Enable SSL for data plane
