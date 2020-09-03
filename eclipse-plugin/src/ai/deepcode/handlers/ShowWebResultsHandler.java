package ai.deepcode.handlers;

import java.util.Arrays;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import com.google.gson.internal.PreJava9DateFormatProvider;
import ai.deepcode.AnalysisJob;
import ai.deepcode.core.AnalysisData;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.PDU;

public class ShowWebResultsHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    String projectName = event.getParameter("ai.deepcode.params.project");
    if (projectName == null)
      DCLogger.getInstance().logWarn("Empty Project param at: " + event);

    final IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    IProject project = Arrays.stream(allProjects).filter(IProject::isOpen)
        .filter(prj -> prj.getName().equals(projectName)).findFirst().get();

    final String analysisUrl = AnalysisData.getInstance().getAnalysisUrl(project);
    if (!analysisUrl.isEmpty())
      PDU.getInstance().showInBrowser(analysisUrl);

    return null;
  }
}
