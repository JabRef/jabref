package org.jabref.http.server.command;

import org.jabref.http.server.services.GuiBridge;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceLocator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "commandId")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SelectEntriesCommand.class, name = "selectentries")
})
public interface Command {

    default Response execute() {
        return Response.serverError().build();
    }

    void setServiceLocator(ServiceLocator serviceLocator);

    ServiceLocator getServiceLocator();

    default GuiBridge getGuiBridge() {
        return getServiceLocator().getService(GuiBridge.class);
    }
}
