package ai.deepcode.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class AnalysisJob extends Job {

  public AnalysisJob(String name) {
    super(name);
  }

  private static final DCLogger dcLogger = DCLogger.getInstance();

  // private static boolean isNewLoginRequstShown = false;

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    boolean isNewLoginRequstShown = false;
    for (IProject project : workspace.getRoot().getProjects()) {
      if (!project.isAccessible())
        continue;
      dcLogger.logInfo("Re-Analyse Project requested for: " + project);
      AnalysisData.getInstance().resetCachesAndTasks(project);
      if (!isNewLoginRequstShown) {
        if (LoginUtils.getInstance().isLogged(project, true)) {
          RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project);
        } else {
          // login request should be already shown, see isLogged()
          isNewLoginRequstShown = true;
        }
      }
    }
    return Status.OK_STATUS;
  }

}
