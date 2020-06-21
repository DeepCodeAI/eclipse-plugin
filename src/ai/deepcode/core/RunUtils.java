package ai.deepcode.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jetbrains.annotations.NotNull;

// TODO generalize RunUtils
public final class RunUtils {

  private static final DCLogger dcLogger = DCLogger.getInstance();

  private static final Map<IProject, Set<IProgressMonitor>> mapProject2Monitors = new ConcurrentHashMap<>();

  private static synchronized Set<IProgressMonitor> getRunningIndicators(@NotNull IProject project) {
    return mapProject2Monitors.computeIfAbsent(project, p -> new HashSet<>());
  }

  private static class MyJob extends Job {

    private Consumer<Object> progressConsumer;
    private IProject project;

    public MyJob(String name, @NotNull IProject project, @NotNull Consumer<Object> progressConsumer) {
      super(name);
      this.project = project;
      this.progressConsumer = progressConsumer;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      dcLogger.logInfo("New Process started at " + project);
      getRunningIndicators(project).add(monitor);
      progressConsumer.accept(monitor);
      dcLogger.logInfo("Process ending at " + project);
      getRunningIndicators(project).remove(monitor);
      return null;
    }

  }

  public static void runInBackground(@NotNull IProject project, @NotNull Consumer<Object> progressConsumer) {
    dcLogger.logInfo("runInBackground requested");
    new MyJob("DeepCode runInBackground running...", project, progressConsumer).schedule();
  }

  public static void runInBackgroundCancellable(@NotNull IFile file, @NotNull Consumer<Object> progressConsumer) {
    dcLogger.logInfo("runInBackgroundCancellable requested");
    // TODO make it cancellable
    new MyJob("DeepCode runInBackgroundCancellable running...", file.getProject(), progressConsumer).schedule();
  }

  public static void cancelRunningIndicators(@NotNull IProject project) {
    String indicatorsList =
        getRunningIndicators(project).stream().map(IProgressMonitor::toString).collect(Collectors.joining("\n"));
    dcLogger.logInfo("Canceling ProgressIndicators:\n" + indicatorsList);
    // in case any indicator holds Bulk mode process
    getRunningIndicators(project).forEach(IProgressMonitor::done);
    getRunningIndicators(project).clear();
    // projectsWithFullRescanRequested.remove(project);
  }

  public static void rescanInBackgroundCancellableDelayed(@NotNull IProject project, int delayMilliseconds) {
    dcLogger.logInfo("rescanInBackgroundCancellableDelayed requested for: " + project.getName());
    new MyJob("DeepCode Rescan running...", project, (progress) -> {
      AnalysisData.getInstance().removeProjectFromCaches(project);
      AnalysisData.getInstance().updateCachedResultsForFiles(project,
          DeepCodeUtils.getInstance().getAllSupportedFilesInProject(project), Collections.emptyList(), progress);
    }).schedule();
  }

}
