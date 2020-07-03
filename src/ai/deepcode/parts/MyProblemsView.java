package ai.deepcode.parts;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
//import org.eclipse.ui.internal.views.markers.ProblemsView;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.eclipse.ui.views.markers.internal.MarkerMessages;
import org.eclipse.ui.views.markers.internal.MarkerSupportRegistry;

@SuppressWarnings("restriction")
public class MyProblemsView extends MarkerSupportView {
	public MyProblemsView() {
		super(MarkerSupportRegistry.PROBLEMS_GENERATOR);
	}

	//@Override
//	void updateTitleImage(Integer[] counts) {
//		Image image= WorkbenchPlugin.getDefault().getSharedImages().getImage(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW);
//		if (counts[0].intValue() > 0) {
//			image= WorkbenchPlugin.getDefault().getSharedImages().getImage(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_ERROR);
//		} else if (counts[1].intValue() > 0) {
//			image= WorkbenchPlugin.getDefault().getSharedImages().getImage(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_WARNING);
//		} else if (counts[2].intValue() > 0) {
//			image= WorkbenchPlugin.getDefault().getSharedImages().getImage(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_INFO);
//		}
//		setTitleImage(image);
//	}

	@Override
	protected IUndoContext getUndoContext() {
		return WorkspaceUndoUtil.getProblemsUndoContext();
	}

	@Override
	protected String getDeleteOperationName(IMarker[] markers) {
		Assert.isLegal(markers.length > 0);
		return markers.length == 1 ? MarkerMessages.deleteProblemMarker_operationName : MarkerMessages.deleteProblemMarkers_operationName;
	}

}
