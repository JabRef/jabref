package org.jabref.http.dto;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

/// Serializes resource return values into `application/json` via Gson so resources can return strongly-typed POJOs.
/// Skips primitive types (`String`, `byte[]`, `InputStream`) so JAX-RS' built-in writers handle them.
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class GsonMessageBodyWriter implements MessageBodyWriter<Object> {

    @Inject
    private Gson gson;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return !String.class.isAssignableFrom(type)
                && !byte[].class.isAssignableFrom(type)
                && !java.io.InputStream.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(Object o,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException {
        try (Writer writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8)) {
            gson.toJson(o, genericType, writer);
        }
    }
}
