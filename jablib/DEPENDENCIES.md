# JabLib Dependencies

This document lists all dependencies of the jablib module, with special attention to JavaFX-related dependencies.

## JavaFX Dependencies

JavaFX is a core UI framework used by JabRef. The following JavaFX modules are used:

### Core JavaFX Modules

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.openjfx:javafx-base` | 25.0.1 (25 on Linux ARM64) | Core JavaFX functionality including properties, collections, and events |
| `org.openjfx:javafx-fxml` | 25.0.1 (25 on Linux ARM64) | FXML support for declarative UI definitions |

### JavaFX-Related Libraries

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.jabref:afterburner.fx` | 2.0.0 | Lightweight JavaFX dependency injection framework |
| `org.jabref:easybind` | 2.3.0 | Simplifies JavaFX property bindings |

## Core Dependencies

### PDF Processing

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.apache.pdfbox:pdfbox` | 3.0.6 | PDF document manipulation |
| `org.apache.pdfbox:fontbox` | 3.0.6 | Font handling for PDFs |
| `org.apache.pdfbox:xmpbox` | 3.0.6 | XMP metadata handling for PDFs |
| `org.bouncycastle:bcprov-jdk18on` | 1.82 | Cryptography provider for reading write-protected PDFs |

### Search and Indexing

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.apache.lucene:lucene-core` | 10.3.2 | Core search engine functionality |
| `org.apache.lucene:lucene-queryparser` | 10.3.2 | Query parsing |
| `org.apache.lucene:lucene-queries` | 10.3.2 | Advanced query support |
| `org.apache.lucene:lucene-analysis-common` | 10.3.2 | Text analysis |
| `org.apache.lucene:lucene-highlighter` | 10.3.2 | Search result highlighting |

### Apache Commons

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.apache.commons:commons-csv` | 1.14.1 | CSV file parsing |
| `org.apache.commons:commons-lang3` | 3.20.0 | Common utilities and helpers |
| `org.apache.commons:commons-text` | 1.15.0 | Text processing utilities |
| `commons-logging:commons-logging` | 1.3.5 | Logging abstraction |
| `commons-io:commons-io` | 2.21.0 | I/O utilities |

### Database

| Dependency | Version | Purpose |
|------------|---------|---------|
| `com.h2database:h2-mvstore` | 2.3.232 | Embedded key-value store |
| `org.postgresql:postgresql` | 42.7.8 | PostgreSQL JDBC driver |
| `io.zonky.test:embedded-postgres` | 2.2.0 | Embedded PostgreSQL for testing |
| `io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8` | 18.1.0 | PostgreSQL binaries for macOS ARM64 |
| `io.zonky.test.postgres:embedded-postgres-binaries-linux-arm64v8` | 18.1.0 | PostgreSQL binaries for Linux ARM64 |

## Integration Libraries

### LibreOffice Integration

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.libreoffice:unoloader` | 24.8.4 | UNO bridge loader |
| `org.libreoffice:libreoffice` | 24.8.4 | LibreOffice API |

### Version Control

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.eclipse.jgit:org.eclipse.jgit` | 7.5.0.202512021534-r | Git support |

### HTTP Clients

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.jsoup:jsoup` | 1.22.1 | HTML parsing |
| `com.konghq:unirest-java-core` | 4.7.1 | HTTP client |
| `com.konghq:unirest-modules-gson` | 4.7.1 | Gson support for Unirest |
| `org.apache.httpcomponents.client5:httpclient5` | 5.5 | Apache HTTP client |
| `jakarta.ws.rs:jakarta.ws.rs-api` | 4.0.0 | JAX-RS API |

## AI and Language Models

| Dependency | Version | Purpose |
|------------|---------|---------|
| `dev.langchain4j:langchain4j` | 1.10.0 | LangChain4j core |
| `dev.langchain4j:langchain4j-open-ai` | 1.10.0 | OpenAI integration |
| `dev.langchain4j:langchain4j-mistral-ai` | 1.10.0 | Mistral AI integration |
| `dev.langchain4j:langchain4j-google-ai-gemini` | 1.10.0 | Google Gemini integration |
| `dev.langchain4j:langchain4j-http-client` | 1.10.0 | HTTP client for LangChain4j |
| `dev.langchain4j:langchain4j-http-client-jdk` | 1.10.0 | JDK HTTP client implementation |
| `org.apache.velocity:velocity-engine-core` | 2.4.1 | Template engine for AI prompts |
| `ai.djl:api` | 0.36.0 | Deep Java Library API |
| `ai.djl.huggingface:tokenizers` | 0.36.0 | Hugging Face tokenizers |
| `ai.djl.pytorch:pytorch-model-zoo` | 0.36.0 | PyTorch model zoo |
| `io.github.stefanbratanov:jvm-openai` | 0.11.0 | JVM OpenAI client |
| `com.squareup.okhttp3:okhttp` | 5.3.2 | OkHttp (required by OpenAI) |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | 2.3.0 | Kotlin standard library (required by OkHttp) |

## Data Processing

### Serialization

| Dependency | Version | Purpose |
|------------|---------|---------|
| `tools.jackson.dataformat:jackson-dataformat-yaml` | 3.0.3 | YAML support |
| `tools.jackson.core:jackson-databind` | 3.0.3 | JSON/object mapping |
| `com.fasterxml:aalto-xml` | 1.3.4 | Fast XML processing |
| `org.yaml:snakeyaml` | 2.5 | YAML parsing |

### Citation Processing

| Dependency | Version | Purpose |
|------------|---------|---------|
| `de.undercouch:citeproc-java` | 3.4.1 | Citation Style Language (CSL) processor |

### Text Processing

| Dependency | Version | Purpose |
|------------|---------|---------|
| `com.vladsch.flexmark:flexmark` | 0.64.8 | Markdown processing |
| `com.vladsch.flexmark:flexmark-html2md-converter` | 0.64.8 | HTML to Markdown conversion |
| `com.github.tomtung:latex2unicode_2.13` | 0.3.2 | LaTeX to Unicode conversion |
| `de.rototor.snuggletex:snuggletex-jeuclid` | 1.3.0 | LaTeX math rendering |

## Utility Libraries

### General Utilities

| Dependency | Version | Purpose |
|------------|---------|---------|
| `com.google.guava:guava` | 33.5.0-jre | Google core libraries |
| `org.jooq:jool` | 0.9.15 | Functional programming utilities |
| `com.github.ben-manes.caffeine:caffeine` | 3.2.3 | High-performance caching |
| `net.harawata:appdirs` | 1.5.0 | Application directory utilities |

### String and Diff Utilities

| Dependency | Version | Purpose |
|------------|---------|---------|
| `io.github.java-diff-utils:java-diff-utils` | 4.16 | Diff and patch utilities |
| `info.debatty:java-string-similarity` | 2.0.0 | String similarity algorithms |
| `io.github.thibaultmeyer:cuid` | 2.0.5 | ID generation |

### Platform Integration

| Dependency | Version | Purpose |
|------------|---------|---------|
| `com.github.javakeyring:java-keyring` | 1.0.4 | OS keyring integration |
| `com.googlecode.plist:dd-plist` | 1.28 | plist file parsing |
| `com.github.vatbub:mslinks` | 1.0.6.2 | Windows .lnk file parsing |

### Internationalization

| Dependency | Version | Purpose |
|------------|---------|---------|
| `com.ibm.icu:icu4j` | 72.0.1 | Unicode and internationalization support |

## Logging

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.slf4j:slf4j-api` | 2.0.17 | Logging API |
| `org.slf4j:jul-to-slf4j` | 2.0.17 | java.util.logging to SLF4J bridge |
| `org.apache.logging.log4j:log4j-to-slf4j` | 2.25.3 | Log4j to SLF4J bridge |
| `org.tinylog:slf4j-tinylog` | 2.7.0 | SLF4J to Tinylog adapter |
| `org.tinylog:tinylog-api` | 2.7.0 | Tinylog API |
| `org.tinylog:tinylog-impl` | 2.7.0 | Tinylog implementation |

## Build and Code Quality

### Annotation Processing

| Dependency | Version | Purpose |
|------------|---------|---------|
| `jakarta.annotation:jakarta.annotation-api` | 3.0.0 | Common annotations |
| `jakarta.inject:jakarta.inject-api` | 2.0.1 | Dependency injection annotations |
| `org.jspecify:jspecify` | 1.0.0 | Nullability annotations |

### ANTLR

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.antlr:antlr4` | 4.13.2 | Parser generator (compile-time) |
| `org.antlr:antlr4-runtime` | 4.13.2 | ANTLR runtime |

### Terminal UI

| Dependency | Version | Purpose |
|------------|---------|---------|
| `io.github.darvil82:terminal-text-formatter` | 2.3.0c | Terminal text formatting |
| `io.github.adr:e-adr` | 2.0.0 | Architecture Decision Records |

## Test Dependencies

### Testing Frameworks

| Dependency | Version | Scope | Purpose |
|------------|---------|-------|---------|
| `org.junit.jupiter:junit-jupiter-api` | 6.0.2 | test | JUnit 5 API |
| `org.junit.jupiter:junit-jupiter` | 6.0.2 | test | JUnit 5 implementation |
| `org.junit.jupiter:junit-jupiter-params` | 6.0.2 | test | Parameterized tests |
| `org.junit.platform:junit-platform-launcher` | 6.0.2 | test | Test launcher |

### Mocking

| Dependency | Version | Scope | Purpose |
|------------|---------|-------|---------|
| `org.mockito:mockito-core` | 5.21.0 | test | Mocking framework |
| `net.bytebuddy:byte-buddy` | 1.18.4 | test | Bytecode manipulation (used by Mockito) |

### Testing Utilities

| Dependency | Version | Scope | Purpose |
|------------|---------|-------|---------|
| `org.hamcrest:hamcrest` | 3.0 | test | Matcher library |
| `org.xmlunit:xmlunit-core` | 2.11.0 | test | XML comparison |
| `org.xmlunit:xmlunit-matchers` | 2.11.0 | test | XML matchers |
| `org.ow2.asm:asm` | 9.9.1 | test | Bytecode analysis |

### JavaFX Testing

| Dependency | Version | Scope | Purpose |
|------------|---------|-------|---------|
| `org.testfx:testfx-core` | 4.0.18 | test | JavaFX testing framework |
| `org.testfx:testfx-junit5` | 4.0.18 | test | JUnit 5 integration for TestFX |

### Architecture Testing

| Dependency | Version | Scope | Purpose |
|------------|---------|-------|---------|
| `com.tngtech.archunit:archunit` | 1.4.1 | test | Architecture testing |
| `com.tngtech.archunit:archunit-junit5-api` | 1.4.1 | test | JUnit 5 API for ArchUnit |
| `com.tngtech.archunit:archunit-junit5-engine` | 1.4.1 | testRuntime | JUnit 5 engine for ArchUnit |

### Code Generation

| Dependency | Version | Scope | Purpose |
|------------|---------|-------|---------|
| `cc.jilt:jilt` | 1.9 | testCompileOnly | Builder pattern generator |
| `io.github.classgraph:classgraph` | 4.8.184 | test | Classpath scanning |

## Development Tools

### Error Checking

| Dependency | Version | Scope | Purpose |
|------------|---------|-------|---------|
| `com.google.errorprone:error_prone_core` | 2.44.0 | errorprone | Error detection |
| `com.uber.nullaway:nullaway` | 0.12.15 | errorprone | Null safety checking |

## Version Notes

- **JavaFX Version**: 25.0.1 (general), 25 (Linux ARM64)
- **Lucene Version**: 10.3.2
- **PDFBox Version**: 3.0.6
- **Jackson Version**: 3.0.3
- **LangChain4j Version**: 1.10.0
- **DJL Version**: 0.36.0
- **JUnit Version**: 6.0.2

## Dependency Management

Dependencies are managed using Gradle with version constraints defined in the `versions` module (`versions/build.gradle.kts`). The jablib module uses a Bill of Materials (BOM) approach for several dependency groups:

- AI libraries: `ai.djl:bom`, `dev.langchain4j:langchain4j-bom`
- Testing: `org.junit:junit-bom`
- Jackson: `tools.jackson:jackson-bom`
- Embedded PostgreSQL: `io.zonky.test.postgres:embedded-postgres-binaries-bom`

## Module Information

The jablib module is configured for Maven publication with the following coordinates:

- **Group ID**: `org.jabref`
- **Artifact ID**: `jablib`
- **Description**: JabRef's Java library to work with BibTeX
- **License**: MIT
- **Repository**: https://github.com/JabRef/jabref/
