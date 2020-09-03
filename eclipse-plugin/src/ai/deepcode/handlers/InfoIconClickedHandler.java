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

public class InfoIconClickedHandler extends AbstractHandler implements IElementUpdater {

  private static final ImageDescriptor icon_info =
      AbstractUIPlugin.imageDescriptorFromPlugin("ai.deepcode", "icons/info.png");

  private static final ImageDescriptor icon_info_gray =
      AbstractUIPlugin.imageDescriptorFromPlugin("ai.deepcode", "icons/info_gray.png");
  
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
    if (UIUtils.getTotalInfos() == 0) {
      element.setIcon(icon_info_gray);
      element.setText("0 ");
      element.setTooltip("DeepCode: No Informational suggestions");
    } else {
      element.setIcon(icon_info);
      element.setText(UIUtils.getTotalInfos() + " ");
      element.setTooltip("DeepCode: " + UIUtils.getTotalErrors() + " Informational suggestions");
    }

  }
}
