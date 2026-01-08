#!/usr/bin/python3

import json
import logging
import platform
import shlex
import shutil
import struct
import subprocess
import sys
from pathlib import Path

# Read a message fro/m stdin and decode it.
def get_message():
    raw_length = sys.stdin.buffer.read(4)
    if not raw_length:
        logging.error("Raw_length null")
        sys.exit(0)
    message_length = struct.unpack("=I", raw_length)[0]
    logging.info("Got length: {} bytes to be read".format(message_length))
    message = sys.stdin.buffer.read(message_length).decode("utf-8")
    logging.info("Got message of {} chars".format(len(message)))
    data = json.loads(message)
    logging.info("Successfully retrieved JSON")
    return data


# Encode a message for transmission, given its content.
def encode_message(message_content):
    encoded_content = json.dumps(message_content).encode("utf-8")
    encoded_length = struct.pack("=I", len(encoded_content))
    return {
        "length": encoded_length,
        "content": struct.pack(str(len(encoded_content)) + "s", encoded_content),
    }


# Send an encoded message to stdout.
def send_message(message):
    encoded_message = encode_message(message)
    sys.stdout.buffer.write(encoded_message["length"])
    sys.stdout.buffer.write(encoded_message["content"])
    sys.stdout.buffer.flush()


def add_jabref_entry(jabref_path, data):
    """Send string via cli as literal to preserve special characters"""
    cmd = str(jabref_path).split() + ["--importBibtex", r"{}".format(data)]
    logging.info("Try to execute command {}".format(cmd))
    response = subprocess.check_output(cmd, stderr=subprocess.STDOUT)
    logging.info("Called JabRef and got: {}".format(response))
    return response

# --- Main Logic ---

# We assume that this python script is located in "jabref/lib" while the executable is "jabref/bin/JabRef"
# Note that the package structure is different when installed as a .app bundle on MacOs, so the path must be altered.

# Current script: jabgui/buildres/macos/Resources/jabrefHost.py
current_script = Path(__file__).resolve()

# 1. Check for Development Build (Gradle installDist)
# Path: jabgui/build/install/jabgui/bin/JabRef
# From current: ../../../../build/install/jabgui/bin/JabRef
dev_build_path = current_script.parent.parent.parent.parent / "build/install/jabgui/bin/JabRef"

if dev_build_path.exists():
    JABREF_PATH = dev_build_path
else:
    # Fallback to standard app bundle structure
    # Standard Mac App: JabRef.app/Contents/Resources/jabrefHost.py
    # Executable: JabRef.app/Contents/MacOS/JabRef
    JABREF_PATH = current_script.parent.parent / "MacOS/JabRef"



logging_dir = Path.home() / ".mozilla/native-messaging-hosts/"
if not logging_dir.exists():
    logging_dir.mkdir(parents=True)
logging.basicConfig(filename=str(logging_dir / "jabref_browser_extension.log"), level=logging.INFO)

if not JABREF_PATH.exists():
    logging.error("Could not determine JABREF_PATH")
    # We can now safely call send_message because it is defined above
    send_message({"message": "error", "output": "Could not find JabRef. Please check that it is installed correctly."})
    sys.exit(-1)

logging.info("Starting JabRef backend")

try:
    message = get_message()
except Exception as e:
    message = str(e)
logging.info(str(message))

if "status" in message and message["status"] == "validate":
    cmd = str(JABREF_PATH).split() + ["--version"]
    try:
        response = subprocess.check_output(cmd, stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as exc:
        logging.error("Failed to call JabRef: {} {}".format(exc.returncode, exc.output))
        send_message({"message": "jarNotFound", "path": str(JABREF_PATH)})
    else:
        logging.info("Response: {}".format(response))
        send_message({"message": "jarFound"})
else:
    entry = message["text"]
    try:
        output = add_jabref_entry(JABREF_PATH, entry)
        send_message({"message": "ok", "output": str(output)})
    except subprocess.CalledProcessError as exc:
        logging.error("Failed to call JabRef: {} {}".format(exc.returncode, exc.output))
        send_message({"message": "error", "output": str(exc.output)})
