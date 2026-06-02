package org.jabref.logic.msc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class MscCodeRepository {
    private static final String MSC_CODES_MAP_NAME = "MscCodes";

    private final Map<String, MscCodeEntry> mscCodes = new HashMap<>();

    public MscCodeRepository() {
    }

    public MscCodeRepository(Collection<MscCodeEntry> entries) {
        entries.forEach(entry -> mscCodes.put(entry.code(), entry));
    }

    public MscCodeRepository(Path mscListFile) throws IOException {
        try (MVStore store = new MVStore.Builder().readOnly().fileName(mscListFile.toAbsolutePath().toString()).open()) {
            MVMap<String, MscCodeEntry> mvMscCodes = store.openMap(MSC_CODES_MAP_NAME);
            mscCodes.putAll(mvMscCodes);
        }
    }

    public Optional<MscCodeEntry> get(String code) {
        return Optional.ofNullable(mscCodes.get(code));
    }

    public Optional<String> getDescription(String code) {
        return get(code).map(MscCodeEntry::description);
    }

    public Collection<MscCodeEntry> getAllLoaded() {
        return List.copyOf(mscCodes.values());
    }
}
