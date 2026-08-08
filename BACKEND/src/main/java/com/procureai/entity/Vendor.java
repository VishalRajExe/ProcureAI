package com.procureai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Vendor entity enhanced with intelligence fields adapted from the
 * AI-Powered-RFP-Analyzer's VendorEvaluationPlugin data model.
 */
@Getter
@Setter
@Entity
@Table(name = "vendors")
public class Vendor extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String contactEmail;

    private String address;

    /** 0-100 reliability score; used in deterministic scoring engine. */
    private Double reliabilityScore = 75.0;

    /** GST registration number for tax compliance verification. */
    private String gstNumber;

    /** Whether vendor has a verified TrustSeal (equivalent to BBB accreditation). */
    private Boolean trustSealVerified = false;

    /** Years in business — longer tenure = higher reputation score. */
    private Integer yearsExperience;

    /** Vendor's query response rate 0-100% */
    private Double responseRate;

    /** Number of past enterprise clients — used in reputation scoring. */
    private Integer pastClientsCount;

    /** Legal status: PROPRIETORSHIP, PARTNERSHIP, PVT_LTD, PUBLIC_LTD */
    private String legalStatus;

    /** Annual turnover range for financial stability assessment. */
    private String annualTurnoverRange;

    /** Known compliance issues or contract disputes count. */
    private Integer complianceIssues = 0;

    /** Industry/category this vendor specializes in. */
    private String category;

    /** Location city/state for logistics assessment. */
    private String location;
}
