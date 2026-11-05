package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.domain.EmailEvent.EmailType;

record TrialEmailContent(
    String subject, String tagline, String body, String button, String link, String help) {

  private static final String P = "<p style=\"font-size:15px;line-height:1.6;color:#475569;\">";
  private static final String LIST =
      "<ol style=\"font-size:15px;line-height:1.7;color:#475569;padding-left:20px;\">";
  private static final String BULLETS =
      "<ul style=\"font-size:15px;line-height:1.7;color:#475569;padding-left:20px;\">";

  static TrialEmailContent of(EmailType type, String frontendUrl) {
    String login = frontendUrl + "/login";
    String billing = frontendUrl + "/billing";
    return switch (type) {
      case TRIAL_FIRST_SALE ->
          new TrialEmailContent(
              "Sua primeira venda no BiniTech PDV em 3 passos",
              "Seu teste grátis está rodando",
              P
                  + "Sua loja ainda não registrou nenhuma venda. Dá para fazer a primeira em poucos minutos:</p>"
                  + LIST
                  + "<li>Cadastre um produto em <strong>Produtos</strong> ou importe a sua planilha de uma vez.</li>"
                  + "<li>Abra o <strong>Caixa</strong>, busque o produto ou leia o código de barras e finalize no dinheiro, Pix, cartão ou fiado.</li>"
                  + "<li>Veja a venda no <strong>Relatório</strong> do dia, com o lucro calculado.</li></ol>",
              "Fazer minha primeira venda",
              login,
              "Travou em algum passo?");
      case TRIAL_FEATURES ->
          new TrialEmailContent(
              "O que mais o BiniTech PDV faz pela sua loja",
              "Dia 3 do seu teste grátis",
              P
                  + "Além do caixa, estes recursos já estão liberados na sua conta durante o teste:</p>"
                  + BULLETS
                  + "<li><strong>Fiado</strong> com a lista de quem deve e quanto.</li>"
                  + "<li><strong>Delivery pelo WhatsApp</strong>, com cardápio online da sua loja.</li>"
                  + "<li><strong>NFC-e</strong> emitida direto na SEFAZ.</li>"
                  + "<li><strong>Estoque</strong> que baixa sozinho a cada venda.</li>"
                  + "<li><strong>Equipe</strong> com usuário e permissão para cada funcionário.</li></ul>",
              "Abrir o sistema",
              login,
              "Quer ajuda para configurar algum deles?");
      case TRIAL_ENDING ->
          new TrialEmailContent(
              "Seu teste grátis termina amanhã",
              "Último dia de teste amanhã",
              P
                  + "O teste grátis do BiniTech PDV termina amanhã. Para continuar vendendo sem pausa,"
                  + " escolha um plano em <strong>Assinatura</strong>. Os planos começam em R$ 99 por mês,"
                  + " no Pix ou no cartão.</p>"
                  + P
                  + "Seus produtos, clientes e vendas continuam salvos.</p>",
              "Escolher meu plano",
              billing,
              "Ficou com dúvida sobre os planos?");
      case TRIAL_ENDED ->
          new TrialEmailContent(
              "Seu teste acabou e seus dados continuam aqui",
              "Seu teste grátis terminou",
              P
                  + "O teste grátis terminou e o caixa fica pausado até você escolher um plano."
                  + " Seus produtos, clientes e vendas continuam guardados: assim que o pagamento"
                  + " for confirmado, você volta a vender de onde parou.</p>",
              "Escolher meu plano",
              billing,
              "O sistema não serviu para a sua loja? Queremos entender o motivo.");
      default -> null;
    };
  }
}
