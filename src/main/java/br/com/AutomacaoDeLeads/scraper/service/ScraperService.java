package br.com.AutomacaoDeLeads.scraper.service;

import br.com.AutomacaoDeLeads.scraper.repository.LeadRepository;
import com.microsoft.playwright.*;
import br.com.AutomacaoDeLeads.scraper.model.Lead;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScraperService {

    private final LeadRepository leadRepository;

    public ScraperService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Async // <--- Isso faz o método rodar em uma thread separada
    public void scrapeGoogleMaps(String query) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)); // Headless true para ser mais rápido
            Page page = browser.newPage();
            page.navigate("https://www.google.com/maps/search/" + query.replace(" ", "+"));

            for (int i = 0; i < 3; i++) {
                page.mouse().wheel(0, 1000);
                page.waitForTimeout(2000);
            }

            Locator cards = page.locator("div[role='article']");
            int total = Math.min(cards.count(), 15); // Aumentamos para 15 leads

            for (int i = 0; i < total; i++) {
                try {
                    cards.nth(i).click();
                    page.waitForTimeout(2000);

                    String name = page.locator("h1.DUwDvf").innerText();

                    // Lógica de Telefone
                    String phone = null;
                    Locator phoneLoc = page.locator("button[data-item-id^='phone:tel']");
                    if (phoneLoc.isVisible()) {
                        phone = phoneLoc.getAttribute("data-item-id").replace("phone:tel:", "");
                    }

                    // VERIFICAÇÃO DE DUPLICADO
                    if (!leadRepository.existsByNameAndPhone(name, phone)) {
                        Lead lead = new Lead();
                        lead.setName(name);
                        lead.setPhone(phone);
                        lead.setCategory(query);

                        Locator webLoc = page.locator("a[data-item-id='authority']");
                        if (webLoc.isVisible()) lead.setWebsite(webLoc.getAttribute("href"));

                        Locator addressLoc = page.locator("button[data-item-id='address']");
                        if (addressLoc.isVisible()) lead.setAddress(addressLoc.innerText());

                        leadRepository.save(lead); // SALVA IMEDIATAMENTE
                        System.out.println("Salvo: " + name);
                    } else {
                        System.out.println("Já existe: " + name);
                    }
                } catch (Exception e) {
                    System.err.println("Erro no item " + i);
                }
            }
            browser.close();
            System.out.println("Tarefa concluída para: " + query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
