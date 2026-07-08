package br.com.AutomacaoDeLeads.scraper.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Data
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String website;

    @Column(length = 500)
    private String address;

    private String category;

    private LocalDateTime createdAt = LocalDateTime.now();
}