---
parent: Decision Records
nav_order: 56
---
# ADR for OCR engine selection

## Context and Problem Statement

JabRef requires an OCR engine to extract text from scanned academic PDFs, especially historic and old documents. Academic documents present unique challenges: mathematical notation, multiple languages, complex layouts, tables, and mixed handwritten/printed content.

Which OCR engines should JabRef support to serve its academic user base?

---

## Decision Drivers

- GPL and non-open-source are not acceptable (with exception to online services).
- No engine will be bundled in JabRef's distribution. Engines must be installed by the user.
- The output of OCR must be embeddable as a text layer in the PDF so that Lucene (or another search engine) can subsequently extract it. This allows Lucene to index the document through the existing logic with no changes.
- OCR Engines must support the accuracy requirements of academic documents: mathematical equations, tables, and multiple languages including non-latin scripts.

---

## Considered Options

- OCRmyPDF subprocess, MPL-2.0
- Tesseract via Tess4J Java API (JNA), Apache 2.0
- Apache Tika with Tesseract Java API, Apache 2.0
- Docling subprocess, MIT
- olmOCR subprocess, Apache 2.0
- PaddleOCR via llama.cpp HTTP API, Apache 2.0
- TurboOCR HTTP API, MIT
- deepdoctection subprocess, Apache 2.0
- SimpleHTR subprocess, MIT
- Surya subprocess, GPL-3.0
- Kreuzberg Java API (FFI), Elastic License 2.0

---

## Decision Outcome

Chosen option: A prioritized set of engines based on availability, quality, and implementation complexity.

1. **OCRmyPDF** (Primary engine). Handles both OCR and text layer embedding in a single subprocess call. Has a plugin system that allows replacing Tesseract with other engines such as PaddleOCR, making it extensible without changes to JabRef's integration code.
2. **Tesseract via Tess4J** (Fallback engine). Used when OCRmyPDF is unavailable (no Python on the user's machine). Pure Java dependency, no subprocess required, but text layer embedding must be handled separately.
3. **Docling** (Stretch goal), high-quality engine. Better layout understanding for complex academic documents: multi-column reading order, tables, and formulas. Requires Python and downloads ML model weights on first use.
4. **olmOCR** (Stretch goal), academic-specialized engine. Specifically trained on academic PDF content, directly addressing JabRef's primary use case. Requires GPU for reasonable performance and downloads model weights on first use.
5. **PaddleOCR-VL** (Advanced tier), remote engine. State-of-the-art accuracy for users who provision a local inference server. Used via the generic `RemoteOcrEngine` HTTP pattern, which also covers other HTTP-based OCR services can also be used through the OCRmyPDF plugins.

Apache Tika is not selected as a primary OCR path because it abstracts away fine-grained settings and does not improve on OCRmyPDF for scanned documents. Surya and Kreuzberg are excluded on license grounds. SimpleHTR and deepdoctection are documented for future consideration but are out of scope for this project phase.

### Consequences

- Good, because OCRmyPDF handles the embedding the text layer automatically.
- Good, because OCRmyPDF will allow users to use different engines through the plugins it offers.
- Good, because OCRmyPDF's `--skip-text` flag handles partially searchable PDFs by skipping pages that already have a valid text layer.
- Bad, because OCRmyPDF requires Python 3.x to be installed on the user's machine, which is an additional step for non-technical users.
- Bad, because Tess4J's JNA native library loading remains fragile.
- Bad, because Docling and olmOCR download machine learning model weights (1–2 GB) on first use, which must be an explicit user opt-in and cannot happen silently.

### Confirmation

The integration will be confirmed by a `DocumentReaderTest` that verifies:

1. `PDFTextStripper` returns empty content for scanned PDFs before performing OCR (established in PR [#15428](https://github.com/JabRef/jabref/pull/15428)) for only the PDFs that are fully scanned and have no text at all (some PDFs have partially selectable text).
2. After OCR processing, `PDFTextStripper` returns more selectable content than before from the same file.

---

## Pros and Cons of the Options

### OCRmyPDF

Homepage: [https://github.com/ocrmypdf/OCRmyPDF](https://github.com/ocrmypdf/OCRmyPDF)
License: [MPL-2.0](https://github.com/ocrmypdf/OCRmyPDF#MPL-2.0-1-ov-file)
Integration: Subprocess via `ProcessBuilder`

OCRmyPDF is a Python command-line tool that takes a scanned PDF, OCRs each page using Tesseract internally, and outputs a new PDF with an invisible text layer. The output PDF is immediately readable by Lucene, requiring no further PDF production.

- Good, because both OCR extraction and text layer embedding are handled in a single subprocess call, and no separate step is needed for embedding the text layer into the PDF.
- Good, because it uses hOCR to position words producing accurately located invisible text, enabling correct copy-paste and PDF viewer search highlighting.
- Good, because `--skip-text` correctly handles partially searchable PDFs without re-OCRing already-good pages.
- Good, because it has [plugins](https://github.com/ocrmypdf/OCRmyPDF#plugins) that uses different engines from Tesseract like [OCRmyPDF-PaddleOCR](https://github.com/clefru/ocrmypdf-paddleocr) that replaces the standard Tesseract OCR engine with PaddleOCR.
- Good, because MPL-2.0 is compatible with JabRef's MIT license for subprocess invocation with no source modification.
- Good, because it is available via `pip`, `apt`, `brew`, and standalone installers on all three supported platforms.
- Bad, because it requires Python 3.x and pip to be available on the user's machine.
- Bad, because subprocess launch takes some time, which is acceptable for background tasks.

### Tesseract via Tess4J

Tess4j homepage: [https://github.com/nguyenq/tess4j](https://github.com/nguyenq/tess4j)
Tesseract homepage: [https://tesseract-ocr.github.io/](https://tesseract-ocr.github.io/)
Baeldung integration guide: [https://www.baeldung.com/java-ocr-tesseract](https://www.baeldung.com/java-ocr-tesseract)
License: [Apache 2.0](https://github.com/nguyenq/tess4j?tab=Apache-2.0-1-ov-file#readme)
Integration: Java API via JNA (Java Native Access)

Tess4J is a Java wrapper for the Tesseract C++ OCR library. It calls `tesseract.doOCR(file)` directly from Java using JNA, returning extracted text as a `.txt` file. When used as the OCR engine, a separate step is required to embed the extracted text as an invisible layer. Correct multi-page embedding requires per-page Tesseract calls with hOCR output to obtain word coordinates instead of words distribution method as in [SearchablePdfCreator.java](https://github.com/JabRef/jabref/pull/13313/changes#diff-8cb8cef63fa6d8197687f74c560f9ab65a39a4fc566f273af98ef12dd578e25a).

- Good, because it requires no Python installation pure Java with a native library dependency.
- Good, because Apache 2.0 license is fully compatible with JabRef's MIT license.
- Good, because Tesseract supports 100+ languages via downloadable `.traineddata` files.
- Bad, because `module-info.java` requires explicit JNA module access declarations.
- Bad, because correct multi-page text layer embedding requires per-page hOCR processing, adding significant implementation complexity compared to OCRmyPDF.
- Bad, because `tessdata` path discovery differs across Windows, macOS, and Linux.

### Apache Tika with Tesseract

Homepage: [https://tika.apache.org/](https://tika.apache.org/)
Tika OCR documentation: [https://cwiki.apache.org/confluence/display/tika/tikaocr](https://cwiki.apache.org/confluence/display/tika/tikaocr)
License: [Apache 2.0](https://github.com/apache/tika/blob/main/LICENSE.txt)
Integration: Java API

Apache Tika is a content extraction toolkit it can invoke Tesseract under the hood via its `TesseractOCRParser` when it encounters image-based content.

- Good, because Apache 2.0 license is fully compatible.
- Good, because Apache Tike is already used in JabRef.
- Bad, because Tika's OCR output is raw extracted text without bounding box coordinates. A separate text layer embedding step is still required, with the same multi-page complexity as the direct Tess4J approach.
- Bad, because Tika abstracts away fine-grained OCR settings, making expert user configuration harder than with direct Tess4J or OCRmyPDF.
- Bad, because Tika internally still calls a system-installed Tesseract binary no installation advantage over direct Tess4J use.
- Bad, because it is not extendable. There is no common OCR interface.

### Docling

Homepage: [https://github.com/docling-project/docling](https://github.com/docling-project/docling)
License: [MIT](https://github.com/docling-project/docling?tab=MIT-1-ov-file#readme)
Integration: Subprocess via `ProcessBuilder`

Docling is a Python document intelligence library from IBM Research. It performs full document understanding: layout analysis, reading order detection, table structure recognition, and OCR via Tesseract or EasyOCR. It outputs clean Markdown or structured JSON.

- Good, because it produces significantly better output quality for complex academic documents: correct multi-column reading order, table recognition, and formula identification.
- Good, because MIT license is fully compatible.
- Good, because 53k+ GitHub stars indicate strong community adoption and longevity.
- Bad, because output is Markdown text, not a pre-built searchable PDF a text layer embedding step is still required.
- Bad, because ML model weights (~1–2 GB) are downloaded on first use.
- Bad, because it requires Python 3.x.

### olmOCR

Homepage: [https://github.com/allenai/olmocr](https://github.com/allenai/olmocr)
License: [Apache 2.0](https://github.com/allenai/olmocr?tab=Apache-2.0-1-ov-file#readme)
Integration: Subprocess via `ProcessBuilder`

olmOCR is an OCR toolkit from the Allen Institute for AI (AI2), specifically trained and optimized for academic PDF documents using a vision-language model architecture.

- Good, because it is specifically optimized for academic PDFs the primary document type JabRef users work with.
- Good, because Apache 2.0 license is fully compatible.
- Good, because subprocess integration pattern is identical to OCRmyPDF and Docling.
- Bad, because GPU hardware is required for reasonable performance.
- Bad, because model weights are downloaded on first use.
- Bad, because it is newer and less tested than OCRmyPDF or Tesseract.

### PaddleOCR-VL via llama.cpp

Homepage: [https://huggingface.co/PaddlePaddle/PaddleOCR-VL](https://huggingface.co/PaddlePaddle/PaddleOCR-VL)
llama.cpp integration: [https://github.com/ggml-org/llama.cpp/pull/16701](https://github.com/ggml-org/llama.cpp/pull/16701)
License: [Apache 2.0](https://huggingface.co/datasets/choosealicense/licenses/blob/main/markdown/apache-2.0.md)
Integration: HTTP API via locally running llama.cpp server

PaddleOCR-VL is a vision-language model achieving state-of-the-art accuracy on OCR benchmarks as of October 2025. llama.cpp PR 16701 adds support for running PaddleOCR-VL locally via llama.cpp's server, which exposes an HTTP API. For JabRef, integration is as a `RemoteOcrEngine` sending requests to a user-provisioned llama.cpp instance.

- Good, because depending on the OCR model, there can be state-of-the-art accuracy on diverse document types and scripts including non-Latin. Model makers regularly release new models.
- Good, because Apache 2.0 license is fully compatible.
- Good, because the HTTP API integration pattern is reusable for other remote OCR engines.
- Good, because inference is fast as a wide variety of GPU backends is supported.
- Bad, because the user must separately install and run a llama.cpp server instance.
- Bad, because model weights must be downloaded separately (several GB depending on model variant).
- Bad, because llama.cpp PR 16701 is recent and less tested than OCRmyPDF or Tesseract.

[https://github.com/ggml-org/llama.cpp/blob/master/docs/multimodal.md](https://github.com/ggml-org/llama.cpp/blob/master/docs/multimodal.md)
[https://github.com/ggml-org/llama.cpp/blob/master/docs/ops.md](https://github.com/ggml-org/llama.cpp/blob/master/docs/ops.md)

### TurboOCR

Homepage: [https://github.com/aiptimizer/TurboOCR](https://github.com/aiptimizer/TurboOCR)
License: [MIT](https://github.com/aiptimizer/TurboOCR?tab=MIT-1-ov-file#readme)
Integration: HTTP API

TurboOCR is a C++/CUDA server wrapping PaddleOCR's PP-OCRv5 with TensorRT FP16 acceleration. It achieves 270 images/second on FUNSD benchmarks and exposes HTTP and gRPC APIs returning JSON with per-word bounding boxes. It represents the institutional/GPU-infrastructure tier of OCR integration.

- Good, because MIT license is fully compatible.
- Good, because JSON response includes bounding boxes for accurately positioned text layer embedding.
- Good, because the HTTP adapter pattern it establishes covers TurboOCR, Google Cloud Vision, Azure Computer Vision, and any other HTTP-based OCR service a single `RemoteOcrEngine` implementation serves all.
- Bad, because to date, it requires NVIDIA GPU hardware, thereby excluding a wide range of users, making it unsuitable as a default engine.
- Bad, because it raises privacy concerns when documents contain unpublished research materials.
- Bad, because to date, the project only supports a few select language family bundles (Latin, Chinese, Greek, Korean, Arabic, eslav, thai).

### deepdoctection

Homepage: [https://github.com/deepdoctection/deepdoctection](https://github.com/deepdoctection/deepdoctection)
License: [Apache 2.0](https://github.com/deepdoctection/deepdoctection?tab=Apache-2.0-1-ov-file#readme)
Integration: Subprocess via `ProcessBuilder`

deepdoctection is a Python Document AI orchestration framework coordinating layout analysis (Detectron2/Transformers), OCR (Tesseract, DocTr, AWS Textract), table structure recognition, and token classification. It produces structured output capturing the full document hierarchy and is the most comprehensive document understanding pipeline among all evaluated options.

- Good, because Apache 2.0 license is fully compatible.
- Good, because it represents the state of the art in document understanding pipelines and serves as the reference architecture for what a comprehensive future JabRef OCR pipeline could look like.
- Bad, because installation requires Detectron2, PyTorch, and multiple model weights..
- Bad, because the installation complexity makes it impractical to document as a user-installable engine for most JabRef users.

deepdoctection is not selected for this project phase but is retained as the reference architecture for future comprehensive document understanding.

### SimpleHTR

Homepage: [https://github.com/githubharald/SimpleHTR](https://github.com/githubharald/SimpleHTR)
License: [MIT](https://github.com/githubharald/SimpleHTR?tab=MIT-1-ov-file#readme)
Integration: Subprocess via `ProcessBuilder`

SimpleHTR is a TensorFlow model trained specifically for handwritten text recognition on historical documents. It addresses the use case that all other evaluated engines handle poorly: handwriting in manuscripts, letters, and archival records.

- Good, because MIT license is fully compatible.
- Good, because it uniquely addresses handwritten historical documents a genuine need for historians and archivists using JabRef.
- Bad, because it requires TensorFlow, making it a large dependency for a narrow use case.
- Bad, because it produces raw text output without bounding boxes, requiring simple page-level text embedding without word-level position accuracy.
- Bad, because it is appropriate only for a small subset of JabRef users.

SimpleHTR is not selected for this project phase but is documented as a future optional engine for users working with handwritten historical documents.

### Surya

Homepage: [https://github.com/VikParuchuri/surya](https://github.com/VikParuchuri/surya)
License: [GPL-3.0](https://github.com/datalab-to/surya?tab=GPL-3.0-1-ov-file#readme)

Surya is a Python OCR engine with strong performance on non-Latin scripts and complex multi-column layouts.

- Good, because it reportedly outperforms Tesseract on non-Latin scripts and complex document layouts.
- Bad, because **GPL-3.0 license is incompatible with JabRef's policy**. Surya is excluded on license grounds.

### Kreuzberg

Homepage: [https://github.com/kreuzberg-dev/kreuzberg](https://github.com/kreuzberg-dev/kreuzberg)
License: [Elastic License 2.0 (ELv2)](https://github.com/kreuzberg-dev/kreuzberg?tab=License-1-ov-file#readme)
Integration: Java API

Kreuzberg is a Rust-core document intelligence framework with Java bindings. It supports multiple OCR backends (Tesseract, PaddleOCR, EasyOCR) and 91+ file formats. A Java binding would eliminate the Python subprocess requirement entirely.

- Good, because a native Java API binding avoids Python subprocess dependency entirely.
- Good, because it supports multiple OCR backends through a single interface.
- Bad, because **Elastic License 2.0 is not an OSI-approved open-source license**. ELv2 is incompatible with JabRef's MIT license and distribution model. Kreuzberg is excluded on license grounds.

---

## More Links

- Meta issue: [#13267](https://github.com/JabRef/jabref/issues/13267)
- GSoC 2025 implementation PR (reference, not merged): [#13313](https://github.com/JabRef/jabref/pull/13313)
- GSoC 2025 ADR PR (reference, not merged): [#13573](https://github.com/JabRef/jabref/pull/13573)
- Scanned PDF test resource: [#15428](https://github.com/JabRef/jabref/pull/15428)
- GSoC 2026 project description: [https://jabref.github.io/GSoC/posts/ocr/](https://jabref.github.io/GSoC/posts/ocr/)
