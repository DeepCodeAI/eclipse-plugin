package ai.deepcode.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ai.deepcode.javaclient.core.PlatformDependentUtilsBase;
import jdk.nashorn.tools.Shell;

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
    String fileContent = HashContentUtils.getInstance().doGetFileContent(file);
    // TODO optimize
    int lineSeparatorLength = fileContent.indexOf("\r\n") == -1 ? 1 : 2;
    // `\n`|`\r`|`\r\n` should be also counted
    return fileContent.lines().limit(prevLineNum).mapToInt(String::length).sum() + prevLineNum * lineSeparatorLength;
  }

  @Override
  public void runInBackgroundCancellable(@NotNull Object file, @NotNull String title,
      @NotNull Consumer<Object> progressConsumer) {
    RunUtils.runInBackgroundCancellable(toIFile(file), progressConsumer);
  }

  @Override
  public void runInBackground(@NotNull Object project, @NotNull String title,
      @NotNull Consumer<Object> progressConsumer) {
    RunUtils.runInBackground(toProject(project), progressConsumer);
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
  public void progressSetText(@Nullable Object progress, String text) {
    if (progress instanceof IProgressMonitor) {
      SubMonitor subMonitor = SubMonitor.convert((IProgressMonitor) progress);
      subMonitor.setTaskName(text);
    }
  }

  @Override
  public void progressCheckCanceled(@Nullable Object progress) {
    if (progress instanceof IProgressMonitor) {
      SubMonitor subMonitor = SubMonitor.convert((IProgressMonitor) progress);
      subMonitor.checkCanceled();
    }
  }

  @Override
  public void progressSetFraction(@Nullable Object progress, double fraction) {
    if (progress instanceof IProgressMonitor) {
      SubMonitor subMonitor = SubMonitor.convert((IProgressMonitor) progress);
      subMonitor.worked((int) (fraction * 100));;
    }
  }

  @Override
  public void showInBrowser(@NotNull String url) {
    try {
      PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
    } catch (PartInitException | MalformedURLException e) {
      dcLogger.logWarn(e.getMessage());
    }
  }

  @Override
  public void showLoginLink(Object project, String message) {
    if (MessageDialog.openConfirm(null, "Login", message)) {
      LoginUtils.getInstance().requestNewLogin(project, true);
    };
  }

  @Override
  public void showConsentRequest(Object project, boolean userActionNeeded) {
    if (MessageDialog.openConfirm(null, "Confirm", "Consent request")) {
      DeepCodeParams.getInstance().setConsentGiven(project);
    };
  }

  @Override
  public void showInfo(String message, Object project) {
    MessageDialog.openInformation(null, "Info", message);
//    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
//      public void run() {
//          Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//      }
//    });    
  }

  @Override
  public void showWarn(String message, Object project) {
    MessageDialog.openWarning(null, "Warning", message);  }

  @Override
  public void showError(String message, Object project) {
    MessageDialog.openError(null, "Error", message);  }

}
