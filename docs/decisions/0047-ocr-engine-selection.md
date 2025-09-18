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

* **Good**, because [Version 4 adds LSTM‑based OCR engine and models for many additional languages and scripts, bringing the total to 116 languages. Additionally 37 scripts are supported](https://en.wikipedia.org/wiki/Tesseract_\(software\))
* **Good**, because [completely free and open‑source (Apache 2.0 license)](https://en.wikipedia.org/wiki/Tesseract_\(software\))
* **Good**, because runs entirely offline preserving document privacy
* **Good**, because [has an active developer community that regularly updates it, fixes bugs, and improves performance based on user feedback](https://unstract.com/blog/guide-to-optical-character-recognition-with-tesseract-ocr/)
* **Good**, because [Tesseract can process right‑to‑left text such as Arabic or Hebrew, many Indic scripts as well as CJK quite well](https://en.wikipedia.org/wiki/Tesseract_\(software\))
* **Neutral**, because [preprocessing can boost the character‑level accuracy of Tesseract 4.0 from 0.134 to 0.616 (+359 % relative change)](https://www.mdpi.com/2073-8994/12/5/715)
* **Bad**, because [Tesseract OCR recognizes the text in the well‑scanned email pretty well. However, for handwritten letters or smartphone‑captured documents it may output nonsense or nothing](https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project)
* **Bad**, because [performs best on documents with straightforward layouts but may struggle with complex layouts](https://www.affinda.com/blog/6-top-open-source-ocr-tools-an-honest-review)
* **Bad**, because [it may perform poorer on noisy scans compared to other solutions](https://research.aimultiple.com/ocr-accuracy/)

##### Sources

* https://www.mdpi.com/2073-8994/12/5/715
* https://en.wikipedia.org/wiki/Tesseract\_(software)
* https://unstract.com/blog/guide-to-optical-character-recognition-with-tesseract-ocr/
* https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project
* https://www.affinda.com/blog/6-top-open-source-ocr-tools-an-honest-review
* https://research.aimultiple.com/ocr-accuracy/

#### Option 2: Google Cloud Vision API

Cloud‑based OCR service from Google.

* **Good**, because [Vision OCR reaches 98% text accuracy on a diverse data set](https://research.aimultiple.com/ocr-accuracy/)
* **Good**, because [language support rivals Azure and Tesseract and surpasses AWS Textract](https://source.opennews.org/articles/our-search-best-ocr-tool-2023/)
* **Good**, because [it is the only viable option among the tested engines for reliable handwriting recognition](https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project)
* **Neutral**, because [costs $1.50 per 1,000 pages (first 1,000 pages free) and new customers get $300 credit (\~200,000 pages)](https://source.opennews.org/articles/our-search-best-ocr-tool-2023/)
* **Bad**, because requires an internet connection and a Google Cloud account
* **Bad**, because documents are processed on Google servers (privacy concern)

##### Sources

* https://research.aimultiple.com/ocr-accuracy/
* https://source.opennews.org/articles/our-search-best-ocr-tool-2023/
* https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project

#### Option 3: AWS Textract

Amazon’s document‑analysis service.

* **Good**, because [moderately outperforms others on noisy scans](https://www.curvestone.io/blog-post/comparison-of-optical-character-recognition-ocr-services-from-microsoft-azure-aws-google-cloud)
* **Good**, because [excels at extracting tables and form fields](https://ironsoftware.com/csharp/ocr/blog/compare-to-other-components/aws-ocr-vs-azure-ocr/)
* **Good**, because [focuses on scanned, structured documents (e.g., forms)](https://www.amplenote.com/blog/2019_examples_amazon_textract_rekognition_microsoft_cognitive_services_google_vision)
* **Neutral**, because pricing similar to Google ($1.50 per 1,000 pages)
* **Bad**, because [limited support for non‑Latin scripts](https://source.opennews.org/articles/our-search-best-ocr-tool-2023/)
* **Bad**, because [handwriting recognition is weak](https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project)

##### Sources

* https://www.curvestone.io/blog-post/comparison-of-optical-character-recognition-ocr-services-from-microsoft-azure-aws-google-cloud
* https://ironsoftware.com/csharp/ocr/blog/compare-to-other-components/aws-ocr-vs-azure-ocr/
* https://www.amplenote.com/blog/2019_examples_amazon_textract_rekognition_microsoft_cognitive_services_google_vision
* https://source.opennews.org/articles/our-search-best-ocr-tool-2023/
* https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project

#### Option 4: Microsoft Azure Computer Vision

* **Good**, because [leads Category 1 of the above mentioned Decision Drivers (digital screenshots) with 99.8 % accuracy](https://research.aimultiple.com/ocr-accuracy/)
* **Good**, because [handles complex layouts (invoices, receipts, ID cards) well](https://ironsoftware.com/csharp/ocr/blog/compare-to-other-components/aws-ocr-vs-azure-ocr/)
* **Good**, because [supports 25+ languages](https://ironsoftware.com/csharp/ocr/blog/compare-to-other-components/aws-ocr-vs-azure-ocr/)
* **Bad**, because [handwriting recognition is poor](https://research.aimultiple.com/ocr-accuracy/)
* **Bad**, because requires cloud processing

##### Sources

* https://research.aimultiple.com/ocr-accuracy/
* https://ironsoftware.com/csharp/ocr/blog/compare-to-other-components/aws-ocr-vs-azure-ocr/

#### Option 5: EasyOCR

* **Good**, because [among open‑source engines it often matches or exceeds peers](https://blog.roboflow.com/best-ocr-models-text-recognition/)
* **Good**, because [supports 70+ languages and runs locally](https://www.affinda.com/blog/6-top-open-source-ocr-tools-an-honest-review)
* **Good**, because [optimized for speed, enabling real‑time processing](https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr)
* **Bad**, because [still trails top LMMs in pure accuracy](https://blog.roboflow.com/best-ocr-models-text-recognition/)
* **Bad**, because limited published benchmark data

##### Sources

* https://blog.roboflow.com/best-ocr-models-text-recognition/
* https://www.affinda.com/blog/6-top-open-source-ocr-tools-an-honest-review
* https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr

#### Option 6: PaddleOCR

* **Good**, because [achieves state‑of‑the‑art scores on ICDAR benchmarks](https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr)
* **Good**, because [supports major Asian and Latin scripts and is fast](https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr)
* **Bad**, because [supports fewer languages than Tesseract or EasyOCR](https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr)
* **Bad**, because [community is smaller and ecosystem less mature](https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr)

##### Sources

* https://www.plugger.ai/blog/comparison-of-paddle-ocr-easyocr-kerasocr-and-tesseract-ocr

#### Option 7: ABBYY FineReader SDK

* **Good**, because [preserves document structure (tables, zones) in output](https://research.aimultiple.com/ocr-accuracy/)
* **Good**, because [excels at tabular data extraction](https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project)
* **Neutral**, because commercial licensing required
* **Bad**, because [handwriting recognition is very poor](https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project)
* **Bad**, because high cost (pricing not publicly listed)

##### Sources

* https://research.aimultiple.com/ocr-accuracy/
* https://dida.do/blog/comparison-of-ocr-tools-how-to-choose-the-best-tool-for-your-project

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
