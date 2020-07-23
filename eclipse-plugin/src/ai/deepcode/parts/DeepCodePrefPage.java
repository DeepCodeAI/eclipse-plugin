package ai.deepcode.parts;

import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import ai.deepcode.core.DeepCodeParams;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;


public class DeepCodePrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public static final String BASE_URL = "BASE_URL";
  public static final String TOKEN_ID = "TOKEN_ID";
  public static final String MIN_SEVERETY_LEVEL = "MIN_SEVERETY_LEVEL";
  public static final String ADD_LINTERS = "ADD_LINTERS";
  public static final IPreferenceStore PREFS_STORE_INSTANCE =
      new ScopedPreferenceStore(InstanceScope.INSTANCE, "ai.deepcode");

  public DeepCodePrefPage() {
    super(GRID);
  }

  @Override
  protected void createFieldEditors() {
    addField(new StringFieldEditor(BASE_URL, "DeepCode server instance &URL:", getFieldEditorParent()));
    addField(new StringFieldEditor(TOKEN_ID, "&TokenId:", getFieldEditorParent()));
    addField(new RadioGroupFieldEditor(
        MIN_SEVERETY_LEVEL, "Min &Severity level to show:", 1, new String[][] {
            {"&Infos, Warnings and Errors", "infos"}, {"&Warnings and Errors", "warns"}, {"&Errors only", "errors"}},
        getFieldEditorParent()));
    addField(new BooleanFieldEditor(ADD_LINTERS, "Add &Linters analysis", getFieldEditorParent()));
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(PREFS_STORE_INSTANCE);
    setDescription("Configuration for the DeepCode plugin");
    
    // read previously saved params
    PREFS_STORE_INSTANCE.setValue(BASE_URL, DeepCodeParams.getInstance().getApiUrl());
    PREFS_STORE_INSTANCE.setValue(TOKEN_ID, DeepCodeParams.getInstance().getSessionToken());
    final int minSeverity = DeepCodeParams.getInstance().getMinSeverity();
    PREFS_STORE_INSTANCE.setValue(MIN_SEVERETY_LEVEL, 
        minSeverity == 1 ? "infos" : minSeverity == 2 ? "warns" : minSeverity == 3 ? "errors" : "infos");
    PREFS_STORE_INSTANCE.setValue(ADD_LINTERS, DeepCodeParams.getInstance().useLinter());
    
    // Listener to update DeepCodeParams when changed in GUI
    PREFS_STORE_INSTANCE.addPropertyChangeListener(event -> {
      final String property = event.getProperty();
      final String value = event.getNewValue().toString();
      if (property.equals(BASE_URL)) {
        DeepCodeParams.getInstance().setApiUrl(value);
      } else if (property.equals(TOKEN_ID)) {
        DeepCodeParams.getInstance().setSessionToken(value);
      } else if (property.equals(MIN_SEVERETY_LEVEL)) {
        int severity = value.equals("infos") ? 1 : value.equals("warns") ? 2 : value.equals("errors") ? 3 : 1;
        DeepCodeParams.getInstance().setMinSeverity(severity);
      } else if (property.equals(ADD_LINTERS)) {
        DeepCodeParams.getInstance().setUseLinter((Boolean)event.getNewValue());
      }
    });
  }

}
