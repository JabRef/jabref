package org.jabref.http.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

/// Reads `application/json` request bodies into POJOs via Gson so resources can declare strongly-typed parameters.
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class GsonMessageBodyReader implements MessageBodyReader<Object> {

    @Inject
    private Gson gson;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> type,
                           Type genericType,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders,
                           InputStream entityStream) throws IOException {
        try (Reader reader = new InputStreamReader(entityStream, StandardCharsets.UTF_8)) {
            Object result = gson.fromJson(reader, genericType);
            if (result == null) {
                throw new BadRequestException("Request body must not be empty");
            }
            return result;
        } catch (JsonSyntaxException e) {
            throw new BadRequestException("Malformed JSON request body", e);
        }
    }
}
