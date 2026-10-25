# Velocity-WoL
<!-- modrinth_exclude.start -->
[![CI](https://github.com/seelimv/velocity-wol/actions/workflows/ci.yml/badge.svg)](https://github.com/seelimv/velocity-wol/actions/workflows/ci.yml)
[![Modrinth](https://img.shields.io/modrinth/v/velocity-wol?label=Modrinth)](https://modrinth.com/plugin/velocity-wol)
[![Release](https://img.shields.io/github/v/release/seelimv/velocity-wol)](https://github.com/seelimv/velocity-wol/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/seelimv/velocity-wol/blob/main/LICENSE.md)
<!-- modrinth_exclude.end -->
Velocity-WoL is a Velocity proxy plugin that wakes up offline backend servers on demand using **Wake-on-LAN (WoL)**.

When a player connects to an offline server, the proxy sends a WoL magic packet, holds the player in a **holding server**, and **automatically transfers** them once the server is reachable.

## Features

- **On-demand wake-up**: Sends WoL magic packets to wake physical hosts when players join.
- **Holding & auto-transfer**: Keeps players on a waiting server with action bar updates and moves them once online.
- **Scheduled downtime**: Optional time windows to disable wake-ups during maintenance or nighttime hours.

## Requirements

- A registered holding server in `velocity.toml`.
- Target machine with Wake-on-LAN enabled in BIOS/UEFI and network adapter settings.

## Configuration

The plugin generates `plugins/velocity-wol/config.conf` on first startup:

```hocon
# Holding server where players wait while target wakes up
holding-server=limbo

# Frequency in seconds to ping the target server to check if it has come online
ping-interval-seconds=2

# Frequency in seconds to re-send WoL magic packets while players are in holding server
wol-packet-interval-seconds=10

# Timeout in seconds before giving up waiting for the server
wake-timeout-seconds=60

# Action bar messages notification settings
messages {
    # Action bar message shown while waiting for the server to wake up. Supports MiniMessage formatting.
    waking-action-bar="⌛ Waking up the server, please wait..."
    # Action bar message shown during downtime when WoL is disabled
    downtime-action-bar="<red>⌚ The server is currently closed for the night.</red>"
}

# Servers configured for Wake-on-LAN
servers {
    survival {
        # Target machine MAC address (e.g. 00:11:22:33:44:55)
        mac="AA:BB:CC:DD:EE:FF"
        # Broadcast IP address of the local subnet
        broadcast-ip="192.168.1.255"
        # WoL UDP target port (usually 7 or 9)
        port=9
        # Optional downtime schedule when WoL will not be sent
        downtime {
            # Enable downtime / restricted hours
            enabled=false
            # Timezone for downtime schedule (e.g., 'UTC', 'Europe/Berlin')
            timezone=UTC
            # Downtime start time in 24h format (HH:mm)
            start-time="00:00"
            # Downtime end time in 24h format (HH:mm)
            end-time="08:00"
        }
    }
}
```

### General Settings

| Option                         | Default             | Description                                                                 |
|--------------------------------|---------------------|-----------------------------------------------------------------------------|
| `holding-server`               | `limbo`             | Server registered in `velocity.toml` where waiting players are held.        |
| `ping-interval-seconds`        | `2`                 | Interval in seconds between ping checks to the target server.               |
| `wol-packet-interval-seconds`  | `10`                | Interval in seconds to repeat WoL packets while players wait.               |
| `wake-timeout-seconds`         | `60`                | Maximum time in seconds to wait before aborting an unresponsive wake cycle. |
| `messages.waking-action-bar`   | `⌛ Waking up...`   | Action bar message shown while waking up (supports MiniMessage).            |
| `messages.downtime-action-bar` | `<red>⌚ ...</red>` | Action bar message shown if a player joins during scheduled downtime.       |

### Server Settings (`servers.<name>`)

| Option                | Default             | Description                                                                          |
|-----------------------|---------------------|--------------------------------------------------------------------------------------|
| `mac`                 | `00:00:00:00:00:00` | MAC address of the target network interface (accepts `:` or `-`).                    |
| `broadcast-ip`        | `255.255.255.255`   | Subnet broadcast address (e.g., `192.168.1.255`).                                    |
| `port`                | `9`                 | Target UDP port for the magic packet (usually 7 or 9).                               |
| `downtime.enabled`    | `false`             | Enables scheduled downtime for this server.                                          |
| `downtime.timezone`   | `UTC`               | Time zone used to evaluate downtime (e.g., `Europe/Berlin`).                         |
| `downtime.start-time` | `00:00`             | Start time in 24-hour notation (`HH:mm`).                                            |
| `downtime.end-time`   | `08:00`             | End time in 24-hour notation (`HH:mm`). Supports overnight ranges crossing midnight. |
<!-- modrinth_exclude.start -->
## Building from Source

```bash
git clone https://github.com/seelimv/velocity-wol.git
cd velocity-wol
./gradlew shadowJar
```

The compiled plugin jar will be located in `build/libs/`.
<!-- modrinth_exclude.end -->
