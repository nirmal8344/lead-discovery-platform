package com.leaddiscovery.controller;

import com.leaddiscovery.dto.LeadDiscoveryRequest;
import com.leaddiscovery.dto.LeadDiscoveryResponse;
import com.leaddiscovery.service.LeadDiscoveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final LeadDiscoveryService leadDiscoveryService;
    private final com.leaddiscovery.service.LeadDiscoveryPipelineService pipelineService;

    public DiscoveryController(LeadDiscoveryService leadDiscoveryService,
                               com.leaddiscovery.service.LeadDiscoveryPipelineService pipelineService) {
        this.leadDiscoveryService = leadDiscoveryService;
        this.pipelineService = pipelineService;
    }

    @PostMapping("/search")
    public ResponseEntity<LeadDiscoveryResponse> searchBusinesses(@Valid @RequestBody LeadDiscoveryRequest request) {
        LeadDiscoveryResponse response = leadDiscoveryService.discoverLeads(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pipeline")
    public ResponseEntity<com.leaddiscovery.dto.PipelineDiscoveryResponse> executePipeline(
            @Valid @RequestBody com.leaddiscovery.dto.PipelineDiscoveryRequest request) {
        com.leaddiscovery.dto.PipelineDiscoveryResponse response = pipelineService.executePipeline(request);
        return ResponseEntity.ok(response);
    }
}
