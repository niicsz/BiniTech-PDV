package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.domain.EmailEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Sends transactional e-mail through Resend's HTTPS API. */
@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${RESEND_API_KEY:}')")
public class ResendEmailServiceAdapter {

  private static final Logger log = LoggerFactory.getLogger(ResendEmailServiceAdapter.class);

  private final RestClient resendClient;
  private final String fromAddress;
  private final String frontendUrl;

  public ResendEmailServiceAdapter(
      RestClient.Builder restClientBuilder,
      @Value("${resend.api-key}") String apiKey,
      @Value("${resend.from}") String fromAddress,
      @Value("${app.frontend-url}") String frontendUrl) {
    this.resendClient =
        restClientBuilder
            .baseUrl("https://api.resend.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.USER_AGENT, "BiniTech-PDV/1.0")
            .build();
    this.fromAddress = fromAddress;
    this.frontendUrl = frontendUrl;
  }

  public void send(EmailEvent event) {
    EmailContent content = emailContentFor(event);
    resendClient
        .post()
        .uri("/emails")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ResendEmailRequest(fromAddress, event.to(), content.subject(), content.html()))
        .retrieve()
        .toBodilessEntity();
    log.info("E-mail {} enviado pelo Resend para: {}", event.type(), event.to());
  }

  private EmailContent emailContentFor(EmailEvent event) {
    if (event.type() == EmailEvent.EmailType.PASSWORD_RESET) {
      return new EmailContent("🔑 Redefinição de senha — BiniTech PDV", buildResetHtml(event));
    }
    return new EmailContent("✅ Sua conta BiniTech PDV foi aprovada!", buildApprovalHtml(event));
  }

  private String buildResetHtml(EmailEvent event) {
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <body style="margin:0;padding:0;background:#f0f4f8;font-family:Arial,Helvetica,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f0f4f8;"><tr><td align="center" style="padding:40px 20px;">
            <table width="580" cellpadding="0" cellspacing="0" border="0" style="max-width:580px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(15,23,42,0.08);">
              <tr><td style="background:linear-gradient(135deg,#7c3aed,#4f46e5);padding:32px 40px;text-align:center;"><h1 style="margin:0;color:#ffffff;font-size:22px;">Redefinição de senha</h1></td></tr>
              <tr><td style="padding:32px 40px;color:#1e293b;">
                <p style="font-size:15px;line-height:1.6;color:#475569;">Recebemos um pedido para redefinir a senha do usuário <strong>%s</strong>. Clique no botão abaixo para criar uma nova senha. Este link expira em 1 hora.</p>
                <table cellpadding="0" cellspacing="0" border="0" align="center" style="margin:24px 0;"><tr><td style="border-radius:10px;background:#7c3aed;"><a href="%s" target="_blank" style="display:inline-block;padding:14px 32px;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;">Redefinir senha</a></td></tr></table>
                <p style="font-size:13px;color:#94a3b8;line-height:1.6;">Se você não solicitou esta alteração, ignore este e-mail — sua senha continua a mesma.</p>
              </td></tr>
              <tr><td style="padding:20px 40px;background:#f8fafc;text-align:center;color:#94a3b8;font-size:12px;">© BiniTech PDV — este é um e-mail automático, não responda.</td></tr>
            </table>
          </td></tr></table>
        </body>
        </html>
        """
        .formatted(event.username(), event.actionLink());
  }

  private String buildApprovalHtml(EmailEvent event) {
    String loginUrl = frontendUrl + "/login?tenant=" + event.tenantSlug();
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <body style="margin:0;padding:0;background:#f0f4f8;font-family:Arial,Helvetica,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f0f4f8;"><tr><td align="center" style="padding:40px 20px;">
            <table width="580" cellpadding="0" cellspacing="0" border="0" style="max-width:580px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(15,23,42,0.08);">
              <tr><td style="background:linear-gradient(135deg,#7c3aed,#4f46e5);padding:32px 40px;text-align:center;"><h1 style="margin:0;color:#ffffff;font-size:24px;">BiniTech PDV</h1><p style="margin:8px 0 0;color:#e0e7ff;font-size:14px;">Sua conta foi aprovada</p></td></tr>
              <tr><td style="padding:32px 40px;color:#1e293b;">
                <p style="font-size:16px;">Olá, <strong>%s</strong>!</p>
                <p style="font-size:15px;line-height:1.6;color:#475569;">A conta da sua empresa foi aprovada e já está ativa. Use as credenciais abaixo para acessar o sistema. Recomendamos alterar a senha no primeiro acesso.</p>
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f8fafc;border-radius:12px;margin:24px 0;"><tr><td style="padding:16px 20px;color:#475569;font-size:14px;"><strong>Usuário:</strong> %s<br /><strong>Senha temporária:</strong> %s</td></tr></table>
                <table cellpadding="0" cellspacing="0" border="0" align="center"><tr><td style="border-radius:10px;background:#7c3aed;"><a href="%s" target="_blank" style="display:inline-block;padding:14px 32px;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;">Acessar o sistema</a></td></tr></table>
              </td></tr>
              <tr><td style="padding:20px 40px;background:#f8fafc;text-align:center;color:#94a3b8;font-size:12px;">© BiniTech PDV — este é um e-mail automático, não responda.</td></tr>
            </table>
          </td></tr></table>
        </body>
        </html>
        """
        .formatted(event.tenantName(), event.username(), event.tempPassword(), loginUrl);
  }

  private record EmailContent(String subject, String html) {}

  private record ResendEmailRequest(String from, String to, String subject, String html) {}
}
