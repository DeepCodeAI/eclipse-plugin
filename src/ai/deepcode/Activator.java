package ai.deepcode;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.eclipse.core.resources.*;

public class Activator implements BundleActivator {

  private static BundleContext context;

  static BundleContext getContext() {
    return context;
  }

  public void start(BundleContext bundleContext) throws Exception {
    Activator.context = bundleContext;

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IResourceChangeListener listener = new DeepCodeResourceChangeListener();
    
    workspace.addResourceChangeListener(listener);
  }

  public void stop(BundleContext bundleContext) throws Exception {
    Activator.context = null;
  }

}
