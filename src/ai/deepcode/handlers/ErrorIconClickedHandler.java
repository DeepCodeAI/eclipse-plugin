package ai.deepcode.handlers;

import java.util.Map;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import ai.deepcode.core.DCLogger;
import ai.deepcode.utils.UIUtils;

public class ErrorIconClickedHandler extends AbstractHandler implements IElementUpdater {

  private static final ImageDescriptor icon_error =
      AbstractUIPlugin.imageDescriptorFromPlugin("ai.deepcode", "icons/error.png");

  private static final ImageDescriptor icon_error_gray =
      AbstractUIPlugin.imageDescriptorFromPlugin("ai.deepcode", "icons/error_gray.png");
  
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().showView("ai.deepcode.parts.myProblemsView");
    } catch (PartInitException e) {
      DCLogger.getInstance().logWarn(e.getMessage());
    }
    return null;
  }

  @Override
  public void updateElement(UIElement element, Map parameters) {
    if (UIUtils.getTotalErrors() == 0) {
      element.setIcon(icon_error_gray);
      element.setText("0 ");
    } else {
      element.setIcon(icon_error);
      element.setText(UIUtils.getTotalErrors() + " ");
    }

  }
}
