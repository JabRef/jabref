package org.jabref.http.server.command;

import org.jabref.logic.UiCommand;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.ws.rs.core.Response;

@JsonTypeName("focus")
public class FocusCommand extends Command {

    public FocusCommand() {
    }

    @Override
    public Response execute() {
        return execute(new UiCommand.Focus());
    }
}
