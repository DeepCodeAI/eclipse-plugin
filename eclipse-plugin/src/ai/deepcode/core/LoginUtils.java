package ai.deepcode.core;

import org.jetbrains.annotations.Nullable;
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

//  private static boolean isNewLoginRequstShown = false;
//
//  @Override
//  public boolean isLogged(@Nullable Object project, boolean userActionNeeded) {
//    final boolean isLogged = super.isLogged(project, userActionNeeded && !isNewLoginRequstShown);
//    if (!isLogged && userActionNeeded) // login request should be already shown, see super
//      isNewLoginRequstShown = true;
//    if (isLogged) // reset flag
//      isNewLoginRequstShown = false;
//    return isLogged;
//  }

  @Override
  protected String getUserAgent() {
    return userAgent;
  }
}
