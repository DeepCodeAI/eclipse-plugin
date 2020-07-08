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
    if (event == null || event.getDelta() == null) {
      return;
    }

    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {

      IResourceDelta rootDelta = event.getDelta();
      final Set<IResource> filesChanged = new HashSet<>();
      final Set<IResource> dcignoreFilesChanged = new HashSet<>();

      try {
        rootDelta.accept(new FileChangedOrCreatedVisitor(filesChanged, dcignoreFilesChanged));
        rootDelta.accept(new ProjectOpenedVisitor());

      } catch (CoreException e) {
        DCLogger.getInstance().logWarn(e.getMessage());
      }

      // Reanalyze files affected
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

      // Rescan .dcignore files changed
      for (IResource dcignoreFile : dcignoreFilesChanged) {
        RunUtils.getInstance().runInBackgroundCancellable(dcignoreFile, "Updating ignored files list...",
            (progress) -> DeepCodeIgnoreInfoHolder.getInstance().update_dcignoreFileContent(dcignoreFile));
      }
      // rescan each project affected
      dcignoreFilesChanged.stream().map(PDU.getInstance()::getProject).distinct().forEach(project -> RunUtils
          .getInstance().rescanInBackgroundCancellableDelayed(project, PDU.DEFAULT_DELAY_SMALL, true));

    }

  }

  private final class FileChangedOrCreatedVisitor implements IResourceDeltaVisitor {
    private final Set<IResource> filesChanged;
    private final Set<IResource> dcignoreFilesChanged;

    private FileChangedOrCreatedVisitor(Set<IResource> filesChanged, Set<IResource> dcignoreFilesChanged) {
      this.filesChanged = filesChanged;
      this.dcignoreFilesChanged = dcignoreFilesChanged;
    }

    public boolean visit(IResourceDelta delta) {
      // only interested in changed or added resources (not removed)
      if (!(delta.getKind() == IResourceDelta.CHANGED || delta.getKind() == IResourceDelta.ADDED))
        return true;
      // only interested in content changes
      if ((delta.getFlags() & IResourceDelta.CONTENT) == 0)
        return true;
      IResource resource = delta.getResource();

      // only interested in files
      if (resource.getType() != IResource.FILE)
        return true;

      // Proceed supported files
      if (DeepCodeUtils.getInstance().isSupportedFileFormat((IFile) resource)) {
        filesChanged.add(resource);
      }

      // Proceed .dcignore files
      if (DeepCodeIgnoreInfoHolder.getInstance().is_dcignoreFile((IFile) resource)) {
        dcignoreFilesChanged.add(resource);
      }

      return true;
    }
  }

  private final class ProjectOpenedVisitor implements IResourceDeltaVisitor {
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
      // Initial silent logging check before analysis.
      if (LoginUtils.getInstance().isLogged(project, false)) {
        RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project);
      }
    }
  }

}
