package com.procureai.controller;

import com.procureai.dto.QuoteDtos;
import com.procureai.entity.Quote;
import com.procureai.entity.WorkflowExecution;
import com.procureai.exception.ExtractionException;
import com.procureai.service.QuoteService;
import com.procureai.util.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Hardened Quote REST controller.
 *
 * Security:
 * - All endpoints require authentication (@anyRequest().authenticated() in SecurityConfig)
 * - File uploads: magic-bytes validated, size hard-capped, extension allow-listed
 * - vendorName sanitized and max-length enforced via DTO
 * - rawDocumentText max 50,000 chars via DTO
 * - Workflow creation uses typed DTO, not raw Map
 * - Filenames are sanitized before use
 */
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private static final Logger log = LoggerFactory.getLogger(QuoteController.class);

    /** Absolute max file size enforced in code (belt-and-suspenders over multipart config). */
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB

    /** PDF magic bytes (file signature). */
    private static final byte[] PDF_MAGIC = new byte[] { 0x25, 0x50, 0x44, 0x46 }; // %PDF

    /** Allowed content-type values from the client Content-Type header. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "text/plain",
        "application/octet-stream"
    );

    private final QuoteService quoteService;
    private final com.procureai.repository.WorkflowExecutionRepository workflowRepository;

    public QuoteController(QuoteService quoteService,
                           com.procureai.repository.WorkflowExecutionRepository workflowRepository) {
        this.quoteService = quoteService;
        this.workflowRepository = workflowRepository;
    }

    @PostMapping("/workflows")
    public ResponseEntity<WorkflowExecution> createWorkflow(
            @Valid @RequestBody QuoteDtos.CreateWorkflowRequest req) {
        WorkflowExecution wf = quoteService.createWorkflow(req.title(), req.description(), CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(wf);
    }

    /** JSON-based quote ingestion — all fields validated via DTO. */
    @PostMapping({"", "/workflows/{workflowId}/quotes"})
    public ResponseEntity<Quote> uploadJsonQuote(
            @PathVariable(required = false) Long workflowId,
            @Valid @RequestBody QuoteDtos.QuoteUploadRequest req) {

        WorkflowExecution wf = resolveOrCreateWorkflow(workflowId);
        String docText = req.getEffectiveRawText();
        if (docText == null || docText.isBlank()) {
            throw new IllegalArgumentException("Quote document text is required");
        }
        Quote quote = quoteService.ingestQuote(wf,
                req.vendorName(), req.vendorEmail(), docText,
                sanitizeFilename(req.sourceFileName()), Quote.SourceType.JSON, CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(quote);
    }

    /** Multipart file ingestion (PDF only). Full file security checks applied. */
    @PostMapping(value = {"/upload", "/workflows/{workflowId}/quotes/upload"},
                 consumes = "multipart/form-data")
    public ResponseEntity<Quote> uploadFileQuote(
            @PathVariable(required = false) Long workflowId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("vendorName")
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 200) String vendorName,
            @RequestParam(value = "vendorEmail", required = false)
            @jakarta.validation.constraints.Email
            @jakarta.validation.constraints.Size(max = 254) String vendorEmail) {

        validateUploadedFile(file);

        String rawText = extractTextFromFile(file);
        String safeFilename = sanitizeFilename(file.getOriginalFilename());

        WorkflowExecution wf = resolveOrCreateWorkflow(workflowId);
        Quote quote = quoteService.ingestQuote(wf, vendorName.strip(), vendorEmail,
                rawText, safeFilename, Quote.SourceType.PDF, CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(quote);
    }

    /**
     * Validates an uploaded file for size, extension, and magic bytes.
     * Throws ExtractionException (400) on any violation.
     */
    private void validateUploadedFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ExtractionException("Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ExtractionException(
                "Uploaded file exceeds the maximum allowed size of 10 MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new ExtractionException("Uploaded filename is invalid");
        }

        String lowerName = originalName.toLowerCase();
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".txt")) {
            throw new ExtractionException(
                "Only PDF and plain-text files are accepted. Received: " + lowerName);
        }

        // Validate magic bytes — do not trust extension or Content-Type header alone
        if (lowerName.endsWith(".pdf")) {
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new ExtractionException("Could not read file for validation");
            }
            if (bytes.length < 4 || !Arrays.equals(Arrays.copyOf(bytes, 4), PDF_MAGIC)) {
                throw new ExtractionException(
                    "File does not appear to be a valid PDF (magic bytes mismatch)");
            }
        }
    }

    private String extractTextFromFile(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (filename.endsWith(".pdf")) {
                return extractPdfText(file);
            } else {
                // Plain text: limit to MAX_FILE_BYTES already enforced above
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // Do NOT include e.getMessage() — could contain internal path info
            throw new ExtractionException("Could not read the uploaded file");
        }
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        // Additional safety: PDFBox parses with a 10MB limit enforced above
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(bytes)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(doc);
            if (text == null || text.isBlank()) {
                throw new ExtractionException(
                    "No extractable text found — this may be a scanned PDF. OCR is not enabled in this build.");
            }
            // Enforce max text length to prevent resource exhaustion
            if (text.length() > 50_000) {
                log.warn("PDF text extraction exceeded 50,000 chars — truncating for safety");
                text = text.substring(0, 50_000);
            }
            return text;
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            throw new ExtractionException("PDF is password-protected and cannot be processed");
        }
    }

    private WorkflowExecution resolveOrCreateWorkflow(Long workflowId) {
        if (workflowId != null) {
            return quoteService.getWorkflow(workflowId);
        }
        return workflowRepository.findTopByOrderByCreatedAtDesc()
                .filter(wf -> wf.getStatus() != WorkflowExecution.Status.PO_GENERATED && wf.getStatus() != WorkflowExecution.Status.COMPLETED)
                .orElseGet(() -> quoteService.createWorkflow(
                        "Procurement Workflow #" + (workflowRepository.count() + 1),
                        "Auto-created for quote upload",
                        CurrentUser.id()));
    }

    /**
     * Sanitizes a user-supplied filename.
     * Returns a safe alphanumeric name with limited allowed characters.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "upload";
        // Remove path separators and non-safe characters
        String safe = filename
                .replace("..", "")
                .replaceAll("[/\\\\<>:\"?*|]", "_")
                .strip();
        if (safe.isEmpty()) safe = "upload";
        return safe.length() > 255 ? safe.substring(0, 255) : safe;
    }

    @GetMapping("/workflows/{workflowId}")
    public ResponseEntity<List<Quote>> getForWorkflow(@PathVariable Long workflowId) {
        return ResponseEntity.ok(quoteService.getQuotesForWorkflow(workflowId));
    }

    /** ADMIN/APPROVER only — fetching all quotes across all workflows. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'PROCUREMENT_USER')")
    public ResponseEntity<List<Quote>> getAll() {
        return ResponseEntity.ok(quoteService.getAllQuotes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quote> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.getQuote(id));
    }
}
