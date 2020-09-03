package ai.deepcode.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import ai.deepcode.AnalysisJob;

public class ShowSettingsHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell activeShell = HandlerUtil.getActiveShell(event);
    PreferenceDialog pref =
        PreferencesUtil.createPreferenceDialogOn(activeShell, "ai.deepcode.preferences", null, null);
    if (pref != null)
      pref.open();
    return null;
  }
}
