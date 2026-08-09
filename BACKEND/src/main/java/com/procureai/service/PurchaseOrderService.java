package com.procureai.service;

import com.procureai.entity.*;
import com.procureai.exception.BusinessRuleException;
import com.procureai.exception.NotFoundException;
import com.procureai.repository.PurchaseOrderRepository;
import com.procureai.service.email.EmailService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a real, dynamic Purchase Order (persisted entity + rendered PDF) from the
 * data of the final selected vendor's quote.
 */
@Service
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final QuoteCalculationService calculationService;
    private final ApprovalService approvalService;
    private final AuditService auditService;
    private final EmailService emailService;

    @Value("${app.po.output-dir:./po-output}")
    private String outputDir;

    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository, QuoteCalculationService calculationService,
                                 ApprovalService approvalService, AuditService auditService, EmailService emailService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.calculationService = calculationService;
        this.approvalService = approvalService;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    @Transactional
    public PurchaseOrder generate(Quote selectedQuote, WorkflowExecution workflow, Long userId, Long approvedNegotiationId) {
        if (approvedNegotiationId != null) {
            approvalService.assertApproved(Approval.ApprovalType.NEGOTIATION, approvedNegotiationId);
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(generatePoNumber());
        po.setWorkflow(workflow);
        po.setVendor(selectedQuote.getVendor());
        po.setSourceQuote(selectedQuote);
        po.setTotalAmount(selectedQuote.getCalculatedTotal());
        po.setShippingCost(selectedQuote.getShippingCost());
        po.setTaxAmount(calculationService.taxAmount(selectedQuote));
        po.setDiscountAmount(calculationService.discountAmount(selectedQuote));
        po.setWarrantyMonths(selectedQuote.getWarrantyMonths());
        po.setDeliveryDays(selectedQuote.getDeliveryDays());
        po.setPaymentTerms(selectedQuote.getPaymentTerms());
        po.setNotes("Generated automatically by ProcureAI from the completed procurement workflow \"" + workflow.getTitle() + "\".");
        po.setStatus(PurchaseOrder.Status.GENERATED);

        for (QuoteItem item : selectedQuote.getItems()) {
            PurchaseOrderItem poItem = new PurchaseOrderItem();
            poItem.setPurchaseOrder(po);
            poItem.setProductName(item.getProductName());
            poItem.setModel(item.getModel());
            poItem.setQuantity(item.getQuantity());
            poItem.setUnitPrice(item.getUnitPrice());
            poItem.setLineTotal(item.getLineSubtotal());
            po.getItems().add(poItem);
        }

        po = purchaseOrderRepository.save(po);

        try {
            String path = renderPdf(po);
            po.setPdfFilePath(path);
            po = purchaseOrderRepository.save(po);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to render Purchase Order PDF: " + e.getMessage());
        }

        auditService.log(workflow.getId(), userId, "PURCHASE_ORDER_GENERATED", "PurchaseOrder", po.getId(),
                "poNumber=" + po.getPoNumber() + " vendor=" + po.getVendor().getName() + " total=" + po.getTotalAmount());
        return po;
    }

    @Transactional
    public PurchaseOrder ensurePdfGenerated(PurchaseOrder po) {
        try {
            String path = renderPdf(po);
            po.setPdfFilePath(path);
            return purchaseOrderRepository.save(po);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to render Purchase Order PDF: " + e.getMessage());
        }
    }

    @Transactional
    public PurchaseOrder sendPoEmail(Long purchaseOrderId, Long userId) {
        PurchaseOrder po = get(purchaseOrderId);
        String vendorEmail = po.getVendor().getContactEmail();
        String to = (vendorEmail != null && !vendorEmail.isBlank()) ? vendorEmail : "vendor@example-demo.com";

        String subject = "Official Purchase Order: " + po.getPoNumber() + " - ProcureAI";
        String body = """
                Dear %s Team,

                Please find attached the official Purchase Order %s for your confirmed quotation.

                Order Summary:
                - PO Number: %s
                - Total Amount: Rs. %s
                - Payment Terms: %s
                - Delivery Days: %s

                Please confirm receipt of this purchase order.

                Best regards,
                Procurement Team
                """.formatted(
                po.getVendor().getName(), po.getPoNumber(), po.getPoNumber(),
                po.getTotalAmount(), po.getPaymentTerms() != null ? po.getPaymentTerms() : "Net 30",
                po.getDeliveryDays() != null ? po.getDeliveryDays() : 7
        );

        EmailMessage emailMsg = emailService.sendEmailDetails(to, subject, body, null, po.getId());
        if (emailMsg.getStatus() == EmailMessage.Status.FAILED) {
            log.warn("Failed to dispatch PO email for {}: {}", po.getPoNumber(), emailMsg.getErrorMessage());
        }

        po.setStatus(PurchaseOrder.Status.ISSUED);
        po = purchaseOrderRepository.save(po);

        auditService.log(po.getWorkflow().getId(), userId, "PURCHASE_ORDER_ISSUED_EMAIL", "PurchaseOrder", po.getId(),
                "poNumber=" + po.getPoNumber() + " emailId=" + emailMsg.getId());
        return po;
    }

    private String generatePoNumber() {
        String candidate;
        do {
            candidate = "PO-" + LocalDate.now().getYear() + "-" + SEQ.incrementAndGet();
        } while (purchaseOrderRepository.existsByPoNumber(candidate));
        return candidate;
    }

    private String renderPdf(PurchaseOrder po) throws IOException {
        Files.createDirectories(Path.of(outputDir));
        String fileName = po.getPoNumber() + ".pdf";
        Path filePath = Path.of(outputDir, fileName);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                var bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                var regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                y = writeLine(cs, bold, 18, margin, y, "PURCHASE ORDER");
                y = writeLine(cs, regular, 10, margin, y - 6, "ProcureAI — AI Procurement Platform");
                y -= 14;

                y = writeLine(cs, bold, 11, margin, y, "PO Number: " + po.getPoNumber());
                y = writeLine(cs, regular, 11, margin, y, "Date: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                y = writeLine(cs, regular, 11, margin, y, "Buyer: ProcureAI Demo Company Pvt. Ltd.");
                y -= 10;

                y = writeLine(cs, bold, 11, margin, y, "Vendor");
                y = writeLine(cs, regular, 11, margin, y, po.getVendor().getName());
                if (po.getVendor().getAddress() != null) {
                    y = writeLine(cs, regular, 11, margin, y, po.getVendor().getAddress());
                }
                if (po.getVendor().getContactEmail() != null) {
                    y = writeLine(cs, regular, 11, margin, y, po.getVendor().getContactEmail());
                }
                y -= 10;

                y = writeLine(cs, bold, 11, margin, y, "Items");
                for (PurchaseOrderItem item : po.getItems()) {
                    String line = String.format("%s (%s) x%d @ Rs.%s = Rs.%s",
                            item.getProductName(), item.getModel() == null ? "-" : item.getModel(),
                            item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
                    y = writeLine(cs, regular, 10, margin, y, line);
                }
                y -= 10;

                y = writeLine(cs, regular, 11, margin, y, "Discount: Rs." + nz(po.getDiscountAmount()));
                y = writeLine(cs, regular, 11, margin, y, "Tax: Rs." + nz(po.getTaxAmount()));
                y = writeLine(cs, regular, 11, margin, y, "Shipping: Rs." + nz(po.getShippingCost()));
                y = writeLine(cs, bold, 12, margin, y, "Total: Rs." + nz(po.getTotalAmount()));
                y -= 10;

                y = writeLine(cs, regular, 11, margin, y, "Warranty: " + (po.getWarrantyMonths() == null ? "-" : po.getWarrantyMonths() + " months"));
                y = writeLine(cs, regular, 11, margin, y, "Delivery: " + (po.getDeliveryDays() == null ? "-" : po.getDeliveryDays() + " days"));
                y = writeLine(cs, regular, 11, margin, y, "Payment Terms: " + (po.getPaymentTerms() == null ? "-" : po.getPaymentTerms()));
                y -= 10;

                if (po.getNotes() != null) {
                    y = writeLine(cs, regular, 9, margin, y, "Notes: " + po.getNotes());
                }
            }

            document.save(filePath.toFile());
        }
        return filePath.toString();
    }

    private float writeLine(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - (size + 6);
    }

    private String sanitize(String text) {
        return text.replace("₹", "Rs.").replaceAll("[^\\x00-\\xFF]", "?");
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public List<PurchaseOrder> all() {
        Long userId = com.procureai.util.CurrentUser.id();
        if (userId != null) {
            return purchaseOrderRepository.findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(userId);
        }
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder get(Long id) {
        Long userId = com.procureai.util.CurrentUser.id();
        if (userId != null) {
            return purchaseOrderRepository.findByIdAndWorkflowCreatedByUserId(id, userId)
                    .orElseThrow(() -> new NotFoundException("Purchase order not found or access denied: " + id));
        }
        return purchaseOrderRepository.findById(id).orElseThrow(() -> new NotFoundException("Purchase order not found: " + id));
    }
}
