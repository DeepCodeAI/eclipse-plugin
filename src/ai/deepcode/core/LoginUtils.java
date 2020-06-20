package ai.deepcode.core;

import ai.deepcode.javaclient.core.LoginUtilsBase;

public final class LoginUtils extends LoginUtilsBase {

  private static final LoginUtils INSTANCE = new LoginUtils();

  public static LoginUtils getInstance() {
    return INSTANCE;
  }

  private LoginUtils() {
    super(PDU.getInstance(), DeepCodeParams.getInstance(), AnalysisData.getInstance(), DCLogger.getInstance());
  }

  private static final String userAgent = "Eclipse";
  // TODO
  // + ApplicationNamesInfo.getInstance().getProductName()
  // + "-"
  // + ApplicationInfo.getInstance().getFullVersion();

  @Override
  protected String getUserAgent() {
    return userAgent;
  }
}
