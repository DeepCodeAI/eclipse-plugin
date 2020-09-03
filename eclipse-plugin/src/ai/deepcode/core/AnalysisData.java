package ai.deepcode.core;

import java.util.Collection;
import org.eclipse.core.resources.IFile;
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
    super(PDU.getInstance(), HashContentUtils.getInstance(), DeepCodeParams.getInstance(), DCLogger.getInstance());
  }

  // TODO fix markers not deleted if during one update call new update
  @Override
  protected void updateUIonFilesRemovalFromCache(@NotNull Collection<Object> files) {
    for (Object file : files) {
      IFile iFile = PDU.toIFile(file);
      try {
        iFile.deleteMarkers("ai.deepcode.deepcodemarker", true, IResource.DEPTH_INFINITE);
      } catch (CoreException e1) {
        if (iFile.isAccessible()) {
          DCLogger.getInstance().logWarn(e1.getMessage());;
        }
      }
    }
  }

}
