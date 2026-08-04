package br.com.AutomacaoDeLeads.scraper.service;

import br.com.AutomacaoDeLeads.scraper.repository.LeadRepository;
import com.microsoft.playwright.*;
import br.com.AutomacaoDeLeads.scraper.model.Lead;
import com.microsoft.playwright.options.WaitUntilState;
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
        // Criamos um contexto com um User-Agent de navegador real para evitar bloqueios
        try (BrowserContext context = page.context().browser().newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .setIgnoreHTTPSErrors(true))) { // Ignora erros de certificado SSL

            Page webPage = context.newPage();
            try {
                // Aumentamos o timeout para 15 segundos e esperamos o DOM carregar
                webPage.navigate(lead.getWebsite(), new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(15000));

                // Espera um pouquinho para scripts carregarem
                webPage.waitForTimeout(2000);

                // EXTRAÇÃO DE INSTAGRAM
                Locator insta = webPage.locator("a[href*='instagram.com']").first();
                if (insta.count() > 0) {
                    lead.setInstagram(insta.getAttribute("href"));
                }

                // EXTRAÇÃO DE FACEBOOK
                Locator face = webPage.locator("a[href*='facebook.com']").first();
                if (face.count() > 0) {
                    lead.setFacebook(face.getAttribute("href"));
                }

                // EXTRAÇÃO DE EMAIL (Melhorado com regex mais forte)
                String content = webPage.content();
                Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)");
                Matcher matcher = emailPattern.matcher(content);
                if (matcher.find()) {
                    lead.setEmail(matcher.group(1));
                }

            } catch (Exception e) {
                // Se der timeout, apenas logamos, mas não travamos o robô
                System.err.println("Timeout ou Bloqueio ao acessar: " + lead.getWebsite());
            } finally {
                webPage.close();
            }
        } catch (Exception e) {
            System.err.println("Erro ao criar contexto de navegação: " + e.getMessage());
        }
    }

    private void parseAddress(String fullAddress, Lead lead) {
        if (fullAddress == null || fullAddress.isEmpty()) return;

        try {
            //Ex: "Av. Paulista, 1000 - Bela Vista, São Paulo - SP, 01310-100"

            // 1. Dividir por vírgulas e traços
            String[] parts = fullAddress.split(",");

            for (String part : parts) {
                if (part.contains("-")) {
                    String[] cityState = part.split("-");
                    if (cityState.length >= 2) {
                        String possibleState = cityState[cityState.length - 1].trim();

                        // Verifica se é uma sigla de estado (2 letras)
                        if (possibleState.length() == 2 && possibleState.matches("[A-Z]{2}")) {
                            lead.setState(possibleState);
                            lead.setCity(cityState[cityState.length - 2].trim());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar endereço: " + fullAddress);
        }
    }
}