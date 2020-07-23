package ai.deepcode;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import ai.deepcode.core.AnalysisData;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.LoginUtils;
import ai.deepcode.core.RunUtils;

public class AnalysisJob extends Job {

  public AnalysisJob(String name) {
    super(name);
  }

  private static final DCLogger dcLogger = DCLogger.getInstance();

  // private static boolean isNewLoginRequstShown = false;

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    for (IProject project : workspace.getRoot().getProjects()) {
      if (!project.isAccessible())
        continue;
      dcLogger.logInfo("Re-Analyse Project requested for: " + project);
      AnalysisData.getInstance().resetCachesAndTasks(project);
      if (!LoginUtils.getInstance().checkLogin(project, true)) {
        break; // login request should be shown, see checkLogin(); no needs to traverse further
      }
      if (LoginUtils.getInstance().checkConsent(project, true)) {
        RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project);
      }
    }
    return Status.OK_STATUS;
  }

}
