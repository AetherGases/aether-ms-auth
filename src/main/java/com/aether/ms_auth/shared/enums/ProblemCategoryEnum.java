package com.aether.ms_auth.shared.enums;

public enum ProblemCategoryEnum {
  ERROR("Erro"),
  BUG("Bug"),
  LOGIN_AND_ACCOUNT("Login e conta"),
  INTERFACE_AND_NAVIGATION("Interface e navegação"),
  PERFORMANCE("Desempenho"),
  DATA_AND_INFORMATION("Dados e informações"),
  PAYMENT("Pagamento"),
  PRIVACY("Privacidade"),
  OTHERS("Outros");

  private final String label;

  ProblemCategoryEnum(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
