package br.com.AutomacaoDeLeads.scraper.repository;

import br.com.AutomacaoDeLeads.scraper.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    boolean existsByNameAndPhone(String name, String phone);

    // Retorna a contagem total de leads
    long count();

    // Retorna a contagem de leads criados hoje
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.createdAt >= CURRENT_DATE")
    long countTodayLeads();

    // Retorna a lista de categorias e quantos leads cada uma tem
    @Query("SELECT l.category, COUNT(l) FROM Lead l GROUP BY l.category")
    List<Object[]> countLeadsByCategory();
}