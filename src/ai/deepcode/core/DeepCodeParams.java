package ai.deepcode.core;

import org.jetbrains.annotations.NotNull;
import ai.deepcode.javaclient.core.DeepCodeParamsBase;

public final class DeepCodeParams extends DeepCodeParamsBase {

  private DeepCodeParams() {
    // TODO 
    super(true, "https://www.deepcode.ai/", false, 1, "", "", "Eclipse");
  }

  private static final DeepCodeParams INSTANCE = new DeepCodeParams();

  public static DeepCodeParams getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean consentGiven(@NotNull Object project) {
    // TODO 
    return true;
  }

  @Override
  public void setConsentGiven(@NotNull Object project) {
    // TODO Auto-generated method stub
  }

}
