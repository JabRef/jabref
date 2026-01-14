# Technical Specification: HTTP Server Commands for CLI Operations

## Difficulty Assessment
**Medium** - Requires creating multiple command classes following existing patterns, understanding the interaction between HTTP server and UI command processing, and properly handling different execution contexts.

## Technical Context
- **Language**: Java 24+
- **Framework**: JAX-RS (Jakarta RESTful Web Services)
- **Dependencies**: 
  - Jackson for JSON serialization/deserialization
  - HK2 for dependency injection
  - Existing JabRef modules: jablib, jabgui, jabsrv

## Current Architecture

### Existing Components
1. **`CommandResource`** (`jabsrv/src/main/java/org/jabref/http/server/command/CommandResource.java`)
   - JAX-RS resource endpoint at `/commands`
   - Accepts POST requests with JSON payloads
   - Deserializes JSON to `Command` implementations using Jackson polymorphic type handling
   - Executes commands via `Command.execute()`

2. **`Command` Interface** (`jabsrv/src/main/java/org/jabref/http/server/command/Command.java`)
   - Uses Jackson `@JsonTypeInfo` and `@JsonSubTypes` for polymorphic deserialization
   - Provides access to `ServiceLocator` for dependency injection
   - Provides helper method `getSrvStateManager()` to access application state
   - Currently has one implementation: `SelectEntriesCommand`

3. **`UiCommand` Records** (`jablib/src/main/java/org/jabref/logic/UiCommand.java`)
   - Sealed interface with record implementations
   - Relevant records for this task:
     - `OpenLibraries(List<Path> toImport)`
     - `AppendBibTeXToCurrentLibrary(String bibtex)` (deprecated but still used)
     - `Focus()`
   - Note: `AppendToCurrentLibrary(List<Path> toAppend)` also exists but task specifies using `AppendBibTeXToCurrentLibrary`

4. **CLI Processing** (`jabgui/src/main/java/org/jabref/cli/ArgumentProcessor.java`)
   - Processes CLI arguments into `UiCommand` instances
   - Shows how the commands are created from command-line input

5. **UI Command Handling** (`jabgui/src/main/java/org/jabref/gui/frame/JabRefFrameViewModel.java`)
   - Implements `UiMessageHandler` interface
   - Method `handleUiCommands(List<UiCommand>)` processes UI commands
   - Handles all `UiCommand` types including:
     - `OpenLibraries`: Opens library files
     - `AppendBibTeXToCurrentLibrary`: Parses and imports BibTeX string
     - `Focus`: (implicit - handled by bringing window to front)

## Implementation Approach

### Strategy
Create three new `Command` implementations that mirror the CLI commands by:
1. Creating command classes that accept HTTP request data
2. Constructing appropriate `UiCommand` instances
3. Delegating execution to `UiMessageHandler` (which must be accessible via `ServiceLocator`)

### Challenge: Accessing UiMessageHandler
The `UiMessageHandler` is implemented by `JabRefFrameViewModel` in the `jabgui` module. The HTTP server commands in `jabsrv` need access to this handler. 

**Solution**: Register `UiMessageHandler` in the HK2 `ServiceLocator` so it can be retrieved by commands via `getServiceLocator().getService(UiMessageHandler.class)`.

### Commands to Implement

1. **`AppendBibTeXCommand`**
   - Maps to CLI `--importBibtex` option
   - Creates `UiCommand.AppendBibTeXToCurrentLibrary(String bibtex)`
   - JSON payload: `{"commandId": "appendbibtex", "bibtex": "..."}`

2. **`OpenLibrariesCommand`**
   - Maps to CLI file arguments (when not in append mode)
   - Creates `UiCommand.OpenLibraries(List<Path> toImport)`
   - JSON payload: `{"commandId": "open", "paths": ["path1", "path2"]}`

3. **`FocusCommand`**
   - Maps to focus functionality
   - Creates `UiCommand.Focus()`
   - JSON payload: `{"commandId": "focus"}`

## Source Code Structure Changes

### Files to Create
1. `jabsrv/src/main/java/org/jabref/http/server/command/AppendBibTeXCommand.java`
2. `jabsrv/src/main/java/org/jabref/http/server/command/OpenLibrariesCommand.java`
3. `jabsrv/src/main/java/org/jabref/http/server/command/FocusCommand.java`

### Files to Modify
1. **`jabsrv/src/main/java/org/jabref/http/server/command/Command.java`**
   - Add new command classes to `@JsonSubTypes` annotation
   - Register: `AppendBibTeXCommand`, `OpenLibrariesCommand`, `FocusCommand`

2. **Registration of UiMessageHandler in ServiceLocator**
   - Need to find where HK2 ServiceLocator is configured for jabsrv
   - Ensure `UiMessageHandler` is bound in the service locator when running in GUI mode

### Implementation Pattern
Each command will follow this pattern (using `AppendBibTeXCommand` as example):

```java
@JsonTypeName("appendbibtex")
public class AppendBibTeXCommand implements Command {
    
    @JsonIgnore
    private ServiceLocator serviceLocator;
    
    @JsonProperty
    private String bibtex;
    
    public AppendBibTeXCommand() {
    }
    
    @Override
    public Response execute() {
        UiMessageHandler handler = getServiceLocator().getService(UiMessageHandler.class);
        if (handler == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("UiMessageHandler not available.")
                          .build();
        }
        
        UiCommand command = new UiCommand.AppendBibTeXToCurrentLibrary(bibtex);
        handler.handleUiCommands(List.of(command));
        
        return Response.ok().build();
    }
    
    @Override
    public void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }
    
    @Override
    public ServiceLocator getServiceLocator() {
        return this.serviceLocator;
    }
    
    // Getters and setters for JSON properties
}
```

## Data Model / API Changes

### JSON API Contract

#### Append BibTeX
```json
{
  "commandId": "appendbibtex",
  "bibtex": "@article{key2024,\n  title={Example},\n  author={Author}\n}"
}
```

#### Open Libraries
```json
{
  "commandId": "open",
  "paths": ["/path/to/library1.bib", "/path/to/library2.bib"]
}
```

#### Focus
```json
{
  "commandId": "focus"
}
```

### Response Format
- **Success**: HTTP 200 OK
- **Error**: HTTP 500 or 400 with error message in body

## Verification Approach

### Unit Tests
Create test class: `jabsrv/src/test/java/org/jabref/http/server/command/CommandsTest.java`
- Test JSON deserialization for each command type
- Test command execution with mocked `UiMessageHandler`
- Verify correct `UiCommand` instances are created and passed to handler

### Integration Tests
- Test the full HTTP flow: POST to `/commands` endpoint with JSON payload
- Verify commands are dispatched correctly
- Test error cases (missing handler, invalid JSON)

### Manual Verification
1. Start JabRef with HTTP server enabled
2. Send HTTP POST requests to test each command:
   ```bash
   # Append BibTeX
   curl -X POST http://localhost:PORT/commands \
     -H "Content-Type: application/json" \
     -d '{"commandId":"appendbibtex","bibtex":"@article{test,title={Test}}"}'
   
   # Open library
   curl -X POST http://localhost:PORT/commands \
     -H "Content-Type: application/json" \
     -d '{"commandId":"open","paths":["/path/to/library.bib"]}'
   
   # Focus
   curl -X POST http://localhost:PORT/commands \
     -H "Content-Type: application/json" \
     -d '{"commandId":"focus"}'
   ```

### Linting and Build
Run JabRef's standard verification:
```bash
./gradlew :jabsrv:check
./gradlew :jabsrv:test
./gradlew checkstyleMain checkstyleTest
```

## Dependencies and Module Boundaries

### Module Dependencies
- **jabsrv** (HTTP server module):
  - Depends on **jablib** (for `UiCommand` definitions)
  - Needs access to **jabgui** types (`UiMessageHandler`) but only via interface
  
### Dependency Injection Consideration
- `UiMessageHandler` is defined in `jabgui` module
- Must be registered in HK2 ServiceLocator when jabsrv runs with GUI
- Commands should gracefully handle case where handler is not available (e.g., in CLI-only mode)

## Edge Cases and Error Handling

1. **No active library** (for append command)
   - `UiMessageHandler` will handle this - it checks for active tab
   - Command just delegates to handler

2. **Invalid file paths** (for open command)
   - `UiMessageHandler` handles file validation
   - Command validates that paths are provided

3. **UiMessageHandler not available**
   - Return HTTP 500 with appropriate error message
   - Occurs when running in headless/CLI mode

4. **Invalid JSON**
   - Already handled by `CommandResource` - returns HTTP 500 with Jackson error

## Open Questions
1. **Where is HK2 ServiceLocator configured for jabsrv?**
   - Need to find the binding configuration to ensure `UiMessageHandler` can be injected
   - May need to create or modify a binder class

2. **Should commands execute asynchronously?**
   - Looking at CLI handler, it uses `Platform.runLater()` for JavaFX thread
   - HTTP commands may need similar approach if they're not already on the correct thread

3. **Thread safety considerations?**
   - HTTP requests come on different threads
   - JavaFX operations must be on FX Application Thread
   - May need to wrap handler calls with `Platform.runLater()`
