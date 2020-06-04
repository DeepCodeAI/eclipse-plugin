package eclipse_plugin;

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
		   for (IProject project : workspace.getRoot().getProjects()) {
			   if (project.isAccessible()) {
				   System.out.println("Projects: " + project);
			   }
		   }
		   IResourceChangeListener listener = new IResourceChangeListener() {
		      public void resourceChanged(IResourceChangeEvent event) {
		         System.out.println("Something changed: " + event);
		      }
		   };
		   workspace.addResourceChangeListener(listener);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
