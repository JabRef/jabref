package org.jabref.http.server.resources;

import java.util.Optional;

import org.jabref.logic.remote.server.ConnectorTokenManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("auth/pair")
public class PairingResource {

    @Inject
    ConnectorTokenManager tokenManager;

    @Inject
    Gson gson;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pair(String body) {
        if (tokenManager == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                           .entity("{\"error\":\"Token manager not available\"}")
                           .build();
        }

        String pin;
        try {
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json == null || !json.has("pin")) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity("{\"error\":\"Missing 'pin' field\"}")
                               .build();
            }
            pin = json.get("pin").getAsString();
        } catch (JsonSyntaxException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\":\"Invalid JSON\"}")
                           .build();
        }

        Optional<String> token = tokenManager.validatePinAndGenerateToken(pin);
        if (token.isPresent()) {
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("token", token.get());
            return Response.ok(gson.toJson(responseJson)).build();
        }

        return Response.status(Response.Status.FORBIDDEN)
                       .entity("{\"error\":\"Invalid or expired PIN\"}")
                       .build();
    }
}
