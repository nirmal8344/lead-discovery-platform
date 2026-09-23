package com.leaddiscovery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    @Column(length = 255)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_page_id")
    private SourcePage sourcePage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public SourcePage getSourcePage() { return sourcePage; }
    public void setSourcePage(SourcePage sourcePage) { this.sourcePage = sourcePage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
