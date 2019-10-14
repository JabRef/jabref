#!/usr/bin/python3 -u

# Note that running python with the `-u` flag is required on Windows,
# in order to ensure that stdin and stdout are opened in binary, rather
# than text, mode.

import json
import sys
import struct
import subprocess
import shlex
import logging
import shutil
from pathlib import Path

# We assume that this python script is located in "jabref/lib" while the executable is "jabref/bin/JabRef"
script_dir = Path(__file__).resolve().parent.parent
JABREF_PATH = script_dir / "bin/JabRef"
if not JABREF_PATH.exists():
    JABREF_PATH = shutil.which("jabref")

logging_dir = Path.home() / ".mozilla/native-messaging-hosts/"
if not logging_dir.exists():
    logging_dir.mkdir(parents=True)
logging.basicConfig(filename=logging_dir / 'jabref_browser_extension.log')

# Read a message from stdin and decode it.
def get_message():
    raw_length = sys.stdin.buffer.read(4)
    if not raw_length:
        logging.error("Raw_length \n")
        sys.exit(0)
    message_length = struct.unpack('=I', raw_length)[0]
    logging.info("Got length: {} bytes to be read\n".format(message_length))
    message = sys.stdin.buffer.read(message_length).decode("utf-8")
    logging.info("Got message of {} chars\n".format(len(message)))
    data = json.loads(message)
    logging.info("Successfully retrieved JSON\n")
    return data


# Encode a message for transmission, given its content.
def encode_message(message_content):
    encoded_content = json.dumps(message_content).encode("utf-8")
    encoded_length = struct.pack('=I', len(encoded_content))
    return {'length': encoded_length, 'content': struct.pack(str(len(encoded_content))+"s",encoded_content)}


# Send an encoded message to stdout.
def send_message(message):
    encoded_message = encode_message(message)
    sys.stdout.buffer.write(encoded_message['length'])
    sys.stdout.buffer.write(encoded_message['content'])
    sys.stdout.buffer.flush()

def add_jabref_entry(data):
    cmd = str(JABREF_PATH) + " -importBibtex " + "\"" + data + "\""
    try: 
        response = subprocess.check_output(shlex.split(cmd), stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as exc:
        logging.error("Failed to call JabRef: %s %s", exc.returncode, exc.output)
    else:
        logging.info(f'Called JabRef and got: {response}')
    return response


logging.info("Starting JabRef backend")

try:
    message = get_message()
except Exception as e:
    message = str(e)
logging.info(str(message))

if 'status' in message and message["status"] == "validate":
    cmd = str(JABREF_PATH) + " -version"
    try: 
        response = subprocess.check_output(shlex.split(cmd), stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as exc:
        logging.error("Failed to call JabRef: %s %s", exc.returncode, exc.output)
        send_message({"message": "jarNotFound", "path": JABREF_PATH})
    else:
        logging.info(f'{response}')
        send_message({"message": "jarFound"})
else:
    entry = message["text"]
    output = add_jabref_entry(entry)
    send_message({"message": "ok", "output": str(output)})
    
