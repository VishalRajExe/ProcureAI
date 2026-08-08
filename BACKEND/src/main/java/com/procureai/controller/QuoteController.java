package com.procureai.controller;

import com.procureai.dto.QuoteUploadRequest;
import com.procureai.entity.Quote;
import com.procureai.entity.WorkflowExecution;
import com.procureai.exception.ExtractionException;
import com.procureai.service.QuoteService;
import com.procureai.util.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping("/workflows")
    public ResponseEntity<WorkflowExecution> createWorkflow(@RequestBody Map<String, String> body) {
        WorkflowExecution wf = quoteService.createWorkflow(
                body.getOrDefault("title", "Untitled Procurement"),
                body.get("description"),
                CurrentUser.id());
        return ResponseEntity.ok(wf);
    }

    /** JSON-based ingestion — used by the demo and any client that already has quote text. */
    @PostMapping({"", "/workflows/{workflowId}/quotes"})
    public ResponseEntity<Quote> uploadJsonQuote(@PathVariable(required = false) Long workflowId, @Valid @RequestBody QuoteUploadRequest req) {
        WorkflowExecution wf = workflowId != null ? quoteService.getWorkflow(workflowId)
                : quoteService.createWorkflow("Procurement " + System.currentTimeMillis(), "Auto-created for quote upload", CurrentUser.id());
        Quote quote = quoteService.ingestQuote(wf, req.vendorName(), req.vendorEmail(), req.rawDocumentText(),
                req.sourceFileName(), Quote.SourceType.JSON, CurrentUser.id());
        return ResponseEntity.ok(quote);
    }

    /** Multipart file ingestion (PDF/PNG/JPG). Text extraction is performed here before AI extraction. */
    @PostMapping(value = {"/upload", "/workflows/{workflowId}/quotes/upload"}, consumes = "multipart/form-data")
    public ResponseEntity<Quote> uploadFileQuote(@PathVariable(required = false) Long workflowId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam("vendorName") String vendorName,
                                                  @RequestParam(value = "vendorEmail", required = false) String vendorEmail) {
        if (file.isEmpty()) {
            throw new ExtractionException("Uploaded file is empty");
        }
        String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String lower = filename.toLowerCase();
        Quote.SourceType sourceType = lower.endsWith(".pdf") ? Quote.SourceType.PDF
                : (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ? Quote.SourceType.IMAGE
                : Quote.SourceType.EMAIL_TEXT;

        String rawText;
        try {
            if (sourceType == Quote.SourceType.PDF) {
                rawText = extractPdfText(file);
            } else if (sourceType == Quote.SourceType.IMAGE) {
                // Hackathon scope: OCR engine is not wired up for images in this build.
                // Document is still recorded, but extraction is reported clearly as unsupported
                // rather than silently guessing — see README "Future Improvements".
                throw new ExtractionException("OCR for image uploads is not enabled in this build. Please use the PDF or JSON quote path for the demo.");
            } else {
                rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ExtractionException("Could not read uploaded file: " + e.getMessage());
        }

        WorkflowExecution wf = workflowId != null ? quoteService.getWorkflow(workflowId)
                : quoteService.createWorkflow("Procurement " + System.currentTimeMillis(), "Auto-created for file upload", CurrentUser.id());
        Quote quote = quoteService.ingestQuote(wf, vendorName, vendorEmail, rawText, filename, sourceType, CurrentUser.id());
        return ResponseEntity.ok(quote);
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(doc);
            if (text == null || text.isBlank()) {
                throw new ExtractionException("No extractable text found — this looks like a scanned PDF. OCR is not enabled in this build.");
            }
            return text;
        }
    }

    @GetMapping("/workflows/{workflowId}")
    public ResponseEntity<List<Quote>> getForWorkflow(@PathVariable Long workflowId) {
        return ResponseEntity.ok(quoteService.getQuotesForWorkflow(workflowId));
    }

    @GetMapping
    public ResponseEntity<List<Quote>> getAll() {
        return ResponseEntity.ok(quoteService.getAllQuotes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quote> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.getQuote(id));
    }
}
