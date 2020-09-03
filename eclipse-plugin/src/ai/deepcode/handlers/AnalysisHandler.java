package ai.deepcode.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import ai.deepcode.AnalysisJob;

public class AnalysisHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		new AnalysisJob("DeepCode analysis running...").schedule();
		return null;
	}
}
