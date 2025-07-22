---
parent: Decision Records
nav_order: 47
---
# OCR Engine Selection for JabRef

## Context and Problem Statement

JabRef requires an OCR engine to extract text from scanned PDFs and image-based academic documents. Tesseract is currently implemented, but accuracy varies significantly with document quality and type. Academic documents present unique challenges: mathematical notation, multiple languages, complex layouts, tables, and mixed handwritten/printed content. Which OCR engine(s) should JabRef adopt to best serve its academic user base while balancing accuracy, cost, privacy, and implementation complexity?

## Decision Drivers

* Accuracy on academic document types (printed papers, scanned books, handwritten notes)
* Privacy requirements for unpublished research materials
* Cost constraints (open-source project with limited funding)
* Language support for international academic community
* Support for mathematical and scientific notation
* Offline capability for secure research environments
* Processing speed for batch operations
* Implementation and maintenance complexity
* Table and structure extraction capabilities
* Long-term sustainability and community support

## Considered Options

* Option 1: Tesseract OCR (current implementation)
* Option 2: Google Cloud Vision API
* Option 3: AWS Textract
* Option 4: Microsoft Azure Computer Vision
* Option 5: EasyOCR
* Option 6: PaddleOCR
* Option 7: ABBYY FineReader SDK

### Pros and Cons of the Options

#### Option 1: Tesseract OCR

Originally developed by Hewlett‑Packard as proprietary software in the 1980s, released as open source in 2005 and development was sponsored by Google in 2006.

* **Good**, because Version 4 adds LSTM‑based OCR engine and models for many additional languages and scripts, bringing the total to 116 languages. Additionally 37 scripts are supported
* **Good**, because completely free and open‑source (Apache 2.0 license)
* **Good**, because runs entirely offline preserving document privacy
* **Good**, because has an active developer community that regularly updates it, fixes bugs, and improves performance based on user feedback
* **Good**, because supports over 100 languages out of the box and can be trained to recognize additional languages or custom fonts
* **Good**, because Tesseract can process right‑to‑left text such as Arabic or Hebrew, many Indic scripts as well as CJK quite well
* **Neutral**, because Tesseract OCR is an open‑source product that can be used for free
* **Neutral**, because preprocessing can boost the character‑level accuracy of Tesseract 4.0 from 0.134 to 0.616 (+359 % relative change) and the F1 score from 0.163 to 0.729 (+347 % relative change)
* **Bad**, because Tesseract OCR recognizes the text in the well‑scanned email pretty well. However, for handwritten letters or smartphone‑captured documents it may output nonsense or nothing
* **Bad**, because performs best on documents with straightforward layouts but may struggle with complex layouts, requiring additional pre‑ or post‑processing
* **Bad**, because it may perform poorer on noisy scans compared to other solutions

##### Sources

* https://www.mdpi.com/2073-8994/12/5/715
* https://nanonets.com/blog/ocr-with-tesseract/
* https://en.wikipedia.org/wiki/Tesseract_(software)

#### Option 2: Google Cloud Vision API

Cloud‑based OCR service from Google.

* **Good**, because Vision OCR reaches 98 % text accuracy on a diverse data set
* **Good**, because it performs well across complex, multilingual, and handwritten documents without language hints
* **Good**, because language support rivals Azure and Tesseract and surpasses AWS Textract
* **Good**, because it is the only viable option among the tested engines for reliable handwriting recognition
* **Neutral**, because costs \$1.50 per 1 000 pages (first 1 000 pages free) and new customers get \$300 credit (~200 000 pages)
* **Bad**, because requires an internet connection and a Google Cloud account
* **Bad**, because documents are processed on Google servers (privacy concern)

##### Sources

* https://nanonets.com/blog/ocr-with-tesseract/
* https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr
* https://en.wikipedia.org/wiki/Tesseract_(software)

#### Option 3: AWS Textract

Amazon’s document‑analysis service.

* **Good**, because moderately outperforms others on noisy scans
* **Good**, because excels at extracting tables and form fields
* **Good**, because focuses on scanned, structured documents (e.g., forms)
* **Neutral**, because pricing similar to Google
* **Bad**, because limited support for non‑Latin scripts
* **Bad**, because handwriting recognition is weak

##### Sources

* https://news.ycombinator.com/item?id=20470439
* https://blog.roboflow.com/best-ocr-models-text-recognition/
* https://unstract.com/blog/guide-to-optical-character-recognition-with-tesseract-ocr/
* https://nanonets.com/blog/ocr-with-tesseract/
* https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr
* https://en.wikipedia.org/wiki/Tesseract_(software)

#### Option 4: Microsoft Azure Computer Vision

* **Good**, because leads Category 1 (digital screenshots) with 99.8 % accuracy
* **Good**, because handles complex layouts (invoices, receipts, ID cards) well
* **Good**, because supports 25+ languages
* **Bad**, because handwriting recognition is poor
* **Bad**, because requires cloud processing

##### Sources

* https://nanonets.com/blog/ocr-with-tesseract/
* https://blog.roboflow.com/best-ocr-models-text-recognition/
* https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project

#### Option 5: EasyOCR

* **Good**, because among open‑source engines it often matches or exceeds peers
* **Good**, because supports 70+ languages and runs locally
* **Good**, because optimized for speed, enabling real‑time processing
* **Bad**, because still trails top LMMs in pure accuracy
* **Bad**, because limited published benchmark data

##### Sources

* https://www.klippa.com/en/blog/information/tesseract-ocr/

#### Option 6: PaddleOCR

* **Good**, because achieves state‑of‑the‑art scores on ICDAR benchmarks
* **Good**, because supports major Asian and Latin scripts and is fast
* **Bad**, because supports fewer languages than Tesseract or EasyOCR
* **Bad**, because community is smaller and ecosystem less mature

##### Sources

* https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project

#### Option 7: ABBYY FineReader SDK

* **Good**, because preserves document structure (tables, zones) in output
* **Good**, because excels at tabular data extraction
* **Neutral**, because commercial licensing required
* **Bad**, because handwriting recognition is very poor
* **Bad**, because high cost (pricing not publicly listed)

##### Sources

* https://nanonets.com/blog/ocr-with-tesseract/
* https://en.wikipedia.org/wiki/Tesseract_(software)

## Decision Outcome

Chosen option: "Option 1: Tesseract OCR", with planned addition of "Option 2: Google Cloud Vision API" as an optional premium feature, because Tesseract provides a solid free foundation while Google Vision offers superior accuracy for users willing to trade privacy for performance.

### Consequences

* **Good**, because maintains free, privacy-preserving option as default
* **Good**, because allows users to opt-in to higher accuracy when needed
* **Good**, because Tesseract's 100+ language support covers academic needs
* **Good**, because implementation is already complete and tested
* **Bad**, because Tesseract struggles with handwritten text
* **Bad**, because requires additional development for cloud integration
* **Bad**, because increases support complexity with multiple engines

## Full Source Overview

The web resources that informed this ADR:

1. <https://www.mdpi.com/2073-8994/12/5/715>
2. <https://nanonets.com/blog/ocr-with-tesseract/>
3. <https://en.wikipedia.org/wiki/Tesseract_(software)>
4. <https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr>
5. <https://news.ycombinator.com/item?id=20470439>
6. <https://blog.roboflow.com/best-ocr-models-text-recognition/>
7. <https://unstract.com/blog/guide-to-optical-character-recognition-with-tesseract-ocr/>
8. <https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project>
9. <https://www.klippa.com/en/blog/information/tesseract-ocr/>
