package ai.deepcode.parts;

import org.eclipse.ui.IStartup;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.LoginUtils;
import ai.deepcode.core.RunUtils;

public class StartupClass implements IStartup {

  @Override
  public void earlyStartup() {
    DCLogger.getInstance().logInfo("Startup initial scan started...");
    // Initial logging check before analysis.
    if (LoginUtils.getInstance().isLogged(null, true)) {
      RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(null);
    }
  }

}
