package org.jabref.benchmarks;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;



@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2)       // default is 5
@Measurement(iterations = 3)  // default is 5
@Fork(1)                      // default is 2
public class UpdateEntryLinksBenchmark {

    @Param({"1000", "10000"})
    public int entryCount;


    private BibDatabase database;
    String newKey = "newKey";
    String oldKey = "oldKey";

    @Setup(Level.Invocation)
    public void setup() {
        database = new BibDatabase();
        for (int i = 0; i < entryCount; i++) {
            BibEntry entry = new BibEntry();
            entry.setCitationKey("id" + i);
            entry.setField(StandardField.TITLE, "Title " + i);
            database.insertEntry(entry);
        }
    }

    @Benchmark
    public void updateEntryLinks_onePass() {
        for (BibEntry entry :  database.getEntries()) {
            Set<Field> fields = entry.getFields(); // all fields once

            for (Field field : fields) {
                EnumSet<FieldProperty> props = field.getProperties();
                String content = entry.getField(field).orElseThrow();

                if (props.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
                    replaceSingleKeyInField(newKey, oldKey, entry, field, content);
                } else if (props.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
                    replaceKeyInMultiplesKeyField(newKey, oldKey, entry, field, content);
                }
            }
        }
    }

    @Benchmark
    public void updateEntryLinks_twoPass() {
        for (BibEntry entry :  database.getEntries()) {
            entry.getFields(f -> f.getProperties().contains(FieldProperty.SINGLE_ENTRY_LINK))
                 .forEach(field -> {
                     String content = entry.getField(field).orElseThrow();
                     replaceSingleKeyInField(newKey, oldKey, entry, field, content);
                 });

            entry.getFields(f -> f.getProperties().contains(FieldProperty.MULTIPLE_ENTRY_LINK))
                 .forEach(field -> {
                     String content = entry.getField(field).orElseThrow();
                     replaceKeyInMultiplesKeyField(newKey, oldKey, entry, field, content);
                 });
        }
    }

    private void replaceSingleKeyInField(String newKey, String oldKey, BibEntry entry, Field field, String content) {
        content.replace(oldKey, newKey);
    }

    private void replaceKeyInMultiplesKeyField(String newKey, String oldKey, BibEntry entry, Field field, String content) {
        content.replaceAll("\\b" + oldKey + "\\b", newKey);
    }
}
