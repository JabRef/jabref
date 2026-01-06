# WebSocket Server for Browser Extensions

JabRef includes a WebSocket server that allows browser extensions and external applications to communicate with JabRef in real-time.

## Configuration

The WebSocket server is **disabled by default** for security reasons. To enable it:

1. Go to **Edit → Preferences → General**
2. Find the **WebSocket Server** section  
3. Check **Enable WebSocket Server on port**
4. Set the port (default: 23116)
5. Click **OK**

The server binds only to `localhost` (127.0.0.1) for security.

## Protocol

The server implements RFC 6455 WebSocket protocol and accepts JSON-formatted text messages.

### Message Format

```json
{
  "command": "commandName",
  "argument": "optional argument"
}
```

### Response Format

```json
{
  "status": "success|error",
  "response": "response data",
  "message": "error message if applicable"
}
```

## Available Commands

### 1. ping
Tests the connection.

**Request:**
```json
{"command": "ping"}
```

**Response:**
```json
{"status": "success", "response": "pong"}
```

### 2. focus
Brings the JabRef window to the front.

**Request:**
```json
{"command": "focus"}
```

**Response:**
```json
{"status": "success", "response": "focused"}
```

### 3. open
Opens a BibTeX file or imports references.

**Request:**
```json
{
  "command": "open",
  "argument": "/path/to/file.bib"
}
```

**Response:**
```json
{"status": "success", "response": "opened"}
```

### 4. add
Adds a BibTeX entry to the currently open library.

**Request:**
```json
{
  "command": "add",
  "argument": "@article{key2024, author={Author}, title={Title}, year={2024}}"
}
```

**Response:**
```json
{"status": "success", "response": "added"}
```

## Example Client (JavaScript/Node.js)

```javascript
const WebSocket = require('ws');

const ws = new WebSocket('ws://localhost:23116');

ws.on('open', function open() {
  console.log('Connected to JabRef');
  
  // Test ping
  ws.send(JSON.stringify({command: 'ping'}));
});

ws.on('message', function message(data) {
  console.log('Received:', data.toString());
  
  // Add a BibTeX entry
  const bibEntry = '@article{example2024,author={John Doe},title={Example Article},year={2024}}';
  ws.send(JSON.stringify({
    command: 'add',
    argument: bibEntry
  }));
});

ws.on('error', function error(err) {
  console.error('WebSocket error:', err);
});
```

## Example Client (Python)

```python
import websockets
import asyncio
import json

async def connect_to_jabref():
    uri = "ws://localhost:23116"
    async with websockets.connect(uri) as websocket:
        # Test ping
        await websocket.send(json.dumps({"command": "ping"}))
        response = await websocket.recv()
        print(f"Received: {response}")
        
        # Add a BibTeX entry
        bib_entry = "@article{example2024,author={John Doe},title={Example},year={2024}}"
        await websocket.send(json.dumps({
            "command": "add",
            "argument": bib_entry
        }))
        response = await websocket.recv()
        print(f"Received: {response}")

asyncio.run(connect_to_jabref())
```

## Browser Extension Example

```javascript
// In your browser extension
const ws = new WebSocket('ws://localhost:23116');

ws.onopen = () => {
  console.log('Connected to JabRef');
};

ws.onmessage = (event) => {
  const response = JSON.parse(event.data);
  console.log('Response:', response);
};

// Function to send a BibTeX entry to JabRef
function sendToJabRef(bibTexEntry) {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      command: 'add',
      argument: bibTexEntry
    }));
  }
}

// Example: Extract citation from current page and send to JabRef
const bibEntry = extractCitationFromPage(); // Your extraction logic
sendToJabRef(bibEntry);
```

## Security Considerations

- The WebSocket server is **disabled by default**
- Binds only to `localhost` (not accessible from network)
- No authentication (relies on localhost security)
- Only accepts connections from the same machine
- Should only be enabled when needed

## Troubleshooting

### Connection Refused
- Ensure the WebSocket server is enabled in preferences
- Check that JabRef is running
- Verify the correct port number (default: 23116)
- Check firewall settings (should allow localhost connections)

### Commands Not Working
- Ensure a library is open in JabRef for `add` command
- Check JSON formatting (must be valid JSON)
- Verify command names are spelled correctly (case-sensitive)

## Implementation Details

The WebSocket server:
- Implements RFC 6455 WebSocket protocol
- Uses pure Java (no external WebSocket libraries)
- Runs in a separate thread
- Reuses existing `RemoteMessageHandler` infrastructure
- Supports text frames only (binary frames not supported)
