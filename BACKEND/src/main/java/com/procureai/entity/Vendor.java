package com.procureai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vendors")
public class Vendor extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String contactEmail;

    private String address;

    /** 0-100 reliability score used in scoring engine; demo/seed value unless historical data exists. */
    private Double reliabilityScore = 75.0;
}
