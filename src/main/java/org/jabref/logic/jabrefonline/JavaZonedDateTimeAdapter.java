package org.jabref.logic.jabrefonline;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.apollographql.apollo3.api.Adapter;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.json.JsonReader;
import com.apollographql.apollo3.api.json.JsonWriter;

public class JavaZonedDateTimeAdapter implements Adapter<ZonedDateTime> {
  public static JavaZonedDateTimeAdapter INSTANCE = new JavaZonedDateTimeAdapter();

  @Override
  public ZonedDateTime fromJson(JsonReader reader, CustomScalarAdapters adapters) throws IOException {
    return ZonedDateTime.parse(reader.nextString());
  }

  @Override
  public void toJson(JsonWriter writer, CustomScalarAdapters adapters, ZonedDateTime value) throws IOException {
    writer.value(value.toString());
  }
}
