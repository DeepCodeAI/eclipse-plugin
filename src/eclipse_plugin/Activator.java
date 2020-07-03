package eclipse_plugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import ai.deepcode.core.AnalysisData;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.DeepCodeUtils;
import ai.deepcode.core.LoginUtils;
import ai.deepcode.core.PDU;
import ai.deepcode.core.RunUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
        if (event == null || event.getDelta() == null) {
          return;
        }

        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
 
          IResourceDelta rootDelta = event.getDelta();
          final Set<IResource> filesChanged = new HashSet<>();
          IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
            public boolean visit(IResourceDelta delta) {
              // only interested in changed resources (not added or removed)
              if (delta.getKind() != IResourceDelta.CHANGED)
                return true;
              // only interested in content changes
              if ((delta.getFlags() & IResourceDelta.CONTENT) == 0)
                return true;
              IResource resource = delta.getResource();
              // only interested in supported files
              if (resource.getType() == IResource.FILE
                  && DeepCodeUtils.getInstance().isSupportedFileFormat((IFile) resource)) {
                filesChanged.add(resource);
              }
              return true;
            }
          };
          try {
            rootDelta.accept(visitor);
          } catch (CoreException e) {
            DCLogger.getInstance().logWarn(e.getMessage());
          }
          if (filesChanged.size() >= 10) {
            // Easier to do full rescan
            filesChanged.stream().map(f -> PDU.getInstance().getProject(f)).distinct()
                .forEach(project -> RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project));
          } else
            for (IResource file : filesChanged) {
              RunUtils.getInstance().runInBackgroundCancellable(file, "Analyzing files changed...", (progress) -> {
                if (AnalysisData.getInstance().isFileInCache(file)) {
                  AnalysisData.getInstance().removeFilesFromCache(Collections.singleton(file));
                }
                Object project = PDU.getInstance().getProject(file);
                RunUtils.getInstance().updateCachedAnalysisResults(project, Collections.singleton(file), progress);
              });
            }
        }

        // on Project opened
        try {
          event.getDelta().accept(new IResourceDeltaVisitor() {
            public boolean visit(final IResourceDelta delta) throws CoreException {
              IResource resource = delta.getResource();
              if (((resource.getType() & IResource.PROJECT) != 0) && resource.getProject().isOpen()
                  && delta.getKind() == IResourceDelta.CHANGED && ((delta.getFlags() & IResourceDelta.OPEN) != 0)) {

                IProject project = (IProject) resource;
                projectOpened(project);
                return false;
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
          DCLogger.getInstance().logWarn(e.getMessage());
        }
      }
    };
    workspace.addResourceChangeListener(listener);

    // RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(null);
  }

  public void stop(BundleContext bundleContext) throws Exception {
    Activator.context = null;
  }

}
