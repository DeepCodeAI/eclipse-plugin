package ai.deepcode.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ai.deepcode.javaclient.core.PlatformDependentUtilsBase;

public class PDU extends PlatformDependentUtilsBase {
  
  private PDU() {};

  private static final PDU INSTANCE = new PDU();

  public static PDU getInstance() {
    return INSTANCE;
  }
  
  private static final DCLogger dcLogger = DCLogger.getInstance();

  @NotNull
  public static IFile toIFile(@NotNull Object file) {
    if (!(file instanceof IFile))
      throw new IllegalArgumentException("file should be IFile instance");
    return (IFile) file;
  }

  @NotNull
  public static Collection<IFile> toIFiles(@NotNull Collection<Object> files) {
    return files.stream().map(PDU::toIFile).collect(Collectors.toSet());
  }

  @NotNull
  public static Collection<Object> toObjects(@NotNull Collection<IFile> files) {
    return new HashSet<>(files);
  }

  @NotNull
  public static IProject toProject(@NotNull Object project) {
    if (!(project instanceof IProject))
      throw new IllegalArgumentException("project should be IProject instance");
    return (IProject) project;
  }
 
  
  @Override
  public @NotNull Object getProject(@NotNull Object file) {
     return toIFile(file).getProject();
  }

  @Override
  public @NotNull String getProjectName(@NotNull Object project) {
    return toProject(project).getName();
  }

  @Override
  public @NotNull String getFileName(@NotNull Object file) {
    return toIFile(file).getName();
  }

  @Override
  protected @NotNull String getProjectBasedFilePath(@NotNull Object file) {
    return toIFile(file).getProjectRelativePath().toString();
  }

  @Override
  public long getFileSize(@NotNull Object file) {
    try {
      return EFS.getStore(toIFile(file).getLocationURI()).fetchInfo().getLength();
    } catch (CoreException e) {
      dcLogger.logWarn(e.getMessage());
      return Integer.MAX_VALUE;
    }
  }

  @Override
  public int getLineStartOffset(@NotNull Object file, int line) {
    int prevLineNum = line - 1;
    // TODO use cached content
    String fileContent =HashContentUtils.getInstance().doGetFileContent(file);
    // TODO optimize
    int lineSeparatorLength = fileContent.indexOf("\r\n") == -1 ? 1 : 2;
    // `\n`|`\r`|`\r\n` should be also counted
    return fileContent.lines().limit(prevLineNum).mapToInt(String::length).sum() + prevLineNum * lineSeparatorLength;
  }

  @Override
  public void runInBackgroundCancellable(@NotNull Object file, @NotNull Runnable runnable) {
    RunUtils.runInBackgroundCancellable(toIFile(file), runnable);
  }
  
  @Override
  public void runInBackground(@NotNull Object project, @NotNull Runnable runnable) {
    RunUtils.runInBackground(toProject(project), runnable);
  }

  @Override
  public void cancelRunningIndicators(@NotNull Object project) {
    RunUtils.cancelRunningIndicators(toProject(project));
  }

  @Override
  public void doFullRescan(@NotNull Object project) {
    RunUtils.rescanInBackgroundCancellableDelayed(toProject(project), DEFAULT_DELAY_SMALL);
  }

  @Override
  public void refreshPanel(@NotNull Object project) {
    // TODO Auto-generated method stub
  }

  @Override
  public boolean isLogged(@Nullable Object project, boolean userActionNeeded) {
    return LoginUtils.getInstance().isLogged(project == null ? null : toProject(project), userActionNeeded);
  }

  @Override
  public void progressSetText(String text) {
    // TODO Auto-generated method stub

  }

  @Override
  public void progressCheckCanceled() {
    // TODO Auto-generated method stub

  }

  @Override
  public void progressSetFraction(double fraction) {
    // TODO Auto-generated method stub

  }

  @Override
  public void showInBrowser(@NotNull String url) {
    // TODO Auto-generated method stub

  }

  @Override
  public void showLoginLink(Object project, String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void showConsentRequest(Object project, boolean userActionNeeded) {
    // TODO Auto-generated method stub

  }

  @Override
  public void showInfo(String message, Object project) {
    // TODO Auto-generated method stub

  }

  @Override
  public void showWarn(String message, Object project) {
    // TODO Auto-generated method stub

  }

  @Override
  public void showError(String message, Object project) {
    // TODO Auto-generated method stub

  }

}
