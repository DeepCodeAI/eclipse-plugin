package ai.deepcode.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

public class AnalysisHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		   IWorkspace workspace = ResourcesPlugin.getWorkspace();
		   for (IProject project : workspace.getRoot().getProjects()) {
			   if (project.isAccessible()) {
				   System.out.println("------ Active Project: " + project);
			   }
		   }
		return null;
	}

}
