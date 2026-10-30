#!/usr/bin/env python3
"""Sample a local Minecraft server command through RCON at tick-sized intervals.

Example: python3 tools/local_rcon_probe.py --password-file /tmp/rcon-pass \
    --command 'data get entity Player123 Pos' --seconds 2
"""

import argparse
import socket
import struct
import time
from pathlib import Path


def read_exact(sock: socket.socket, size: int) -> bytes:
    data = bytearray()
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise ConnectionError("RCON connection closed")
        data.extend(chunk)
    return bytes(data)


def send_packet(sock: socket.socket, request_id: int, packet_type: int, payload: str) -> None:
    body = struct.pack("<ii", request_id, packet_type) + payload.encode("utf-8") + b"\0\0"
    sock.sendall(struct.pack("<i", len(body)) + body)


def receive_packet(sock: socket.socket) -> tuple[int, int, str]:
    (length,) = struct.unpack("<i", read_exact(sock, 4))
    if not 10 <= length <= 1_048_576:
        raise ValueError(f"Invalid RCON packet length: {length}")
    body = read_exact(sock, length)
    request_id, packet_type = struct.unpack_from("<ii", body)
    return request_id, packet_type, body[8:-2].decode("utf-8", "replace")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=25575)
    parser.add_argument("--password-file", type=Path, required=True)
    parser.add_argument("--command", required=True)
    parser.add_argument("--seconds", type=float, default=2.0)
    parser.add_argument("--interval", type=float, default=0.25)
    args = parser.parse_args()
    if args.seconds <= 0 or args.interval <= 0:
        parser.error("--seconds and --interval must be positive")

    with socket.create_connection((args.host, args.port), timeout=3) as sock:
        sock.settimeout(3)
        send_packet(sock, 1, 3, args.password_file.read_text().strip())
        while True:
            request_id, packet_type, _ = receive_packet(sock)
            if packet_type == 2:
                if request_id != 1:
                    raise PermissionError("RCON authentication failed")
                break
        started = time.monotonic()
        next_at = started
        sequence = 10
        print("elapsed_ms\tresponse", flush=True)
        while time.monotonic() - started < args.seconds:
            now = time.monotonic()
            if now < next_at:
                time.sleep(next_at - now)
            request_id = sequence
            sequence += 1
            send_packet(sock, request_id, 2, args.command)
            while True:
                response_id, _, response = receive_packet(sock)
                if response_id == request_id:
                    print(f"{(time.monotonic() - started) * 1000:.0f}\t{response}", flush=True)
                    break
            next_at += args.interval


if __name__ == "__main__":
    main()
