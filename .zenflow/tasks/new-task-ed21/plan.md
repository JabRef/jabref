# Spec and build

## Configuration
- **Artifacts Path**: {@artifacts_path} → `.zenflow/tasks/{task_id}`

---

## Agent Instructions

Ask the user questions when anything is unclear or needs their input. This includes:
- Ambiguous or incomplete requirements
- Technical decisions that affect architecture or user experience
- Trade-offs that require business context

Do not make assumptions on important decisions — get clarification first.

---

## Workflow Steps

### [x] Step: Technical Specification
<!-- chat-id: 42e719eb-bed5-4e52-9b11-934c2e648256 -->

Assess the task's difficulty, as underestimating it leads to poor outcomes.
- easy: Straightforward implementation, trivial bug fix or feature
- medium: Moderate complexity, some edge cases or caveats to consider
- hard: Complex logic, many caveats, architectural considerations, or high-risk changes

Create a technical specification for the task that is appropriate for the complexity level:
- Review the existing codebase architecture and identify reusable components.
- Define the implementation approach based on established patterns in the project.
- Identify all source code files that will be created or modified.
- Define any necessary data model, API, or interface changes.
- Describe verification steps using the project's test and lint commands.

Save the output to `{@artifacts_path}/spec.md` with:
- Technical context (language, dependencies)
- Implementation approach
- Source code structure changes
- Data model / API / interface changes
- Verification approach

If the task is complex enough, create a detailed implementation plan based on `{@artifacts_path}/spec.md`:
- Break down the work into concrete tasks (incrementable, testable milestones)
- Each task should reference relevant contracts and include verification steps
- Replace the Implementation step below with the planned tasks

Rule of thumb for step size: each step should represent a coherent unit of work (e.g., implement a component, add an API endpoint, write tests for a module). Avoid steps that are too granular (single function).

Save to `{@artifacts_path}/plan.md`. If the feature is trivial and doesn't warrant this breakdown, keep the Implementation step below as is.

---

### [x] Step: Create Response Model Classes
<!-- chat-id: 4b2cab23-bca5-4a0b-aee5-8de9581a663e -->

Create the data model classes for OpenCitations API responses:
- `CitationItem.java`: Individual citation record with OCI, citing/cited PIDs, creation date, timespan
- `CitationResponse.java`: Array wrapper for citations/references
- `CountResponse.java`: Wrapper for count responses

Each class should:
- Use package-private fields (no access modifier) - no getters/setters needed
- Include Gson annotations where needed (e.g., @SerializedName for "journal_sc", "author_sc")
- Add public helper methods in CitationItem: extractDoi() and toBibEntry()

**Verification**: Models should compile without errors and follow JabRef code style.

---

### [ ] Step: Implement OpenCitationsFetcher Core Class

Implement `OpenCitationsFetcher` class that implements `CitationFetcher` interface:
- Constructor accepting `ImporterPreferences`
- `getName()` returning "OpenCitations"
- URL construction methods for references, citations, and counts
- Implement `getReferences()`: fetch outgoing references
- Implement `getCitations()`: fetch incoming citations
- Implement `getCitationCount()`: fetch citation count
- DOI extraction and BibEntry conversion logic
- Error handling for missing DOI, network errors, malformed responses
- Optional API key support via ImporterPreferences

**Verification**: Class compiles, implements all required interface methods.

---

### [ ] Step: Register OpenCitations in CitationFetcherType Enum

Update `CitationFetcherType.java`:
- Add `OPENCITATIONS("OpenCitations")` enum constant
- Add case in `getCitationFetcher()` switch to instantiate `OpenCitationsFetcher`

**Verification**: Enum compiles, factory method returns correct fetcher instance.

---

### [ ] Step: Implement Unit Tests

Create `OpenCitationsFetcherTest.java` with test cases:
- `testGetReferencesWithValidDoi()`: Test reference fetching with known DOI
- `testGetCitationsWithValidDoi()`: Test citation fetching with known DOI  
- `testGetCitationCount()`: Verify citation count retrieval
- `testEmptyWhenNoDoi()`: Verify empty list when DOI missing
- `testGetName()`: Verify fetcher name

Mark test class with `@FetcherTest` annotation.

**Verification**: Run `./gradlew :jablib:test --tests "*OpenCitations*"` - all tests pass.

---

### [ ] Step: Final Verification and Reporting

1. Run full test suite for citation fetchers
2. Verify code style compliance (if checkstyle configured)
3. Manual smoke test with JabRef (if applicable)
4. Write completion report to `{@artifacts_path}/report.md` with:
   - Summary of what was implemented
   - Test results
   - Any challenges or issues encountered
   - Known limitations (e.g., DOI-only support)
