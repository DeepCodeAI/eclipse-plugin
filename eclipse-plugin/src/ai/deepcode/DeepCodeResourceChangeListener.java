package ai.deepcode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.Nullable;
import ai.deepcode.core.AnalysisData;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.DeepCodeIgnoreInfoHolder;
import ai.deepcode.core.DeepCodeUtils;
import ai.deepcode.core.LoginUtils;
import ai.deepcode.core.PDU;
import ai.deepcode.core.RunUtils;

final class DeepCodeResourceChangeListener implements IResourceChangeListener {

  public void resourceChanged(IResourceChangeEvent event) {
    if (event == null) {
      return;
    }

    // TODO delete file event
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {

      if (event.getDelta() == null) {
        return;
      }
      IResourceDelta rootDelta = event.getDelta();
      final Set<IResource> filesChanged = new HashSet<>();
      final Set<IResource> ignoreFilesChanged = new HashSet<>();

      try {
        rootDelta.accept(new FileChangedOrCreatedVisitor(filesChanged, ignoreFilesChanged));
        rootDelta.accept(new ProjectOpenedOrCreatedVisitor());

      } catch (CoreException e) {
        DCLogger.getInstance().logWarn(e.getMessage());
      }

      // TODO optimize
      // Reanalyze files affected
      if (filesChanged.size() > 1) {
        // Easier to do full rescan
        filesChanged.stream().map(f -> PDU.getInstance().getProject(f)).distinct()
            .forEach(project -> RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project));
      } else
        for (IResource file : filesChanged) {
          RunUtils.getInstance().runInBackgroundCancellable(file,
              "Analyzing file changed: " + file.getProjectRelativePath(), (progress) -> {
                if (AnalysisData.getInstance().isFileInCache(file)) {
                  AnalysisData.getInstance().removeFilesFromCache(Collections.singleton(file));
                }
                Object project = PDU.getInstance().getProject(file);
                RunUtils.getInstance().updateCachedAnalysisResults(project, Collections.singleton(file), progress);
              });
        }

      // Rescan .dcignore and .gitignore files changed
      for (IResource ignoreFile : ignoreFilesChanged) {
        RunUtils.getInstance().runInBackgroundCancellable(ignoreFile, "Updating ignored files list...", (progress) -> {
          final DeepCodeIgnoreInfoHolder ignoreHolder = DeepCodeIgnoreInfoHolder.getInstance();
          if (ignoreHolder.is_dcignoreFile(ignoreFile))
            ignoreHolder.update_dcignoreFileContent(ignoreFile);
          else if (ignoreHolder.is_gitignoreFile(ignoreFile))
            ignoreHolder.update_gitignoreFileContent(ignoreFile);
          else
            DCLogger.getInstance()
                .logWarn("ignore file should be either .gitignore or .dcignore: " + ignoreFile.getName());
        });
      }
      // rescan each project affected by .dcignore changes
      ignoreFilesChanged.stream().map(PDU.getInstance()::getProject).distinct().forEach(project -> RunUtils
          .getInstance().rescanInBackgroundCancellableDelayed(project, PDU.DEFAULT_DELAY_SMALL, true));

      // Project closing event
    } else if (event.getType() == IResourceChangeEvent.PRE_CLOSE
        || event.getType() == IResourceChangeEvent.PRE_DELETE) {
      IResource rsrc = event.getResource();
      if (rsrc instanceof IProject) {
        RunUtils.getInstance().runInBackground(rsrc,
            "Removing Project [" + PDU.getInstance().getProjectName(rsrc) + "] from cache...",
            (progress) -> AnalysisData.getInstance().resetCachesAndTasks(rsrc));
      }
    }

  }

  private final class FileChangedOrCreatedVisitor implements IResourceDeltaVisitor {
    private final Set<IResource> filesChanged;
    private final Set<IResource> ignoreFilesChanged;

    private FileChangedOrCreatedVisitor(Set<IResource> filesChanged, Set<IResource> ignoreFilesChanged) {
      this.filesChanged = filesChanged;
      this.ignoreFilesChanged = ignoreFilesChanged;
    }

    public boolean visit(IResourceDelta delta) {
      final boolean isResorceContentChanged = 
          delta.getKind() == IResourceDelta.CHANGED // changed resource
          && ((delta.getFlags() & IResourceDelta.CONTENT) != 0); // content changes
      final boolean isResourceAdded = delta.getKind() == IResourceDelta.ADDED;

      if (!(isResorceContentChanged || isResourceAdded))
        return true;

      IResource resource = delta.getResource();
      // only interested in files, valid(accessible) files.
      if (resource.getType() != IResource.FILE && resource.isAccessible())
        return true;

      // Proceed supported files
      if (DeepCodeUtils.getInstance().isSupportedFileFormat((IFile) resource)) {
        filesChanged.add(resource);
      }

      // Proceed .dcignore and .gitignore files
      if (DeepCodeIgnoreInfoHolder.getInstance().is_ignoreFile((IFile) resource)) {
        ignoreFilesChanged.add(resource);
      }

      return true;
    }
  }

  private final class ProjectOpenedOrCreatedVisitor implements IResourceDeltaVisitor {
    public boolean visit(final IResourceDelta delta) throws CoreException {
      IResource resource = delta.getResource();
      final boolean isProjectDelta = ((resource.getType() & IResource.PROJECT) != 0) && resource.getProject().isOpen();
      final boolean isProjectOpenedEvent =
          delta.getKind() == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.OPEN) != 0;
      final boolean isProjectCreatedEvent = delta.getKind() == IResourceDelta.ADDED;
      
      if (isProjectDelta && (isProjectOpenedEvent || isProjectCreatedEvent)) {
        IProject project = (IProject) resource;
        analyseProject(project);
        return false;
      }
      return true;
    }

    private void analyseProject(@Nullable IProject project) {
      AnalysisData.getInstance().resetCachesAndTasks(project);
      // Initial (silent needed???) logging check before analysis.
      if (LoginUtils.getInstance().isLogged(project, true)) {
        RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project);
      }
    }
  }

}
