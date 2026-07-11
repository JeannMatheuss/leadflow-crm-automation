package br.com.AutomacaoDeLeads.scraper.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class LeadStatsDTO {
    private long totalLeads;
    private long todayLeads;
    private Map<String, Long> categoryDistribution;
}
