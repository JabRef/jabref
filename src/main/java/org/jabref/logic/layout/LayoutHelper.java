package org.jabref.logic.layout;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.file.Path;

import java.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;


import org.jabref.logic.journals.JournalAbbreviationRepository;

/**
 * Helper class to get a Layout object.
 *
 * <pre>
 * <code>
 * LayoutHelper helper = new LayoutHelper(...a reader...);
 * Layout layout = helper.getLayoutFromText();
 * </code>
 * </pre>
 */
public class LayoutHelper {
  
    public static Map<Integer, Boolean> branchCoverage = new HashMap<>();

  
    public static final int IS_LAYOUT_TEXT = 1;
    public static final int IS_SIMPLE_COMMAND = 2;
    public static final int IS_FIELD_START = 3;
    public static final int IS_FIELD_END = 4;
    public static final int IS_OPTION_FIELD = 5;
    public static final int IS_GROUP_START = 6;
    public static final int IS_GROUP_END = 7;
    public static final int IS_ENCODING_NAME = 8;
    public static final int IS_FILENAME = 9;
    public static final int IS_FILEPATH = 10;

    private static String currentGroup;

    private final PushbackReader in;
    private final List<StringInt> parsedEntries = new ArrayList<>();
    private final List<Path> fileDirForDatabase;
    private final LayoutFormatterPreferences preferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private boolean endOfFile;

    public LayoutHelper(Reader in,
                        List<Path> fileDirForDatabase,
                        LayoutFormatterPreferences preferences,
                        JournalAbbreviationRepository abbreviationRepository) {
        this.in = new PushbackReader(Objects.requireNonNull(in));
        this.preferences = Objects.requireNonNull(preferences);
        this.abbreviationRepository = abbreviationRepository;
        this.fileDirForDatabase = fileDirForDatabase;
    }

    public LayoutHelper(Reader in,
                        LayoutFormatterPreferences preferences,
                        JournalAbbreviationRepository abbreviationRepository) {
        this(in, Collections.emptyList(), preferences, abbreviationRepository);
    }

    public Layout getLayoutFromText() throws IOException {
        parse();

        for (StringInt parsedEntry : parsedEntries) {
            if ((parsedEntry.i == LayoutHelper.IS_SIMPLE_COMMAND) || (parsedEntry.i == LayoutHelper.IS_FIELD_START)
                    || (parsedEntry.i == LayoutHelper.IS_FIELD_END) || (parsedEntry.i == LayoutHelper.IS_GROUP_START)
                    || (parsedEntry.i == LayoutHelper.IS_GROUP_END)) {
                parsedEntry.s = parsedEntry.s.trim().toLowerCase(Locale.ROOT);
            }
        }

        return new Layout(parsedEntries, fileDirForDatabase, preferences, abbreviationRepository);
    }

    public static String getCurrentGroup() {
        return LayoutHelper.currentGroup;
    }

    public static void setCurrentGroup(String newGroup) {
        LayoutHelper.currentGroup = newGroup;
    }

    private void doBracketedField(final int field) throws IOException {
        StringBuilder buffer = null;
        int currentCharacter;
        boolean start = false;

        while (!endOfFile) {
            currentCharacter = read();

            if (currentCharacter == -1) {
                endOfFile = true;
                if (buffer != null) {
                    parsedEntries.add(new StringInt(buffer.toString(), field));
                }
                return;
            }

            if ((currentCharacter == '{') || (currentCharacter == '}')) {
                if (currentCharacter == '}') {
                    if (buffer != null) {
                        parsedEntries.add(new StringInt(buffer.toString(), field));
                        return;
                    }
                } else {
                    start = true;
                }
            } else {
                if (buffer == null) {
                    buffer = new StringBuilder(100);
                }

                if (start && (currentCharacter != '}')) {
                    buffer.append((char) currentCharacter);
                }
            }
        }
    }

    private void doBracketedOptionField() throws IOException {
        StringBuilder buffer = null;
        int c;
        boolean start = false;
        boolean inQuotes = false;
        boolean doneWithOptions = false;
        String option = null;
        String tmp;

        while (!endOfFile) {
            branchCoverage.put(1, true);
            c = read();

            if (c == -1) {
                branchCoverage.put(1, true);
                endOfFile = true;

                if (buffer != null) {
                    branchCoverage.put(2, true);
                    if (option == null) {
                        branchCoverage.put(3, true);
                        tmp = buffer.toString();
                    } else {
                        branchCoverage.put(4, true);
                        tmp = buffer.toString() + '\n' + option;
                    }

                    parsedEntries.add(new StringInt(tmp, LayoutHelper.IS_OPTION_FIELD));
                }

                return;
            }
            if (!inQuotes && ((c == ']') || (c == '[') || (doneWithOptions && ((c == '{') || (c == '}'))))) {
                branchCoverage.put(5, true);
                branchCoverage.put(6, true);
                branchCoverage.put(7, true);
                branchCoverage.put(8, true);
                branchCoverage.put(9, true);
                branchCoverage.put(10, true);
                if ((c == ']') || (doneWithOptions && (c == '}'))) {
                    branchCoverage.put(11, true);
                    branchCoverage.put(12, true);
                    branchCoverage.put(13, true);
                    // changed section start - arudert
                    // buffer may be null for parameters
                    if ((c == ']') && (buffer != null)) {
                        branchCoverage.put(14, true);
                        branchCoverage.put(15, true);
                        // changed section end - arudert
                        option = buffer.toString();
                        buffer = null;
                        start = false;
                        doneWithOptions = true;
                    } else if (c == '}') {
                        branchCoverage.put(16, true);
                        // changed section begin - arudert
                        // bracketed option must be followed by an (optionally empty) parameter
                        // if empty, the parameter is set to " " (whitespace to avoid that the tokenizer that
                        // splits the string later on ignores the empty parameter)
                        String parameter = buffer == null ? " " : buffer.toString();
                        if (option == null) {
                            branchCoverage.put(17, true);
                            tmp = parameter;
                        } else {
                            branchCoverage.put(18, true);
                            tmp = parameter + '\n' + option;
                        }

                        parsedEntries.add(new StringInt(tmp, LayoutHelper.IS_OPTION_FIELD));

                        return;
                    }
                    // changed section end - arudert
                    // changed section start - arudert
                    // }
                    // changed section end - arudert
                } else {
                    branchCoverage.put(19, true);
                    start = true;
                }
            } else if (c == '"') {
                branchCoverage.put(20, true);
                inQuotes = !inQuotes;

                if (buffer == null) {
                    branchCoverage.put(21, true);
                    buffer = new StringBuilder(100);
                }
                buffer.append('"');
            } else {
                branchCoverage.put(22, true);
                if (buffer == null) {
                    branchCoverage.put(23, true);
                    buffer = new StringBuilder(100);
                }

                if (start) {
                    branchCoverage.put(24, true);
                    // changed section begin - arudert
                    // keep the backslash so we know wether this is a fieldname or an ordinary parameter
                    // if (c != '\\') {
                    buffer.append((char) c);
                    // }
                    // changed section end - arudert
                }
            }
        }
    }

    private void parse() throws IOException {
        skipWhitespace();

        int c;

        StringBuilder buffer = null;
        boolean escaped = false;

        while (!endOfFile) {
            c = read();

            if (c == -1) {
                endOfFile = true;

                // Check for null, otherwise a Layout that finishes with a curly brace throws a NPE
                if (buffer != null) {
                    parsedEntries.add(new StringInt(buffer.toString(), LayoutHelper.IS_LAYOUT_TEXT));
                }

                return;
            }

            if ((c == '\\') && (peek() != '\\') && !escaped) {
                if (buffer != null) {
                    parsedEntries.add(new StringInt(buffer.toString(), LayoutHelper.IS_LAYOUT_TEXT));

                    buffer = null;
                }

                parseField();

                // To make sure the next character, if it is a backslash,
                // doesn't get ignored, since "previous" now holds a backslash:
                escaped = false;
            } else {
                if (buffer == null) {
                    buffer = new StringBuilder(100);
                }

                if ((c != '\\') || escaped) /* (previous == '\\'))) */ {
                    buffer.append((char) c);
                }

                escaped = (c == '\\') && !escaped;
            }
        }
    }

    private void parseField() throws IOException {
        int c;
        StringBuilder buffer = null;
        String name;

        while (!endOfFile) {
            branchCoverage.put(1, true);
            c = read();
            if (c == -1) {
                branchCoverage.put(2, true);
                endOfFile = true;
            }

            if (!Character.isLetter((char) c) && (c != '_')) {
                unread(c);
                branchCoverage.put(3, true);

                name = buffer == null ? "" : buffer.toString();

                if (name.isEmpty()) {
                    branchCoverage.put(4, true);
                    StringBuilder lastFive = new StringBuilder(10);
                    if (parsedEntries.isEmpty()) {
                        branchCoverage.put(5, true);
                        lastFive.append("unknown");
                    } else {
                        branchCoverage.put(6, true);
                        for (StringInt entry : parsedEntries.subList(Math.max(0, parsedEntries.size() - 6),
                                parsedEntries.size() - 1)) {
                            branchCoverage.put(7, true);
                            lastFive.append(entry.s);
                        }
                    }
                    throw new IOException(
                            "Backslash parsing error near \'" + lastFive.toString().replace("\n", " ") + '\'');
                }

                if ("begin".equalsIgnoreCase(name)) {
                    // get field name
                    branchCoverage.put(8, true);
                    doBracketedField(LayoutHelper.IS_FIELD_START);

                    return;
                } else if ("begingroup".equalsIgnoreCase(name)) {
                    // get field name
                    branchCoverage.put(9, true);
                    doBracketedField(LayoutHelper.IS_GROUP_START);
                    return;
                } else if ("format".equalsIgnoreCase(name)) {
                    branchCoverage.put(10, true);
                    if (c == '[') {
                        // get format parameter
                        // get field name
                        branchCoverage.put(11, true);
                        doBracketedOptionField();

                        return;
                    } else {
                        // get field name
                        branchCoverage.put(12, true);
                        doBracketedField(LayoutHelper.IS_OPTION_FIELD);

                        return;
                    }
                } else if ("filename".equalsIgnoreCase(name)) {
                    // Print the name of the database BIB file.
                    // This is only supported in begin/end layouts, not in
                    // entry layouts.
                    branchCoverage.put(13, true);
                    parsedEntries.add(new StringInt(name, LayoutHelper.IS_FILENAME));
                    return;
                } else if ("filepath".equalsIgnoreCase(name)) {
                    // Print the full path of the database BIB file.
                    // This is only supported in begin/end layouts, not in
                    // entry layouts.
                    branchCoverage.put(14, true);
                    parsedEntries.add(new StringInt(name, LayoutHelper.IS_FILEPATH));
                    return;
                } else if ("end".equalsIgnoreCase(name)) {
                    // get field name
                    branchCoverage.put(15, true);
                    doBracketedField(LayoutHelper.IS_FIELD_END);
                    return;
                } else if ("endgroup".equalsIgnoreCase(name)) {
                    // get field name
                    branchCoverage.put(16, true);
                    doBracketedField(LayoutHelper.IS_GROUP_END);
                    return;
                } else if ("encoding".equalsIgnoreCase(name)) {
                    // Print the name of the current encoding used for export.
                    // This is only supported in begin/end layouts, not in
                    // entry layouts.
                    branchCoverage.put(17, true);
                    parsedEntries.add(new StringInt(name, LayoutHelper.IS_ENCODING_NAME));
                    return;
                }

                // for all other cases -> simple command
                parsedEntries.add(new StringInt(name, LayoutHelper.IS_SIMPLE_COMMAND));

                return;
            } else {
                branchCoverage.put(18, true);
                if (buffer == null) {
                    branchCoverage.put(19, true);
                    buffer = new StringBuilder(100);
                }
                buffer.append((char) c);
            }
        }
    }

    private int peek() throws IOException {
        int c = read();
        unread(c);

        return c;
    }

    private int read() throws IOException {
        return in.read();
    }

    private void skipWhitespace() throws IOException {
        int c;

        while (true) {
            c = read();

            if ((c == -1) || (c == 65535)) {
                endOfFile = true;

                return;
            }

            if (!Character.isWhitespace((char) c)) {
                unread(c);
                break;
            }
        }
    }

    private void unread(int c) throws IOException {
        in.unread(c);
    }
}
