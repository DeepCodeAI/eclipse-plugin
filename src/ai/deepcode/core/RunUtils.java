package ai.deepcode.core;

import java.util.Collection;
import java.util.function.Consumer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.jetbrains.annotations.NotNull;
import ai.deepcode.javaclient.core.MyTextRange;
import ai.deepcode.javaclient.core.RunUtilsBase;
import ai.deepcode.javaclient.core.SuggestionForFile;

public final class RunUtils extends RunUtilsBase {

  private static final RunUtils INSTANCE = new RunUtils();

  public static RunUtils getInstance() {
    return INSTANCE;
  }

  private RunUtils() {
    super(
        PDU.getInstance(),
        HashContentUtils.getInstance(),
        AnalysisData.getInstance(),
        DeepCodeUtils.getInstance(),
        DCLogger.getInstance());
  }

  private class MyJob extends Job {

    private Consumer<Object> progressConsumer;
    private IProject project;

    public MyJob(@NotNull IProject project, @NotNull String title, @NotNull Consumer<Object> progressConsumer) {
      super(title);
      this.project = project;
      this.progressConsumer = progressConsumer;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      progressConsumer.accept(SubMonitor.convert(monitor, 100));
      return Status.OK_STATUS;
    }

  }

  @Override
  protected boolean reuseCurrentProgress(@NotNull Object project, @NotNull String title,
      @NotNull Consumer<Object> progressConsumer) {
    // TODO Auto-generated method stub    
   return false;
  }

  @Override
  protected void doBackgroundRun(@NotNull Object project, @NotNull String title,
      @NotNull Consumer<Object> progressConsumer) {
    new MyJob(PDU.toProject(project), title, progressConsumer).schedule();
  }

  @NotNull
  private static SubMonitor toProgress(@NotNull Object progress) {
    if (!(progress instanceof SubMonitor))
      throw new IllegalArgumentException("progress should be SubMonitor instance");
    return (SubMonitor) progress;
  }

  @Override
  protected void cancelProgress(@NotNull Object progress) {
    toProgress(progress).done();
  }

  @Override
  protected void bulkModeForceUnset(@NotNull Object project) {
    // TODO Auto-generated method stub    
  }

  @Override
  protected void bulkModeUnset(@NotNull Object project) {
    // TODO Auto-generated method stub    
  }

  @Override
  protected void updateAnalysisResultsUIPresentation(@NotNull Collection<Object> files) {
    for (Object file : files) {           
      for (SuggestionForFile suggestion : AnalysisData.getInstance().getAnalysis(file)) {
        for (MyTextRange range : suggestion.getRanges()) {
          try {
            IMarker m = PDU.toIFile(file).createMarker("ai.deepcode.deepcodemarker");
            
            m.setAttribute(IMarker.LINE_NUMBER, range.getStartRow());
            m.setAttribute(IMarker.CHAR_START, range.getStart());
            m.setAttribute(IMarker.CHAR_END, range.getEnd());
            String prefix = "(" + range.getStartRow() + ":" + (range.getStartCol() + 1) + ")" + " DeepCode: ";
            m.setAttribute(IMarker.MESSAGE, prefix + suggestion.getMessage());
            m.setAttribute(IMarker.SEVERITY, suggestion.getSeverity() - 1);
          } catch (CoreException e) {
            dcLogger.logWarn(e.getMessage());;
          }
        }
      }
    }
  }

}
