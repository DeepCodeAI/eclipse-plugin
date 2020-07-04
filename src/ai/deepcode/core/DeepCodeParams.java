package ai.deepcode.core;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.prefs.BackingStoreException;
import ai.deepcode.javaclient.DeepCodeRestApi;
import ai.deepcode.javaclient.core.DeepCodeParamsBase;

public final class DeepCodeParams extends DeepCodeParamsBase {

  // private final IPreferencesService globalPreferences = Platform.getPreferencesService();
  private final IEclipsePreferences globalPreferences = InstanceScope.INSTANCE.getNode("ai.deepcode");

  private DeepCodeParams() {
    // TODO
    super(InstanceScope.INSTANCE.getNode("ai.deepcode").getBoolean("isEnable", true),
        InstanceScope.INSTANCE.getNode("ai.deepcode").get("apiUrl", "https://www.deepcode.ai/"),
        InstanceScope.INSTANCE.getNode("ai.deepcode").getBoolean("useLinter", false),
        InstanceScope.INSTANCE.getNode("ai.deepcode").getInt("minSeverity", 1),
        InstanceScope.INSTANCE.getNode("ai.deepcode").get("sessionToken", ""),
        InstanceScope.INSTANCE.getNode("ai.deepcode").get("loginUrl", ""), "Eclipse");
    DeepCodeRestApi.setBaseUrl(getApiUrl());
  }

  private static final DeepCodeParams INSTANCE = new DeepCodeParams();

  public static DeepCodeParams getInstance() {
    return INSTANCE;
  }

  private Map<IProject, IEclipsePreferences> project2preferences = new HashMap<>();

  private IEclipsePreferences getPojectPreferences(IProject project) {
    return project2preferences.computeIfAbsent(project, prj -> new ProjectScope(prj).getNode("ai.deepcode"));
  }

  @Override
  public void setSessionToken(String sessionToken) {
    super.setSessionToken(sessionToken);
    globalPreferences.put("sessionToken", sessionToken);
    savePreferences(globalPreferences);
  }

  @Override
  public void setLoginUrl(String loginUrl) {
    super.setLoginUrl(loginUrl);
    globalPreferences.put("loginUrl", loginUrl);
    savePreferences(globalPreferences);
  }

  @Override
  public void setUseLinter(boolean useLinter) {
    super.setUseLinter(useLinter);
    globalPreferences.putBoolean("useLinter", useLinter);
    savePreferences(globalPreferences);
  }

  @Override
  public void setMinSeverity(int minSeverity) {
    super.setMinSeverity(minSeverity);
    globalPreferences.putInt("minSeverity", minSeverity);
    savePreferences(globalPreferences);
  }

  @Override
  public void setApiUrl(@NotNull String apiUrl) {
    super.setApiUrl(apiUrl);
    globalPreferences.put("apiUrl", apiUrl);
    savePreferences(globalPreferences);
  }

  @Override
  public void setEnable(boolean isEnable) {
    super.setEnable(isEnable);
    globalPreferences.putBoolean("isEnable", isEnable);
    savePreferences(globalPreferences);
  }

  @Override
  public boolean consentGiven(@NotNull Object project) {
    return getPojectPreferences(PDU.toProject(project)).getBoolean("consentGiven", false);
  }

  @Override
  public void setConsentGiven(@NotNull Object project) {
    final IEclipsePreferences pojectPreferences = getPojectPreferences(PDU.toProject(project));
    pojectPreferences.putBoolean("consentGiven", true);
    savePreferences(pojectPreferences);
  }

  private void savePreferences(final IEclipsePreferences preferences) {
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      DCLogger.getInstance().logWarn(e.getMessage());
    }
  }

}
