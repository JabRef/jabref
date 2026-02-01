# Technical Specification: OpenCitations Citation Fetcher

## Task Difficulty Assessment

**Difficulty Level: Medium**

**Rationale:**
- The implementation follows established patterns in the codebase (SemanticScholarCitationFetcher, CrossRefCitationFetcher)
- OpenCitations API is well-documented with a straightforward REST API structure
- Response parsing is simple JSON (no complex nested structures)
- No authentication required (API key is optional for rate limit increase)
- Some edge cases need handling (DOI extraction from PIDs, empty responses)
- Requires creating response model classes and proper error handling

---

## Technical Context

### Language & Dependencies
- **Language**: Java 21+
- **Key Dependencies**:
  - Gson (JSON parsing) - already used in SemanticScholarCitationFetcher
  - Jackson (alternative, used in CrossRefCitationFetcher)
  - JUnit 5 (testing)
  - Mockito (mocking in tests)
  - JabRef's existing fetcher infrastructure

### API Details
- **Base URL**: `https://api.opencitations.net/index/v2`
- **Key Endpoints**:
  - `/references/{id}`: Get outgoing references from a paper (supports DOI, PMID, OMID)
  - `/citations/{id}`: Get incoming citations to a paper (supports DOI, PMID, OMID)
  - `/citation-count/{id}`: Get citation count (supports DOI, PMID, OMID)
  - `/reference-count/{id}`: Get reference count (supports DOI, PMID, OMID)
- **Rate Limiting**: 180 requests/minute per IP
- **Authentication**: Optional access token via `authorization` header
- **Response Format**: JSON (default) or CSV

### Response Structure
```json
[
  {
    "oci": "06180334099-062603917082",
    "citing": "omid:br/06180334099 doi:10.1108/jd-12-2013-0166 openalex:W1977728350",
    "cited": "omid:br/062603917082 doi:10.1038/35079151 doi:10.1038/nature28042 pmid:11484021",
    "creation": "2015-03-09",
    "timespan": "P13Y9M9D",
    "journal_sc": "no",
    "author_sc": "no"
  }
]
```

---

## Implementation Approach

### Architecture Pattern
Follow the existing citation fetcher pattern used by SemanticScholarCitationFetcher:

1. **Main Fetcher Class**: `OpenCitationsFetcher` implements `CitationFetcher` interface
2. **Response Model Classes**: Create POJOs for JSON deserialization
3. **DOI-based Fetching**: Primary identifier will be DOI (most common in JabRef)
4. **URL Construction**: Separate URL building for better logging (follows ADR-0014)

### Class Structure

#### Main Class
**Location**: `jablib/src/main/java/org/jabref/logic/importer/fetcher/citation/opencitations/OpenCitationsFetcher.java`

**Responsibilities**:
- Implement `CitationFetcher` interface methods:
  - `getCitations(BibEntry entry)`: Returns papers citing the given entry
  - `getReferences(BibEntry entry)`: Returns papers referenced by the given entry
  - `getCitationCount(BibEntry entry)`: Returns citation count
  - `getName()`: Returns "OpenCitations"
- Construct API URLs with proper DOI formatting
- Handle HTTP requests using `URLDownload`
- Parse JSON responses using Gson
- Convert OpenCitations citation data to BibEntry objects
- Handle errors (missing DOI, API failures, empty responses)

#### Response Model Classes
**Location**: `jablib/src/main/java/org/jabref/logic/importer/fetcher/citation/opencitations/`

1. **`CitationResponse.java`**: Array wrapper for citation/reference responses
2. **`CitationItem.java`**: Individual citation record with fields:
   - `oci`: Open Citation Identifier
   - `citing`: PIDs of citing entity (space-separated: "omid:xxx doi:xxx pmid:xxx")
   - `cited`: PIDs of cited entity
   - `creation`: Publication date (ISO format: "YYYY-MM-DD")
   - `timespan`: Duration between publications (XSD format: "P1Y2M3D")
   - `journal_sc`: Journal self-citation flag ("yes"/"no")
   - `author_sc`: Author self-citation flag ("yes"/"no")
3. **`CountResponse.java`**: Single count value response
   - `count`: Integer count value

### DOI Extraction Logic

OpenCitations returns PIDs in format: `"omid:br/06180334099 doi:10.1108/jd-12-2013-0166 openalex:W1977728350"`

Strategy:
1. Split PID string by spaces
2. Look for entry starting with "doi:"
3. Extract DOI value after "doi:" prefix
4. Use DOI to fetch full BibEntry via existing DOI fetcher (DoiFetcher or CrossRef)
5. Fallback: If no DOI, check for PMID and use PMID fetcher

### Key Implementation Details

1. **DOI Requirement**: Only process entries that have a DOI field
   ```java
   if (entry.getDOI().isEmpty()) {
       return List.of();
   }
   ```

2. **URL Construction**:
   ```java
   private String getApiUrl(String endpoint, BibEntry entry) {
       String doi = entry.getDOI().orElseThrow().asString();
       return API_BASE_URL + "/" + endpoint + "/doi:" + doi;
   }
   ```

3. **BibEntry Conversion**:
   - Extract DOI from `citing` or `cited` field based on request type
   - Use DoiFetcher to get complete BibEntry
   - If DOI fetching fails, create minimal BibEntry with DOI field only

4. **Error Handling**:
   - Catch `MalformedURLException` → throw `FetcherException("Malformed URL", e)`
   - Catch `IOException` → throw `FetcherException("Could not read from OpenCitations", e)`
   - Handle empty responses → return empty list
   - Handle missing DOI in PID string → skip entry or use fallback

5. **Optional API Key Support**:
   ```java
   importerPreferences.getApiKey(getName())
       .ifPresent(apiKey -> urlDownload.addHeader("authorization", apiKey));
   ```

---

## Source Code Structure Changes

### New Files to Create

#### Main Implementation
```
jablib/src/main/java/org/jabref/logic/importer/fetcher/citation/opencitations/
├── OpenCitationsFetcher.java         (Main fetcher implementation)
├── CitationResponse.java             (Response wrapper - array of CitationItem)
├── CitationItem.java                 (Individual citation record)
└── CountResponse.java                (Count response wrapper)
```

#### Test Files
```
jablib/src/test/java/org/jabref/logic/importer/fetcher/citation/opencitations/
└── OpenCitationsFetcherTest.java     (Unit tests)
```

### Files to Modify

1. **`jablib/src/main/java/org/jabref/logic/importer/fetcher/citation/CitationFetcherType.java`**
   - Add `OPENCITATIONS("OpenCitations")` enum constant
   - Add case in `getCitationFetcher()` switch statement

---

## Data Model Changes

### CitationItem Structure

```java
public class CitationItem {
    private String oci;              // Open Citation Identifier
    private String citing;           // Space-separated PIDs of citing work
    private String cited;            // Space-separated PIDs of cited work
    private String creation;         // ISO date: "2015-03-09"
    private String timespan;         // XSD duration: "P13Y9M9D"
    
    @SerializedName("journal_sc")
    private String journalSelfCitation;  // "yes" or "no"
    
    @SerializedName("author_sc")
    private String authorSelfCitation;   // "yes" or "no"
    
    // Getters, setters, and helper methods
    public String extractDoi();      // Extract DOI from citing/cited string
    public BibEntry toBibEntry();    // Convert to BibEntry using DoiFetcher
}
```

### No Interface Changes
The `CitationFetcher` interface remains unchanged. OpenCitationsFetcher implements all existing methods.

---

## Verification Approach

### Unit Tests
**File**: `OpenCitationsFetcherTest.java`

**Test Cases**:
1. **`testGetReferencesWithValidDoi()`**
   - Use a well-known DOI (e.g., "10.1108/jd-12-2013-0166")
   - Verify list is not empty
   - Verify returned entries have DOI fields
   - Verify at least one entry has title/author

2. **`testGetCitationsWithValidDoi()`**
   - Use a well-cited DOI
   - Verify list is not empty
   - Check structure of returned entries

3. **`testGetCitationCount()`**
   - Verify count is returned as Optional<Integer>
   - Verify count is greater than 0 for well-cited paper

4. **`testEmptyWhenNoDoi()`**
   - Pass entry without DOI
   - Verify empty list is returned

5. **`testGetName()`**
   - Verify getName() returns "OpenCitations"

6. **`testMalformedResponse()`** (if needed)
   - Test handling of unexpected API responses

### Test Annotation
Use `@FetcherTest` annotation (existing in codebase) to mark tests that require network access.

### Manual Testing
1. Test with various DOIs from different sources
2. Verify rate limiting behavior (180 req/min)
3. Test with/without API key
4. Verify integration with JabRef GUI (if applicable)

### Build Commands
```bash
# Run tests
./gradlew :jablib:test --tests "*OpenCitations*"

# Run all fetcher tests
./gradlew :jablib:test --tests "*.citation.*"

# Check code style (if applicable)
./gradlew :jablib:checkstyleMain
```

---

## Implementation Considerations

### Edge Cases
1. **Missing DOI in entry**: Return empty list immediately
2. **DOI in response but no metadata**: Create minimal BibEntry with just DOI
3. **Multiple DOIs in PID string**: Take first one
4. **PMID-only citations**: Future enhancement (not in initial version)
5. **Empty API response**: Return empty list (not an error)
6. **Rate limiting**: API handles this; no special client-side handling needed

### Performance
- Each API call fetches all results (no pagination needed, API returns complete list)
- DOI resolution for each citation may be slow (network calls)
- Consider caching DOI lookups (future enhancement)

### Security
- No API key stored in code (uses ImporterPreferences)
- HTTPS only (built into API URL)

### Logging
- Log API URLs before requests (DEBUG level)
- Log response parsing errors (WARN level)
- Follow existing pattern from SemanticScholarCitationFetcher

---

## Dependencies & Compatibility

### Required Dependencies
All dependencies already present in JabRef:
- `com.google.code.gson:gson` (JSON parsing)
- JabRef's fetcher infrastructure
- JUnit 5 + Mockito (testing)

### Compatibility
- Works with JabRef's existing DOI fetcher integration
- Compatible with ImporterPreferences system
- Supports optional API key configuration
- No breaking changes to existing code

---

## Future Enhancements (Out of Scope)

1. Support PMID and OMID identifiers (currently DOI-only)
2. Client-side rate limiting to prevent API quota exhaustion
3. Caching of DOI resolutions
4. Support for CSV response format
5. Batch fetching for multiple entries
6. Metadata enrichment using OpenCitations data (timespan, self-citation flags)

---

## Summary

This is a **medium-complexity** task requiring:
- Creation of 4 new Java classes (1 fetcher + 3 models)
- Modification of 1 existing enum
- Unit tests following existing patterns
- No complex algorithms or data structures
- Straightforward REST API integration
- Good documentation via OpenCitations API docs

The implementation closely follows existing patterns (especially SemanticScholarCitationFetcher), minimizing architectural decisions and risks.
