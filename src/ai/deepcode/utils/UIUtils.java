package ai.deepcode.utils;

import ai.deepcode.core.AnalysisData;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.DeepCodeUtils;

public class UIUtils {
  
  private UIUtils() {}

  private static int totalErrors;
  
  public static int getTotalErrors() {
    return totalErrors;
  }
  
  public static void updateSummaryIcons() {
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
    DCLogger.getInstance().logInfo("error=" + errors + " warning=" + warnings + " info=" + infos);
    
  }

}
