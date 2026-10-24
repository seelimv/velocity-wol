# Velocity-WoL

A Velocity proxy plugin that wakes up an offline server via Wake-on-LAN (WoL) when players join, holds them in a waiting server, and automatically transfers them once ready.

## Features

- Wakes up target servers via Wake-on-LAN when players connect.
- Keeps players in a holding server with action bar notifications while starting up.
- Automatically transfers players once the server is reachable (including after restarts).
- Console logging for sent WoL packets and ping status.
- Optional scheduled downtime to disable wakeups during specific hours.

## Configuration

Configurable via `config.conf` file:

- Holding and redirect server names
- Broadcast IP and target MAC address
- Ping frequency
- Optional downtime window
