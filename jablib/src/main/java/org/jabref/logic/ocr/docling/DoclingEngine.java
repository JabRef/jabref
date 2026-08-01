package org.jabref.logic.ocr.docling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.ocr.OcrEngine;
import org.jabref.logic.ocr.OcrFailureReason;
import org.jabref.logic.ocr.OcrPreferences;
import org.jabref.logic.ocr.OcrResult;
import org.jabref.logic.ocr.OcrUtils;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.model.ocr.docling.DoclingBBox;
import org.jabref.model.ocr.docling.DoclingDocument;
import org.jabref.model.ocr.docling.DoclingText;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/// Implementation of the [OcrEngine] interface using Docling.
public class DoclingEngine implements OcrEngine {

    public static final Logger LOGGER = LoggerFactory.getLogger(DoclingEngine.class);
    private final static JsonMapper JSON_MAPPER = new JsonMapper();
    private final static PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final static float FONTSIZE = 12F;
    private final OcrPreferences ocrPreferences;

    public DoclingEngine(OcrPreferences ocrPreferences) {
        this.ocrPreferences = ocrPreferences;
    }

    @Override
    public String getName() {
        return "Docling";
    }

    @Override
    public OcrResult performOcrAndEmbedText(Path pdfPath) {
        if (!OcrUtils.isAvailable(ocrPreferences)) {
            return OcrResult.failure(OcrFailureReason.NOT_AVAILABLE);
        }
        Path outputDir = pdfPath.getParent();
        // although a list of Strings, it represents a single command as that is how the ProcessBuilder expects it.
        ArrayList<String> command = new ArrayList<>();
        command.add("docling");
        command.add("--to");
        command.add("json");
        command.add("--no-tables");
        command.add("--image-export-mode");
        command.add("placeholder");
        command.add("--output");
        command.add(outputDir.toString());
        command.add(pdfPath.toString());
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            // Get the output and the errors of the process
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);

            boolean finished = process.waitFor(OcrUtils.TIMEOUT_MINS, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return OcrResult.failure(OcrFailureReason.TIMEOUT);
            }

            if (process.exitValue() == 0) {
                String fileName = pdfPath.getFileName().toString();
                String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                Path jsonOutputPath = outputDir.resolve(baseName + ".json");
                return embedText(jsonOutputPath, pdfPath);
            } else {
                return OcrResult.failure(OcrFailureReason.NON_ZERO_EXIT);
            }
        } catch (IOException e) {
            LOGGER.error("Error while running Docling.", e);
            return OcrResult.failure(OcrFailureReason.IO_ERROR);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            LOGGER.error("Docling process was interrupted.", e);
            return OcrResult.failure(OcrFailureReason.INTERRUPTED);
        }
    }

    private OcrResult embedText(Path jsonOutputPath, Path originalPdf) throws IOException {
        DoclingDocument doclingDocument = JSON_MAPPER.readValue(jsonOutputPath.toFile(), DoclingDocument.class);

        Map<Integer, ArrayList<DoclingText>> pageTextMap = new HashMap<>();
        for (DoclingText doclingText : doclingDocument.texts()) {
            // Docling outputs the pages number 1 indexed, while PDFBox uses 0 indexed pages
            int pageNo = doclingText.prov().getFirst().pageNo() - 1;
            pageTextMap.computeIfAbsent(pageNo, _ -> new ArrayList<>()).add(doclingText);
        }

        Path outputPdf = OcrUtils.makeOutputFilePath(originalPdf);

        try (PDDocument pdfWithText = Loader.loadPDF(originalPdf.toFile())) {
            for (Map.Entry<Integer, ArrayList<DoclingText>> entry : pageTextMap.entrySet()) {
                int pageNo = entry.getKey();
                PDPage pdPage = pdfWithText.getPage(pageNo);

                try (PDPageContentStream contentStream = new PDPageContentStream(
                        pdfWithText, pdPage, PDPageContentStream.AppendMode.APPEND, true)) {

                    for (DoclingText doclingText : entry.getValue()) {
                        DoclingBBox bbox = doclingText.prov().getFirst().bbox();
                        String text = doclingText.text();
                        try {
                            contentStream.beginText();
                            contentStream.setRenderingMode(RenderingMode.NEITHER);
                            contentStream.setFont(FONT, FONTSIZE);
                            contentStream.newLineAtOffset((float) bbox.l(), (float) bbox.b());
                            contentStream.showText(text);
                            contentStream.endText();
                        } catch (IllegalArgumentException e) {
                            text = filterEncodableCharacters(text);
                            contentStream.showText(text);
                            contentStream.endText();
                        }
                    }
                }
            }

            pdfWithText.save(outputPdf.toFile());
        }

        Files.delete(jsonOutputPath);
        return OcrResult.success(outputPdf);
    }

    private String filterEncodableCharacters(String text) {
        StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            try {
                FONT.encode(ch);
                filtered.append(ch);
            } catch (IllegalArgumentException | IOException e) {
                LOGGER.debug("Skipping unsupported character: {}", ch, e);
            }
        }
        return filtered.toString();
    }
}
