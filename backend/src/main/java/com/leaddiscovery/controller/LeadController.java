package com.leaddiscovery.controller;

import com.leaddiscovery.dto.CategoryStatDto;
import com.leaddiscovery.dto.LeadDetailDto;
import com.leaddiscovery.dto.LeadFilterCriteria;
import com.leaddiscovery.dto.LeadListItemDto;
import com.leaddiscovery.entity.enums.VerificationStatus;
import com.leaddiscovery.service.LeadExportService;
import com.leaddiscovery.service.LeadService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "createdAt", "businessName", "confidenceScore", "city", "category"
    );

    private final LeadService leadService;
    private final LeadExportService leadExportService;

    public LeadController(LeadService leadService, LeadExportService leadExportService) {
        this.leadService = leadService;
        this.leadExportService = leadExportService;
    }

    @GetMapping
    public ResponseEntity<Page<LeadListItemDto>> listLeads(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 100));

        String validSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(direction, validSortBy));

        LeadFilterCriteria criteria = new LeadFilterCriteria(city, category, verificationStatus, search, null);
        return ResponseEntity.ok(leadService.listLeads(criteria, pageable));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryStatDto>> getCategories() {
        return ResponseEntity.ok(leadService.getCategoryStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadDetailDto> getLead(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.getLeadDetails(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteLead(@PathVariable Long id) {
        leadService.deleteLead(id);
        return ResponseEntity.ok(Map.of(
                "message", "Lead deleted successfully",
                "leadId", id
        ));
    }

    /**
     * CSV export - returns RFC 4180 CSV with formula injection protection.
     */
    @GetMapping("/export")
    public void exportLeads(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) String search,
            HttpServletResponse response) throws IOException {

        String filename = buildFilename("leads_export", category, "csv");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        LeadFilterCriteria criteria = new LeadFilterCriteria(city, category, verificationStatus, search, null);
        leadExportService.exportLeadsToCsv(criteria, response.getWriter());
    }

    /**
     * Excel export - returns .xlsx with styled header, alternating rows.
     */
    @GetMapping("/export/excel")
    public void exportLeadsToExcel(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) String search,
            HttpServletResponse response) throws IOException {

        String filename = buildFilename("leads_export", category, "xlsx");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        LeadFilterCriteria criteria = new LeadFilterCriteria(city, category, verificationStatus, search, null);
        leadExportService.exportLeadsToExcel(criteria, response.getOutputStream());
    }

    private String buildFilename(String base, String category, String ext) {
        String date = java.time.LocalDate.now().toString();
        if (category != null && !category.isBlank()) {
            String safeCat = category.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
            return base + "_" + safeCat + "_" + date + "." + ext;
        }
        return base + "_" + date + "." + ext;
    }
}
