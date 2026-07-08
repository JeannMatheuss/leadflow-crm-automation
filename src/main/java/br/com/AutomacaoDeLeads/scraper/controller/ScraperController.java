package br.com.AutomacaoDeLeads.scraper.controller;

import br.com.AutomacaoDeLeads.scraper.model.Lead;
import br.com.AutomacaoDeLeads.scraper.repository.LeadRepository;

import br.com.AutomacaoDeLeads.scraper.service.ScraperService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads") // Mudamos o nome para algo mais profissional
@CrossOrigin("*")
public class ScraperController {

    private final ScraperService scraperService;
    private final LeadRepository leadRepository;

    public ScraperController(ScraperService scraperService, LeadRepository leadRepository) {
        this.scraperService = scraperService;
        this.leadRepository = leadRepository;
    }

    // Endpoint 1: Inicia a busca
    @PostMapping("/search")
    public String startSearch(@RequestParam String query) {
        scraperService.scrapeGoogleMaps(query);
        return "Busca iniciada para '" + query + "'. Os dados aparecerão no banco em instantes.";
    }

    // Endpoint 2: Lista todos os leads salvos
    @GetMapping
    public List<Lead> getAllLeads() {
        return leadRepository.findAll();
    }

    // Endpoint 3: Deleta um lead
    @DeleteMapping("/{id}")
    public void deleteLead(@PathVariable Long id) {
        leadRepository.deleteById(id);
    }
}
