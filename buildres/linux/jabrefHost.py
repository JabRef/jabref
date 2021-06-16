#!/usr/bin/env python3

import json
import logging
import os
import platform
import shlex
import shutil
import struct
import subprocess
import sys
from pathlib import Path

# Try a set of possible launchers to execute JabRef
script_dir = Path(__file__).resolve().parent.parent
relpath_path = script_dir / "bin/JabRef"
lowercase_path = shutil.which("jabref")
uppercase_path = shutil.which("JabRef")

# Relative path used in the portable install
if relpath_path.exists():
    JABREF_PATH = relpath_path
# Lowercase launcher used in deb/rpm/snap packages
elif lowercase_path is not None and os.path.exists(lowercase_path):
    JABREF_PATH = Path(lowercase_path)
# Uppercase launcher used in Arch AUR package
elif uppercase_path is not None and os.path.exists(uppercase_path):
    JABREF_PATH = Path(uppercase_path)
# FLatpak support
elif subprocess.run(["flatpak", "info", "org.jabref.jabref"], capture_output=True).returncode == 0:
    JABREF_PATH = "flatpak run org.jabref.jabref"
else:
    logging.error("Could not determine JABREF_PATH")
    sys.exit(-1)

logging_dir = Path.home() / ".mozilla/native-messaging-hosts/"
if not logging_dir.exists():
    logging_dir.mkdir(parents=True)
logging.basicConfig(filename=str(logging_dir / "jabref_browser_extension.log"))

# Read a message from stdin and decode it.
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


def add_jabref_entry(data):
    """Send string via cli as literal to preserve special characters"""
    cmd = str(JABREF_PATH).split() + ["--importBibtex", r"{}".format(data)]
    logging.info("Try to execute command {}".format(cmd))
    try:
        response = subprocess.check_output(cmd, stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as exc:
        logging.error("Failed to call JabRef: {} {}".format(exc.returncode, exc.output))
    else:
        logging.info("Called JabRef and got: {}".format(response))
    return response


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
        send_message({"message": "jarNotFound", "path": JABREF_PATH})
    else:
        logging.info("Response: {}".format(response))
        send_message({"message": "jarFound"})
else:
    entry = message["text"]
    output = add_jabref_entry(entry)
    send_message({"message": "ok", "output": str(output)})
