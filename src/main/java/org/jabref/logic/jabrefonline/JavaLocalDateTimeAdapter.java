package org.jabref.logic.jabrefonline;

import java.io.IOException;
import java.time.LocalDateTime;

import com.apollographql.apollo3.api.Adapter;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.json.JsonReader;
import com.apollographql.apollo3.api.json.JsonWriter;

public class JavaLocalDateTimeAdapter implements Adapter<LocalDateTime> {
  public static JavaLocalDateTimeAdapter INSTANCE = new JavaLocalDateTimeAdapter();

  @Override
  public LocalDateTime fromJson(JsonReader reader, CustomScalarAdapters adapters) throws IOException {
    return LocalDateTime.parse(reader.nextString().replace("Z", ""));
  }

  @Override
  public void toJson(JsonWriter writer, CustomScalarAdapters adapters, LocalDateTime value) throws IOException {
    writer.value(value.toString());
  }
}
