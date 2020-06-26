package eclipse_plugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import ai.deepcode.core.AnalysisData;
import ai.deepcode.core.LoginUtils;
import ai.deepcode.core.PDU;
import ai.deepcode.core.RunUtils;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.Nullable;

public class Activator implements BundleActivator {

  private static BundleContext context;

  static BundleContext getContext() {
    return context;
  }

  public void start(BundleContext bundleContext) throws Exception {
    Activator.context = bundleContext;

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IResourceChangeListener listener = new IResourceChangeListener() {
      public void resourceChanged(IResourceChangeEvent event) {
        System.out.println("Something changed: " + event);

        // on Project opened
        if (event == null || event.getDelta() == null) {
          return;
        }
        try {
          event.getDelta().accept(new IResourceDeltaVisitor() {
            public boolean visit(final IResourceDelta delta) throws CoreException {
              IResource resource = delta.getResource();
              if (((resource.getType() & IResource.PROJECT) != 0) && resource.getProject().isOpen()
                  && delta.getKind() == IResourceDelta.CHANGED && ((delta.getFlags() & IResourceDelta.OPEN) != 0)) {

                IProject project = (IProject) resource;
                projectOpened(project);
              }
              return true;
            }

            private void projectOpened(@Nullable IProject project) {
              AnalysisData.getInstance().resetCachesAndTasks(project);
              // Initial logging if needed.
              if (LoginUtils.getInstance().isLogged(project, true)) {
                RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project);
              }
            }            
          });          
        } catch (CoreException e) {
          e.printStackTrace();
        }
      }
    };
    workspace.addResourceChangeListener(listener);
    
    //RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(null);
  }

  public void stop(BundleContext bundleContext) throws Exception {
    Activator.context = null;
  }

}
