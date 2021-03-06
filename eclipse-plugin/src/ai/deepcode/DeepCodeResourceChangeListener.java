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

    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {

      if (event.getDelta() == null) {
        return;
      }
      IResourceDelta rootDelta = event.getDelta();
      final Set<IResource> filesChanged = new HashSet<>();
      final Set<IFile> filesDeleted = new HashSet<>();
      final Set<IResource> ignoreFilesChanged = new HashSet<>();

      try {
        rootDelta.accept(new FileChangedCreatedDeletedVisitor(filesChanged, filesDeleted, ignoreFilesChanged));
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

      filesDeleted.stream().map(PDU.getInstance()::getProject).distinct().forEach(project -> {
        DCLogger.getInstance().logInfo("Found " + filesDeleted.size() + " files to remove: " + filesDeleted);
        // if too many files removed then it's easier to do full rescan
        if (filesDeleted.size() > 10) {
          // small delay to prevent multiple rescan Background tasks
          RunUtils.getInstance().rescanInBackgroundCancellableDelayed(project, PDU.DEFAULT_DELAY_SMALL, true);
        } else if (!RunUtils.getInstance().isFullRescanRequested(project)) {
          RunUtils.getInstance().runInBackground(project,
              "Removing " + filesDeleted.size() + " locally deleted files on server...", (progress) -> {
                AnalysisData.getInstance().removeFilesFromCache(PDU.toObjects(filesDeleted));
                RunUtils.getInstance().updateCachedAnalysisResults(project, Collections.emptyList(),
                    PDU.toObjects(filesDeleted), progress);
              });
        }
      });

      // rescan each project affected by .dcignore changes
      ignoreFilesChanged.stream().map(PDU.getInstance()::getProject).distinct().forEach(project -> {
        // Rescan .dcignore and .gitignore files changed / deleted
        RunUtils.getInstance().runInBackground(project, "Updating ignored files list...", (progress) -> {
          for (IResource ignoreFile : ignoreFilesChanged) {
            final DeepCodeIgnoreInfoHolder ignoreHolder = DeepCodeIgnoreInfoHolder.getInstance();
            if (ignoreHolder.is_dcignoreFile(ignoreFile))
              ignoreHolder.update_dcignoreFileContent(ignoreFile);
            else if (ignoreHolder.is_gitignoreFile(ignoreFile))
              ignoreHolder.update_gitignoreFileContent(ignoreFile);
            else
              DCLogger.getInstance()
                  .logWarn("ignore file should be either .gitignore or .dcignore: " + ignoreFile.getName());
          }
          RunUtils.getInstance().rescanInBackgroundCancellableDelayed(project, PDU.DEFAULT_DELAY_SMALL, true);
        });
      });

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

  private final class FileChangedCreatedDeletedVisitor implements IResourceDeltaVisitor {
    private final Set<IResource> filesChanged;
    private final Set<IFile> filesDeleted;
    private final Set<IResource> ignoreFilesChanged;

    private FileChangedCreatedDeletedVisitor(Set<IResource> filesChanged, Set<IFile> filesDeleted,
        Set<IResource> ignoreFilesChanged) {
      this.filesChanged = filesChanged;
      this.filesDeleted = filesDeleted;
      this.ignoreFilesChanged = ignoreFilesChanged;
    }

    public boolean visit(IResourceDelta delta) {
      IResource resource = delta.getResource();
      // only interested in files.
      if (resource.getType() != IResource.FILE)
        return true;

      final boolean isResorceContentChanged = delta.getKind() == IResourceDelta.CHANGED // changed resource
          && ((delta.getFlags() & IResourceDelta.CONTENT) != 0); // content changes
      final boolean isResourceAdded = delta.getKind() == IResourceDelta.ADDED;
      // only interested in valid(accessible) files here.
      if ((isResorceContentChanged || isResourceAdded) && resource.isAccessible()) {
        // Proceed supported files
        if (DeepCodeUtils.getInstance().isSupportedFileFormat(resource)) {
          filesChanged.add(resource);
        }
        // Proceed .dcignore and .gitignore files
        if (DeepCodeIgnoreInfoHolder.getInstance().is_ignoreFile(resource)) {
          ignoreFilesChanged.add(resource);
        }
      } else if (delta.getKind() == IResourceDelta.REMOVED) {
        // Proceed deleted supported files
        if (AnalysisData.getInstance().isFileInCache(resource)) {
          filesDeleted.add((IFile) resource);
        }
        // Proceed deleted .dcignore and .gitignore files
        if (DeepCodeIgnoreInfoHolder.getInstance().is_ignoreFile(resource)) {
          ignoreFilesChanged.add(resource);
        }
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
