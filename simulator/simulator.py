#!/usr/bin/env python3
"""Simulate five devices sending heartbeats to Mini Device Fleet Monitor.

Commands while running:
  stop device-01   Stop heartbeats for a device
  start device-01  Resume heartbeats for a device
  status           Show simulator state
  help             Show commands
  quit             Stop the simulator
"""

import argparse
import json
import threading
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

DEVICES = {
    "device-01": "Lab Device 01",
    "device-02": "Lab Device 02",
    "device-03": "Lab Device 03",
    "device-04": "Lab Device 04",
    "device-05": "Lab Device 05",
}
HEARTBEAT_INTERVAL_SECONDS = 5
REQUEST_TIMEOUT_SECONDS = 5


class FleetSimulator:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.active = {device_id: True for device_id in DEVICES}
        self.lock = threading.Lock()
        self.stop_event = threading.Event()
        self.threads = []

    def request(self, method, path, payload=None):
        body = None if payload is None else json.dumps(payload).encode("utf-8")
        headers = {"Accept": "application/json"}
        if body is not None:
            headers["Content-Type"] = "application/json"

        request = Request(
            self.base_url + path,
            data=body,
            headers=headers,
            method=method,
        )

        try:
            with urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
                return response.status, response.read().decode("utf-8")
        except HTTPError as exc:
            response_body = exc.read().decode("utf-8", errors="replace")
            print(f"[HTTP {exc.code}] {method} {path}: {response_body}")
            return exc.code, response_body
        except (URLError, TimeoutError) as exc:
            print(f"[ERROR] {method} {path} failed: {exc}")
            return None, None

    def register_devices(self):
        print(f"Registering devices against {self.base_url} ...")
        for device_id, name in DEVICES.items():
            status, _ = self.request(
                "POST", "/devices", {"id": device_id, "name": name}
            )

            if status in (200, 201):
                print(f"[REGISTERED] {device_id}")
            elif status == 409:
                # Existing database records are fine when rerunning the simulator.
                print(f"[EXISTS] {device_id} is already registered; continuing")
            elif status is not None:
                print(f"[WARN] Registration for {device_id} returned HTTP {status}")

    def heartbeat_loop(self, device_id):
        counter = 0

        while not self.stop_event.is_set():
            with self.lock:
                should_send = self.active[device_id]

            if should_send:
                payload = {
                    "status": "OK",
                    "cpuUsage": float(20 + (counter * 7) % 75),
                    "signalStrength": 50 + (counter * 3) % 50,
                }

                status, _ = self.request(
                    "POST", f"/devices/{device_id}/heartbeat", payload
                )

                if status is not None and 200 <= status < 300:
                    print(f"[HEARTBEAT] {device_id} sent")

                counter += 1

            if self.stop_event.wait(HEARTBEAT_INTERVAL_SECONDS):
                break

    def start(self):
        self.register_devices()

        for device_id in DEVICES:
            thread = threading.Thread(
                target=self.heartbeat_loop,
                args=(device_id,),
                name=f"heartbeat-{device_id}",
                daemon=True,
            )
            thread.start()
            self.threads.append(thread)

        print("\nSimulator running. Type 'help' for commands.\n")

    def handle_command(self, line):
        parts = line.strip().split()
        if not parts:
            return True

        command = parts[0].lower()

        if command in ("quit", "exit", "q"):
            self.stop_event.set()
            return False

        if command == "help":
            print(
                "Commands:\n"
                "  stop <device-id>   Stop heartbeats for a device\n"
                "  start <device-id>  Resume heartbeats for a device\n"
                "  status             Show simulator state\n"
                "  help               Show commands\n"
                "  quit               Stop the simulator"
            )
            return True

        if command == "status":
            with self.lock:
                for device_id, name in DEVICES.items():
                    state = "SENDING" if self.active[device_id] else "STOPPED"
                    print(f"{device_id} ({name}): {state}")
            return True

        if command in ("stop", "start"):
            if len(parts) != 2 or parts[1] not in DEVICES:
                print(
                    f"Usage: {command} <device-id>; "
                    f"valid IDs: {', '.join(DEVICES)}"
                )
                return True

            device_id = parts[1]
            with self.lock:
                self.active[device_id] = command == "start"

            if command == "stop":
                print(f"[STOPPED] {device_id}; no more heartbeats will be sent.")
                print("Check its API status after more than 30 seconds.")
            else:
                print(f"[STARTED] {device_id}; heartbeats will resume.")

            return True

        print("Unknown command. Type 'help' for commands.")
        return True

    def run(self):
        self.start()

        try:
            while not self.stop_event.is_set():
                try:
                    line = input("simulator> ")
                except EOFError:
                    break

                if not self.handle_command(line):
                    break
        except KeyboardInterrupt:
            print("\nStopping simulator...")
        finally:
            self.stop_event.set()
            for thread in self.threads:
                thread.join(timeout=1)
            print("Simulator stopped.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Simulate five fleet devices.")
    parser.add_argument(
        "--base-url",
        default="http://localhost:8080",
        help="Application base URL (default: http://localhost:8080)",
    )
    args = parser.parse_args()
    FleetSimulator(args.base_url).run()
