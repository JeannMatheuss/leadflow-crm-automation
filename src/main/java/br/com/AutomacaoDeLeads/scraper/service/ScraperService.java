package br.com.AutomacaoDeLeads.scraper.service;

import br.com.AutomacaoDeLeads.scraper.repository.LeadRepository;
import com.microsoft.playwright.*;
import br.com.AutomacaoDeLeads.scraper.model.Lead;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScraperService {

    private final LeadRepository leadRepository;

    public ScraperService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Async
    public void scrapeGoogleMaps(String query) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate("https://www.google.com/maps/search/" + query.replace(" ", "+"));

            for (int i = 0; i < 3; i++) {
                page.mouse().wheel(0, 1000);
                page.waitForTimeout(2000);
            }

            Locator cards = page.locator("div[role='article']");
            int total = Math.min(cards.count(), 15);

            for (int i = 0; i < total; i++) {
                try {
                    cards.nth(i).click();
                    page.waitForTimeout(2000);

                    // Extração básica
                    String name = page.locator("h1.DUwDvf").innerText();
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

                        // Website e Endereço
                        Locator webLoc = page.locator("a[data-item-id='authority']");
                        if (webLoc.isVisible()) lead.setWebsite(webLoc.getAttribute("href"));

                        Locator addressLoc = page.locator("button[data-item-id='address']");
                        if (addressLoc.isVisible()) lead.setAddress(addressLoc.innerText());

                        // --- ENRIQUECIMENTO ---
                        if (lead.getWebsite() != null) {
                            enrichLeadData(page, lead);
                        }

                        leadRepository.save(lead);
                        System.out.println("Salvo e Enriquecido: " + name);
                    } else {
                        System.out.println("Já existe: " + name);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar item " + i + ": " + e.getMessage());
                }
            }
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enrichLeadData(Page page, Lead lead) {
        try {
            Page webPage = page.context().newPage();
            webPage.navigate(lead.getWebsite(), new Page.NavigateOptions().setTimeout(10000));

            // Instagram
            Locator insta = webPage.locator("a[href*='instagram.com']").first();
            if (insta.count() > 0) lead.setInstagram(insta.getAttribute("href"));

            // Facebook
            Locator face = webPage.locator("a[href*='facebook.com']").first();
            if (face.count() > 0) lead.setFacebook(face.getAttribute("href"));

            // Email via Regex
            String content = webPage.content();
            Pattern emailPattern = Pattern.compile("[a-zA-Z0-9.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z0-9.-]+");
            Matcher matcher = emailPattern.matcher(content);
            if (matcher.find()) {
                lead.setEmail(matcher.group());
            }

            webPage.close();
        } catch (Exception e) {
            System.err.println("Erro no enriquecimento: " + lead.getWebsite());
        }
    }
}