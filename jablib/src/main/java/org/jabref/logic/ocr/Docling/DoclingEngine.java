package org.jabref.logic.ocr.Docling;

import java.io.IOException;
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

import com.fasterxml.jackson.databind.json.JsonMapper;
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

/// Implementation of the {@link OcrEngine} interface using Docling.
public class DoclingEngine implements OcrEngine {

    public static final Logger LOGGER = LoggerFactory.getLogger(DoclingEngine.class);
    private final OcrPreferences ocrPreferences;

    public DoclingEngine(OcrPreferences ocrPreferences) {
        this.ocrPreferences = ocrPreferences;
    }

    @Override
    public String getName() {
        return "Docling";
    }

    @Override
    public boolean isAvailable() {
        return true;
        //        ArrayList<String> command = StringUtil.splitRespectingEscapedWhitespace(ocrPreferences.getOcrEnginePath());
        //        command.add("--version");
        //        try {
        //            ProcessBuilder processBuilder = new ProcessBuilder(command);
        //            processBuilder.redirectErrorStream(true);
        //            Process process = processBuilder.start();
        //            boolean finished = process.waitFor(OcrUtils.CHECKING_TIMEOUT, TimeUnit.SECONDS);
        //            if (!finished) {
        //                process.destroyForcibly();
        //                LOGGER.debug("Checking Docling availability timed out");
        //                return false;
        //            }
        //            return process.exitValue() == 0;
        //        } catch (IOException e) {
        //            LOGGER.error("Docling is not available at {}: IOException occurred", ocrPreferences.getOcrEnginePath(), e);
        //            return false;
        //        } catch (InterruptedException e) {
        //            Thread.currentThread().interrupt();
        //            LOGGER.error("Checking Docling availability was interrupted", e);
        //            return false;
        //        }
    }

    @Override
    public OcrResult performOcrAndEmbedText(Path pdfPath) {
        if (!isAvailable()) {
            return OcrResult.failure(OcrFailureReason.NOT_AVAILABLE);
        }
        Path outputDir = pdfPath.getParent();
        // although a list of Strings, it represents a single command as that is how the ProcessBuilder expects it.
        ArrayList<String> command = new ArrayList<>();
        command.add("docling");
        command.add("--to");
        command.add("json");
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
        JsonMapper jsonMapper = new JsonMapper();
        DoclingDocument doclingDocument = jsonMapper.readValue(jsonOutputPath.toFile(), DoclingDocument.class);

        Map<Integer, ArrayList<DoclingText>> pageTextMap = new HashMap<>();
        for (DoclingText doclingText : doclingDocument.texts()) {
            // Docling outputs the pages number 1 indexed, while PDFBox uses 0 indexed pages
            int pageNo = doclingText.prov().getFirst().pageNo() - 1;
            pageTextMap.computeIfAbsent(pageNo, _ -> new ArrayList<>()).add(doclingText);
        }

        Path outputPdf = OcrUtils.makeOutputFilePath(originalPdf);

        try (PDDocument pdfWithText = Loader.loadPDF(originalPdf.toFile())) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float fontSize = 12f;
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
                            contentStream.setFont(font, fontSize);
                            contentStream.newLineAtOffset((float) bbox.left(), (float) bbox.bottom());
                            contentStream.showText(text);
                            contentStream.endText();
                        } catch (IllegalArgumentException e) {
                            contentStream.endText();
                            LOGGER.debug("Skipping unsupported text span: {}", text, e);
                        }
                    }
                }
            }

            pdfWithText.save(outputPdf.toFile());
        }

        return OcrResult.success(outputPdf);
    }
}
