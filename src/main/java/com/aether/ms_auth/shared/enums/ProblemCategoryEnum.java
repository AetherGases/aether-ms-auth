package com.aether.ms_auth.shared.enums;

public enum ProblemCategoryEnum {
  ERROR,
  BUG,
  LOGIN_AND_ACCOUNT,
  INTERFACE_AND_NAVIGATION,
  PERFORMANCE,
  DATA_AND_INFORMATION,
  PAYMENT,
  PRIVACY,
  OTHERS;

  /** Chave do messages.properties com o rótulo da categoria, traduzido conforme o Accept-Language. */
  public String getMessageKey() {
    return "problem-category." + name();
  }
}
