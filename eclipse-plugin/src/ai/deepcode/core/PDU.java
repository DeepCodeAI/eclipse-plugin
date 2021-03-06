package ai.deepcode.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ai.deepcode.javaclient.core.PlatformDependentUtilsBase;
import ai.deepcode.utils.UIUtils;

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
  public Object[] getOpenProjects() {
    final IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    return Arrays.stream(allProjects).filter(IProject::isOpen).toArray();
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
  public int getLineStartOffset(@NotNull Object file, int lineNumber) {
    String fileContent = HashContentUtils.getInstance().getFileContent(file);
    int offset = 0;
    final List<String> lines = Arrays.stream(fileContent.split("\r")).limit(lineNumber).collect(Collectors.toList());
    for (String line : lines) {
      // TODO: for strings with special symbols (2,3, etc bytes per char) will be inaccurate.
      // we might need to read file content in charset eclipse shows files (performance inefficient?)
      offset += line.length() + 1; // add length of `\r` symbol
      // offset = addLineSeparatorOffset(fileContent, offset);
    }
    if (offset < fileContent.length() - 1 && fileContent.charAt(offset) == '\n') {
      offset++; // add length of `\n` symbol
    }
    return offset;
    // final String lineText = fileContent.lines().skip(line).findFirst().orElseThrow();
    // int lineSeparatorLength = fileContent.indexOf("\r\n") == -1 ? 1 : 2;
    // int estimatedOffset = fileContent.lines().limit(line).mapToInt(String::length).sum()
    // // `\n`|`\r`|`\r\n` should be also counted
    // + line * lineSeparatorLength;
    // // could be (?) mixed new line separators `\n`|`\r`|`\r\n`
    // int exactOffset = estimatedOffset;
    // return exactOffset;
  }

  // java.lang.StringUTF16.LinesSpliterator#skipLineSeparator
  private int addLineSeparatorOffset(String value, int start) {
    int fence = value.length() - 1;
    if (start < fence) {
      if (value.charAt(start) == '\r') {
        int next = start + 1;
        if (next < fence && value.charAt(next) == '\n') {
          return next + 1;
        }
      }
      return start + 1;
    }
    return fence;
  }

  @Override
  public void runInBackgroundCancellable(@NotNull Object file, @NotNull String title,
      @NotNull Consumer<Object> progressConsumer) {
    RunUtils.getInstance().runInBackgroundCancellable(file, title, progressConsumer);
  }

  @Override
  public void runInBackground(@NotNull Object project, @NotNull String title,
      @NotNull Consumer<Object> progressConsumer) {
    RunUtils.getInstance().runInBackground(project, title, progressConsumer);
  }

  @Override
  public void cancelRunningIndicators(@NotNull Object project) {
    RunUtils.getInstance().cancelRunningIndicators(project);
  }

  @Override
  public void doFullRescan(@NotNull Object project) {
    RunUtils.getInstance().rescanInBackgroundCancellableDelayed(project, DEFAULT_DELAY_SMALL, false);
  }

  @Override
  public void refreshPanel(@NotNull Object project) {
    UIUtils.updateEWISummary();
  }

  // no direct call possible due to circular dependencies
  @Override
  public boolean isLogged(@Nullable Object project, boolean userActionNeeded) {
    return LoginUtils.getInstance().isLogged(project, userActionNeeded);
  }

  @Override
  public void progressSetText(@Nullable Object progress, String text) {
    if (progress instanceof SubMonitor) {
      SubMonitor subMonitor = (SubMonitor) progress;
      subMonitor.setTaskName(text);
    } else {
      throw new IllegalArgumentException("progress should be SubMonitor instance");
    }
  }

  @Override
  public void progressCheckCanceled(@Nullable Object progress) {
    if (progress instanceof SubMonitor) {
      SubMonitor subMonitor = (SubMonitor) progress;
      subMonitor.checkCanceled();
    } else {
      throw new IllegalArgumentException("progress should be SubMonitor instance");
    }
  }

  private static final Map<SubMonitor, Integer> mapMonitor2TicksUsed = new ConcurrentHashMap<>();

  @Override
  public void progressSetFraction(@Nullable Object progress, double fraction) {
    if (progress instanceof SubMonitor) {
      SubMonitor subMonitor = (SubMonitor) progress;
      // subMonitor.setWorkRemaining(100);
      int ticksUsed = mapMonitor2TicksUsed.getOrDefault(subMonitor, 0);
      int newTicksUsed = (int) (fraction * 100 / 3); // will report 100% (1.00) in 3 phases
      mapMonitor2TicksUsed.put(subMonitor, newTicksUsed);
      if (newTicksUsed - ticksUsed >= 1) {
        subMonitor.worked(1);
      }
    } else {
      throw new IllegalArgumentException("progress should be SubMonitor instance");
    }
  }

  @Override
  public void showInBrowser(@NotNull String url) {
    runInUIThread((shell) -> {
      try {
        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
      } catch (PartInitException | MalformedURLException e) {
        dcLogger.logWarn(e.getMessage());
      }
    });
  }

  private void runInUIThread(Consumer<Shell> consumer) {
    PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
      Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(); // Display.getDefault().getActiveShell()
      consumer.accept(activeShell);
    });
  }

  private static final String title = "Deepcode: ";

  private static boolean isNewLoginRequestShown = false;

  @Override
  public void showLoginLink(@Nullable Object project, String message) {
    if (isNewLoginRequestShown)
      return;
    isNewLoginRequestShown = true;
    runInUIThread((shell) -> {
      if (MessageDialog.openConfirm(shell, title + "Login", message)) {
        RunUtils.getInstance().doBackgroundRun(project, "New Login request", (progress) -> {
          if (LoginUtils.getInstance().checkLogin(project, false)) {
            // in case login was done while background job been waiting (like in tests)
            return;
          }
          LoginUtils.getInstance().requestNewLogin(project, true);
        });
      } ;
      isNewLoginRequestShown = false;
    });
  }

  private static final Set<Object> projectsWithConsentRequestShown = new HashSet<>();

  @Override
  public void showConsentRequest(Object project, boolean userActionNeeded) {
    if (!userActionNeeded && projectsWithConsentRequestShown.contains(project))
      return;
    projectsWithConsentRequestShown.add(project);
    runInUIThread((shell) -> {
      final String message = "Confirm remote analysis of " + PDU.getInstance().getProjectName(project)
          + "\n(see Terms and Conditions at https://www.deepcode.ai/tc)";
      if (MessageDialog.openConfirm(shell, title + "Consent confirmation.", message)) {
        DeepCodeParams.getInstance().setConsentGiven(project);
        projectsWithConsentRequestShown.remove(project);
        RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project);
      } ;
    });
  }

  @Override
  public void showInfo(String message, Object project) {
    runInUIThread((shell) -> {
      MessageDialog.openInformation(shell, title + "Info", message);
    });
  }

  @Override
  public void showWarn(String message, Object project) {
    runInUIThread((shell) -> {
      MessageDialog.openWarning(shell, title + "Warning", message);
    });
  }

  @Override
  public void showError(String message, Object project) {
    runInUIThread((shell) -> {
      MessageDialog.openError(shell, title + "Error", message);
    });
  }

}
