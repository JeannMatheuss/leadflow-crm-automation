package br.com.AutomacaoDeLeads.scraper.repository;

import br.com.AutomacaoDeLeads.scraper.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    // Verifica se já existe uma empresa com esse nome e telefone
    boolean existsByNameAndPhone(String name, String phone);
}