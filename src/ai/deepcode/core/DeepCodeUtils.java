package ai.deepcode.core;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import ai.deepcode.javaclient.core.DeepCodeUtilsBase;

public final class DeepCodeUtils extends DeepCodeUtilsBase {

  private static final DeepCodeUtils INSTANCE = new DeepCodeUtils();
  private final DCLogger dcLogger = DCLogger.getInstance();

  private DeepCodeUtils() {
    super(AnalysisData.getInstance(), DeepCodeParams.getInstance(), DeepCodeIgnoreInfoHolder.getInstance(),
        DCLogger.getInstance());
  }

  public static DeepCodeUtilsBase getInstance() {
    return INSTANCE;
  }

  @Override
  protected Collection<Object> allProjectFiles(@NotNull Object project) {
    Collection<Object> filesToProcced = new ArrayList<>();
    try {
      PDU.toProject(project).accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile) {
            filesToProcced.add(resource);
            return false;
          }
          return true;
        }
      });
    } catch (CoreException e) {
      dcLogger.logWarn(e.toString());
    }
    return filesToProcced;
  }

  @Override
  protected long getFileLength(@NotNull Object file) {
    return PDU.getInstance().getFileSize(file);
  }

  @Override
  protected String getFileExtention(@NotNull Object file) {
    return PDU.toIFile(file).getFileExtension();
  }

  @Override
  protected boolean isGitIgnored(@NotNull Object file) {
    return DeepCodeIgnoreInfoHolder.getInstance().isGitIgnoredFile(file);
  }


}
