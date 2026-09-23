package com.leaddiscovery.service;

import com.leaddiscovery.dto.LeadExportRowDto;
import com.leaddiscovery.dto.LeadFilterCriteria;
import com.leaddiscovery.entity.Organization;
import com.leaddiscovery.repository.OrganizationRepository;
import com.leaddiscovery.repository.OrganizationSpecification;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

@Service
public class LeadExportService {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_EXPORT_LIMIT = 10000;

    private final OrganizationRepository organizationRepository;
    private final SecurityUtils securityUtils;

    public LeadExportService(OrganizationRepository organizationRepository, SecurityUtils securityUtils) {
        this.organizationRepository = organizationRepository;
        this.securityUtils = securityUtils;
    }

    // -----------------------------------------------------------------------
    // CSV Export (UTF-8 RFC 4180, formula injection protected)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public void exportLeadsToCsv(LeadFilterCriteria criteria, Writer writer) throws IOException {
        LeadFilterCriteria effectiveCriteria = applyOwnership(criteria);

        // Write CSV header
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < LeadExportRowDto.HEADERS.length; i++) {
            if (i > 0) header.append(',');
            header.append(escapeCsvCell(LeadExportRowDto.HEADERS[i]));
        }
        header.append("\r\n");
        writer.write(header.toString());

        int pageNumber = 0;
        int totalExported = 0;

        while (totalExported < MAX_EXPORT_LIMIT) {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by("id").descending());
            Page<Organization> page = organizationRepository.findAll(
                    OrganizationSpecification.withCriteria(effectiveCriteria), pageable);

            if (page.isEmpty()) break;

            for (Organization org : page.getContent()) {
                LeadExportRowDto row = LeadExportRowDto.fromEntity(org);
                String[] values = row.toArray();

                StringBuilder line = new StringBuilder();
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) line.append(',');
                    line.append(escapeCsvCell(values[i]));
                }
                line.append("\r\n");
                writer.write(line.toString());

                totalExported++;
                if (totalExported >= MAX_EXPORT_LIMIT) break;
            }

            if (!page.hasNext()) break;
            pageNumber++;
        }

        writer.flush();
    }

    // -----------------------------------------------------------------------
    // Excel Export (.xlsx) using Apache POI SXSSF (streaming, memory-efficient)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public void exportLeadsToExcel(LeadFilterCriteria criteria, OutputStream outputStream) throws IOException {
        LeadFilterCriteria effectiveCriteria = applyOwnership(criteria);

        // SXSSFWorkbook keeps 100 rows in memory; rest flushed to disk
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("Leads");
            sheet.setDefaultColumnWidth(22);

            // Header style: dark background, white bold text
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBottomBorderColor(IndexedColors.WHITE.getIndex());

            // Alternating row style
            CellStyle evenRowStyle = workbook.createCellStyle();
            evenRowStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            evenRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            evenRowStyle.setBorderBottom(BorderStyle.THIN);
            evenRowStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

            CellStyle defaultRowStyle = workbook.createCellStyle();
            defaultRowStyle.setBorderBottom(BorderStyle.THIN);
            defaultRowStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

            // Header row (row 0)
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            String[] headers = LeadExportRowDto.HEADERS;
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIndex = 1;
            int totalExported = 0;
            int pageNumber = 0;

            while (totalExported < MAX_EXPORT_LIMIT) {
                Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by("id").descending());
                Page<Organization> page = organizationRepository.findAll(
                        OrganizationSpecification.withCriteria(effectiveCriteria), pageable);

                if (page.isEmpty()) break;

                for (Organization org : page.getContent()) {
                    LeadExportRowDto rowDto = LeadExportRowDto.fromEntity(org);
                    String[] values = rowDto.toArray();

                    Row row = sheet.createRow(rowIndex);
                    row.setHeightInPoints(18);
                    CellStyle rowStyle = (rowIndex % 2 == 0) ? evenRowStyle : defaultRowStyle;

                    for (int col = 0; col < values.length; col++) {
                        Cell cell = row.createCell(col);
                        String val = values[col];
                        cell.setCellValue(val != null ? val : "");
                        cell.setCellStyle(rowStyle);
                    }

                    rowIndex++;
                    totalExported++;
                    if (totalExported >= MAX_EXPORT_LIMIT) break;
                }

                if (!page.hasNext()) break;
                pageNumber++;
            }

            // Freeze header row
            sheet.createFreezePane(0, 1);

            workbook.write(outputStream);
            workbook.dispose(); // clean up temp files
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private LeadFilterCriteria applyOwnership(LeadFilterCriteria criteria) {
        LeadFilterCriteria effective = criteria != null ? criteria : new LeadFilterCriteria();
        if (!securityUtils.isAdmin()) {
            UserPrincipal principal = securityUtils.getRequiredCurrentUserPrincipal();
            effective.setUserId(principal.getId());
        }
        return effective;
    }

    public static String escapeCsvCell(String value) {
        if (value == null) {
            return "\"\"";
        }
        String safeValue = sanitizeFormulaInjection(value.trim());
        // Always quote to keep RFC 4180 consistent
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    public static String sanitizeFormulaInjection(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        char firstChar = input.charAt(0);
        if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@'
                || firstChar == '\t' || firstChar == '\r') {
            return "'" + input;
        }
        return input;
    }
}
