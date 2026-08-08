package com.procureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vendors")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vendor extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String contactEmail;

    private String address;

    private Double reliabilityScore = 75.0;

    private String gstNumber;

    private Boolean trustSealVerified = false;

    private Integer yearsExperience;

    private Double responseRate;

    private Integer pastClientsCount;

    private String legalStatus;

    private String annualTurnoverRange;

    private Integer complianceIssues = 0;

    private String category;

    private String location;
}
