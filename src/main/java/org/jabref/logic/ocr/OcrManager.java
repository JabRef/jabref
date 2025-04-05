package org.jabref.logic.ocr;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.engines.OcrEngineAdapter;
import org.jabref.logic.ocr.engines.TesseractAdapter;
import org.jabref.logic.ocr.exception.OcrProcessException;
import org.jabref.logic.ocr.models.OcrEngineConfig;
import org.jabref.logic.ocr.models.OcrLanguage;
import org.jabref.logic.ocr.models.OcrResult;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class for OCR operations.
 * <p>
 * This class coordinates OCR operations and integrates with JabRef's
 * task execution and indexing systems. It follows JabRef's facade pattern
 * to provide a simplified interface for OCR functionality.
 */
public class OcrManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrManager.class);
    
    private final Map<String, OcrService> ocrEngines = new HashMap<>();
    private final TaskExecutor taskExecutor;
    private final Optional<IndexManager> indexManager;
    private final String defaultEngineName;
    
    /**
     * Create a new OCR manager.
     *
     * @param tesseractPath Path to Tesseract installation
     * @param taskExecutor Task executor for background processing
     * @param indexManager Index manager for search indexing
     */
    public OcrManager(String tesseractPath, TaskExecutor taskExecutor, Optional<IndexManager> indexManager) {
        this.taskExecutor = taskExecutor;
        this.indexManager = indexManager;
        
        // Register available OCR engines
        registerEngines(tesseractPath);
        
        // Set default engine (first one registered)
        this.defaultEngineName = ocrEngines.isEmpty() ? null : ocrEngines.keySet().iterator().next();
    }
    
    /**
     * Register available OCR engines.
     *
     * @param tesseractPath Path to Tesseract installation
     */
    private void registerEngines(String tesseractPath) {
        // Register Tesseract OCR engine
        TesseractAdapter tesseractAdapter = new TesseractAdapter(tesseractPath);
        ocrEngines.put(tesseractAdapter.getEngineName(), tesseractAdapter);
        
        // Additional engines would be registered here
    }
    
    /**
     * Get an OCR service by name.
     *
     * @param engineName Engine name
     * @return OCR service
     * @throws OcrProcessException if the engine is not available
     */
    public OcrService getOcrService(String engineName) throws OcrProcessException {
        OcrService service = ocrEngines.get(engineName);
        
        if (service == null) {
            throw new OcrProcessException(
                    String.format("OCR engine '%s' not found. Available engines: %s", 
                            engineName, ocrEngines.keySet()));
        }
        
        if (!service.isAvailable()) {
            throw new OcrProcessException(
                    String.format("OCR engine '%s' is not available. Please check the installation.", 
                            engineName));
        }
        
        return service;
    }
    
    /**
     * Get the default OCR service.
     *
     * @return Default OCR service
     * @throws OcrProcessException if no engine is available
     */
    public OcrService getDefaultOcrService() throws OcrProcessException {
        if (defaultEngineName == null) {
            throw new OcrProcessException("No OCR engine is available");
        }
        
        return getOcrService(defaultEngineName);
    }
    
    /**
     * Process a PDF file with OCR.
     *
     * @param pdfPath Path to the PDF file
     * @return OCR result
     * @throws OcrProcessException if OCR processing fails
     */
    public OcrResult processPdf(Path pdfPath) throws OcrProcessException {
        OcrService service = getDefaultOcrService();
        return service.processPdf(pdfPath);
    }
    
    /**
     * Process an image file with OCR.
     *
     * @param imagePath Path to the image file
     * @return OCR result
     * @throws OcrProcessException if OCR processing fails
     */
    public OcrResult processImage(Path imagePath) throws OcrProcessException {
        OcrService service = getDefaultOcrService();
        return service.processImage(imagePath);
    }
    
    /**
     * Process a PDF file and add a searchable text layer.
     *
     * @param pdfPath Path to the PDF file
     * @param outputPath Path to save the processed PDF
     * @return OCR result
     * @throws OcrProcessException if OCR processing fails
     */
    public OcrResult processPdfAndAddTextLayer(Path pdfPath, Path outputPath) throws OcrProcessException {
        OcrService service = getDefaultOcrService();
        OcrResult result = service.processPdf(pdfPath);
        service.addTextLayerToPdf(pdfPath, outputPath, result);
        return result;
    }
    
    /**
     * Process PDF files for a BibEntry asynchronously.
     *
     * @param entry BibEntry to process
     * @return true if processing was started
     */
    public boolean processPdfFilesForEntry(BibEntry entry) {
        boolean processingStarted = false;
        
        for (LinkedFile file : entry.getFiles()) {
            if ("pdf".equalsIgnoreCase(file.getFileType().toString())) {
                Path filePath = file.findIn(entry).orElse(null);
                
                if (filePath != null) {
                    processingStarted = true;
                    
                    // Process in background
                    taskExecutor.execute(() -> {
                        try {
                            // Process PDF
                            OcrResult result = processPdf(filePath);
                            
                            // Index the result
                            indexManager.ifPresent(manager -> {
                                manager.addToIndex(entry, result.getExtractedText());
                            });
                            
                            LOGGER.info("OCR completed for {}: {} characters extracted", 
                                    filePath, result.getExtractedText().length());
                            
                        } catch (OcrProcessException e) {
                            LOGGER.error("OCR processing failed for {}: {}", 
                                    filePath, e.getMessage(), e);
                        }
                    });
                }
            }
        }
        
        return processingStarted;
    }
    
    /**
     * Get names of available OCR engines.
     *
     * @return Map of engine names to availability status
     */
    public Map<String, Boolean> getAvailableEngines() {
        Map<String, Boolean> engines = new HashMap<>();
        
        for (OcrService service : ocrEngines.values()) {
            engines.put(service.getEngineName(), service.isAvailable());
        }
        
        return engines;
    }
    
    /**
     * Check if any OCR engine is available.
     *
     * @return true if at least one engine is available
     */
    public boolean isAnyEngineAvailable() {
        return ocrEngines.values().stream().anyMatch(OcrService::isAvailable);
    }
}