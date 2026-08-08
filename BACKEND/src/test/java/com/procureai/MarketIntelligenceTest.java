package com.procureai;

import com.procureai.entity.Quote;
import com.procureai.entity.QuoteItem;
import com.procureai.entity.Vendor;
import com.procureai.entity.WorkflowExecution;
import com.procureai.repository.QuoteRepository;
import com.procureai.repository.VendorRepository;
import com.procureai.repository.WorkflowExecutionRepository;
import com.procureai.service.MarketIntelligenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
@Transactional
@DisplayName("Market Intelligence Dynamic Dataset Integration Test")
class MarketIntelligenceTest {

    @Autowired
    private MarketIntelligenceService marketIntelligenceService;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private WorkflowExecutionRepository workflowRepository;

    @Test
    @DisplayName("Base categories return default benchmark data when DB is empty")
    void testDefaultCategories() {
        Map<String, MarketIntelligenceService.MarketData> all = marketIntelligenceService.getAllCategories();
        assertThat(all).isNotNull();
        assertThat(all.size()).isGreaterThanOrEqualTo(8);
        assertThat(all).containsKey("thinkpad");
    }

    @Test
    @DisplayName("Ingested quote dynamically updates market intelligence range and competitor insights")
    void testIngestedQuoteUpdatesMarketIntelligence() {
        WorkflowExecution wf = new WorkflowExecution();
        wf.setTitle("Test Laptop Workflow");
        wf.setStatus(WorkflowExecution.Status.PROCESSING);
        wf = workflowRepository.save(wf);

        Vendor v = new Vendor();
        v.setName("Lenovo Direct Sales");
        v.setContactEmail("sales@lenovo.demo");
        v = vendorRepository.save(v);

        Quote q = new Quote();
        q.setWorkflow(wf);
        q.setVendor(v);
        q.setCalculatedTotal(new BigDecimal("130000"));

        QuoteItem item = new QuoteItem();
        item.setQuote(q);
        item.setProductName("Lenovo ThinkPad P16");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("65000"));
        q.getItems().add(item);

        quoteRepository.save(q);

        Map<String, MarketIntelligenceService.MarketData> all = marketIntelligenceService.getAllCategories();
        assertThat(all).containsKey("thinkpad");

        MarketIntelligenceService.MarketData thinkpadData = all.get("thinkpad");
        assertThat(thinkpadData).isNotNull();
        assertThat(thinkpadData.competitorInsights()).anyMatch(c -> c.contains("Lenovo Direct Sales") && c.contains("65000"));
    }

    @Test
    @DisplayName("Custom uploaded category generates dynamic market card")
    void testCustomCategoryCreation() {
        WorkflowExecution wf = new WorkflowExecution();
        wf.setTitle("Test Quantum Workflow");
        wf.setStatus(WorkflowExecution.Status.PROCESSING);
        wf = workflowRepository.save(wf);

        Vendor v = new Vendor();
        v.setName("Quantum Systems");
        v.setContactEmail("info@quantumsys.demo");
        v = vendorRepository.save(v);

        Quote q = new Quote();
        q.setWorkflow(wf);
        q.setVendor(v);

        QuoteItem item = new QuoteItem();
        item.setQuote(q);
        item.setProductName("Quantum Supercomputer Rack");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("1500000"));
        q.getItems().add(item);

        quoteRepository.save(q);

        Map<String, MarketIntelligenceService.MarketData> all = marketIntelligenceService.getAllCategories();
        assertThat(all.values()).anyMatch(m -> m.category().contains("Quantum") || m.competitorInsights().stream().anyMatch(ci -> ci.contains("Quantum Systems")));
    }
}
