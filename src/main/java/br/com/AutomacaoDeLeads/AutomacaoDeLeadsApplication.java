package br.com.AutomacaoDeLeads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AutomacaoDeLeadsApplication {
	public static void main(String[] args) {
		SpringApplication.run(AutomacaoDeLeadsApplication.class, args);
	}
}
