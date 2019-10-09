#!/usr/bin/python -u

# Note that running python with the `-u` flag is required on Windows,
# in order to ensure that stdin and stdout are opened in binary, rather
# than text, mode.

import json
import sys
import struct
import subprocess
import shlex

# We assume that this python script is located in "jabref/lib" while the executable is "jabref/bin/JabRef"
script_dir = os.path.dirname(os.path.realpath('__file__'))
JABREF_PATH = os.path.join(script_dir, '../bin/JabRef')
INIT_LOGGER = False
LOG_FILE = "jabfox_backend_log.txt"
def logger(msg):
    global INIT_LOGGER, LOG_FILE
    if INIT_LOGGER:
        with open(LOG_FILE, "a") as f:
            f.write(msg)
    else:
        INIT_LOGGER = True
        with open(LOG_FILE, "w") as f:
            f.write(msg)
        

# Read a message from stdin and decode it.
def get_message():
    raw_length = sys.stdin.buffer.read(4)
    if not raw_length:
        logger("[ERROR] Raw_length \n")
        sys.exit(0)
    message_length = struct.unpack('=I', raw_length)[0]
    logger("[INFO] Got length: {} bytes to be read\n".format(message_length))
    message = sys.stdin.buffer.read(message_length).decode("utf-8")
    logger("[INFO] Got message of {} chars\n".format(len(message)))
    data = json.loads(message)
    logger("[INFO] Successfully retrieved JSON\n")
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
    cmd = JABREF_PATH + " -importBibtex " + "\"" + data + "\""
    try: 
        response = subprocess.check_output(shlex.split(cmd), stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as exc:
        logger("[ERROR] Failed to call JabRef", exc.returncode, exc.output)
    else:
        logger(str(response))
        logger("[INFO] Called JabRef")
    return response


logger("[INFO] Starting JabRef backend\n")

try:
    message = get_message()
except Exception as e:
    message = str(e)
logger(str(message) + "\n")

if 'status' in message and message["status"] == "validate":
    cmd = JABREF_PATH + " -version"
    try: 
        response = subprocess.check_output(shlex.split(cmd), stderr=subprocess.STDOUT, shell=True)
    except subprocess.CalledProcessError as exc:
        logger("[ERROR] Failed to call JabRef", exc.returncode, exc.output)
        send_message({"message": "jarNotFound", "path": JABREF_PATH})
    else:
        logger(str(response))
        send_message({"message": "jarFound"})
else:
    entry = message["text"]
    output = add_jabref_entry(entry)
    send_message({"message": "ok", "output": str(output)})
    
