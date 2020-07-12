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

public class WarnIconClickedHandler extends AbstractHandler implements IElementUpdater {

  private static final ImageDescriptor icon_warn =
      AbstractUIPlugin.imageDescriptorFromPlugin("ai.deepcode", "icons/warn.png");

  private static final ImageDescriptor icon_warn_gray =
      AbstractUIPlugin.imageDescriptorFromPlugin("ai.deepcode", "icons/warn_gray.png");
  
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
    if (UIUtils.getTotalWarnings() == 0) {
      element.setIcon(icon_warn_gray);
      element.setText("0 ");
    } else {
      element.setIcon(icon_warn);
      element.setText(UIUtils.getTotalWarnings() + " ");
    }

  }
}
