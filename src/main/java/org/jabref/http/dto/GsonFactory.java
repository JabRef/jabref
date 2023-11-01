package org.jabref.http.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.glassfish.hk2.api.Factory;

public class GsonFactory implements Factory<Gson> {
    @Override
    public Gson provide() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    @Override
    public void dispose(Gson instance) {
    }
}
