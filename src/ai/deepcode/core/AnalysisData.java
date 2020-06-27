package ai.deepcode.core;

import java.util.Collection;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import ai.deepcode.javaclient.core.*;

public final class AnalysisData extends AnalysisDataBase {

  private static final AnalysisData INSTANCE = new AnalysisData();

  public static AnalysisData getInstance() {
    return INSTANCE;
  }

  private AnalysisData() {
    super(
        PDU.getInstance(),
        HashContentUtils.getInstance(),
        DeepCodeParams.getInstance(),
        DCLogger.getInstance());
  }

  @Override
  protected void updateUIonFilesRemovalFromCache(@NotNull Collection<Object> files) {
    for (Object file : files) {
      try {
        PDU.toIFile(file).deleteMarkers("ai.deepcode.deepcodemarker", true, IResource.DEPTH_INFINITE);
      } catch (CoreException e1) {
        DCLogger.getInstance().logWarn(e1.getMessage());;
      }
    }
  }

}
