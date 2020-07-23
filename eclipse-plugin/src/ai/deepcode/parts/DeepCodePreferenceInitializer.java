package ai.deepcode.parts;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class DeepCodePreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = DeepCodePrefPage.PREFS_STORE_INSTANCE;
    store.setDefault(DeepCodePrefPage.BASE_URL, "https://www.deepcode.ai/");
  }

}
