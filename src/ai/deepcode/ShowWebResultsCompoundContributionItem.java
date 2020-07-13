package ai.deepcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

public class ShowWebResultsCompoundContributionItem extends CompoundContributionItem implements IWorkbenchContribution {

  private IServiceLocator myServiceLocator;

  @Override
  protected IContributionItem[] getContributionItems() {
    List<IContributionItem> result = new ArrayList<>();
    final IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (IProject project : allProjects) {
      if (!project.isOpen())
        continue;
      Map<String, String> params = new HashMap<>();
      params.put("project", project.getName());
      CommandContributionItemParameter commandContributionItemParameter =
          new CommandContributionItemParameter(myServiceLocator, "ai.deepcode.toolbar.showWebId",
              "ai.deepcode.showWebCommandId", CommandContributionItem.STYLE_PUSH);
//      commandContributionItemParameter.parameters = params;
      commandContributionItemParameter.label = project.getName();

      result.add(new CommandContributionItem(commandContributionItemParameter));
    }
    return result.toArray(new IContributionItem[0]);
  }

  @Override
  public void initialize(IServiceLocator serviceLocator) {
    myServiceLocator = serviceLocator;
  }

}
