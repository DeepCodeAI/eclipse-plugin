package ai.deepcode.utils;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import ai.deepcode.core.AnalysisData;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.DeepCodeUtils;

public class UIUtils {
  
  private UIUtils() {}

  private static int totalErrors = 0;
  private static int totalWarns = 0;
  private static int totalInfos = 0;
  
  public static void updateEWISummary() {
    int errors = 0;
    int warnings = 0;
    int infos = 0;
    for (Object project : AnalysisData.getInstance().getAllCachedProject()) {
      final DeepCodeUtils.ErrorsWarningsInfos ewi =
          DeepCodeUtils.getInstance().getEWI(AnalysisData.getInstance().getAllFilesWithSuggestions(project));
      errors += ewi.getErrors();
      warnings += ewi.getWarnings();
      infos += ewi.getInfos();
    }
    totalErrors = errors;
    totalWarns = warnings;
    totalInfos = infos;
    DCLogger.getInstance().logInfo("error=" + errors + " warning=" + warnings + " info=" + infos);
    
    // update labels and icons
    ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
    cs.refreshElements("ai.deepcode.errorIconCommand", null);
    cs.refreshElements("ai.deepcode.warnIconCommand", null);
    cs.refreshElements("ai.deepcode.infoIconCommand", null);
  }

  public static int getTotalErrors() {
    return totalErrors;
  }
  
  public static int getTotalWarnings() {
    return totalWarns;
  }

  public static int getTotalInfos() {
    return totalInfos;
  }
  
}
