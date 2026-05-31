package com.sara.superadmin.service;

import com.sara.superadmin.model.AiProviderConfig;
import com.sara.superadmin.model.Incident;
import com.sara.superadmin.model.IncidentAnalysis;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Sends RCA incident alert emails from the super-admin (central). SMTP + recipients are
 * read from the DB config (UI-editable); if the DB has no SMTP, falls back to the static
 * property-configured {@link JavaMailSender} and {@code rca.alert.*} properties.
 */
@Service
public class RcaAlertEmailService {

	private static final Logger log = LoggerFactory.getLogger(RcaAlertEmailService.class);

	private final JavaMailSender defaultMailSender;
	private final TemplateEngine templateEngine;
	private final AiProviderConfigService configService;
	private final String propAlertTo;
	private final String propAlertFrom;

	public RcaAlertEmailService(JavaMailSender defaultMailSender,
								TemplateEngine templateEngine,
								AiProviderConfigService configService,
								@Value("${rca.alert.to}") String propAlertTo,
								@Value("${rca.alert.from}") String propAlertFrom) {
		this.defaultMailSender = defaultMailSender;
		this.templateEngine = templateEngine;
		this.configService = configService;
		this.propAlertTo = propAlertTo;
		this.propAlertFrom = propAlertFrom;
	}

	/** @return true if the email was sent. */
	public boolean sendIncidentAlert(Incident incident, IncidentAnalysis analysis) {
		AiProviderConfig cfg = configService.getOrEmpty();
		String to = (cfg.getAlertToEmails() != null && !cfg.getAlertToEmails().isBlank())
				? cfg.getAlertToEmails() : propAlertTo;
		String from = (cfg.getSmtpFrom() != null && !cfg.getSmtpFrom().isBlank())
				? cfg.getSmtpFrom() : propAlertFrom;
		if (to == null || to.isBlank()) {
			log.warn("RCA alert skipped: no recipient configured");
			return false;
		}

		try {
			JavaMailSender sender = resolveSender(cfg);
			Context ctx = new Context();
			ctx.setVariable("storeName", incident.getStoreName());
			ctx.setVariable("storeId", incident.getStoreId());
			ctx.setVariable("endpoint", incident.getApiEndpoint());
			ctx.setVariable("method", incident.getHttpMethod());
			ctx.setVariable("statusCode", incident.getStatusCode());
			ctx.setVariable("errorFlag", incident.getErrorFlag());
			ctx.setVariable("errorMessage", incident.getErrorMessage());
			ctx.setVariable("occurredAt", incident.getOccurredAt() == null ? "—"
					: DateTimeFormatter.ISO_INSTANT.format(incident.getOccurredAt()));
			ctx.setVariable("rootCause", analysis.getRootCause());
			ctx.setVariable("suggestedFix", analysis.getSuggestedFix());
			ctx.setVariable("confidence", analysis.getConfidence());
			ctx.setVariable("provider", analysis.getProviderUsed());
			ctx.setVariable("model", analysis.getModelUsed());

			String html = templateEngine.process("emails/rca-alert", ctx);

			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(from);
			helper.setTo(to.split("\\s*,\\s*"));
			helper.setSubject("[RCA] " + incident.getStoreName() + " — " + incident.getApiEndpoint()
					+ " (" + incident.getStatusCode() + ")");
			helper.setText(html, true);
			sender.send(message);
			log.info("RCA alert email sent to {} for incident {}", to, incident.getId());
			return true;
		} catch (Exception e) {
			log.error("Failed to send RCA alert email for incident {}: {}", incident.getId(), e.getMessage());
			return false;
		}
	}

	/** DB SMTP if present, else the property-configured default sender. */
	private JavaMailSender resolveSender(AiProviderConfig cfg) {
		if (cfg.getSmtpHost() == null || cfg.getSmtpHost().isBlank()
				|| cfg.getSmtpPassword() == null || cfg.getSmtpPassword().isBlank()) {
			return defaultMailSender;
		}
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(cfg.getSmtpHost());
		sender.setPort(cfg.getSmtpPort() != null ? cfg.getSmtpPort() : 465);
		sender.setUsername(cfg.getSmtpUsername());
		sender.setPassword(cfg.getSmtpPassword());
		Properties p = sender.getJavaMailProperties();
		p.put("mail.smtp.auth", "true");
		if (cfg.isSmtpSslEnabled()) {
			p.put("mail.smtp.ssl.enable", "true");
			p.put("mail.smtp.ssl.required", "true");
		} else {
			p.put("mail.smtp.starttls.enable", "true");
		}
		return sender;
	}
}
