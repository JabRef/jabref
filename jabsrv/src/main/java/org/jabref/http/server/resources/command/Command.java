package org.jabref.http.server.resources.command;

import org.jabref.http.SrvStateManager;

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

    default SrvStateManager getSrvStateManager() {
        return getServiceLocator().getService(SrvStateManager.class);
    }
}
