package ai.deepcode.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import ai.deepcode.javaclient.DeepCodeRestApi;
import ai.deepcode.javaclient.requests.FileContent;
import ai.deepcode.javaclient.requests.FileContentRequest;
import ai.deepcode.javaclient.requests.FileHash2ContentRequest;
import ai.deepcode.javaclient.requests.FileHashRequest;
import ai.deepcode.javaclient.responses.AnalysisResults;
import ai.deepcode.javaclient.responses.CreateBundleResponse;
import ai.deepcode.javaclient.responses.EmptyResponse;
import ai.deepcode.javaclient.responses.FileRange;
import ai.deepcode.javaclient.responses.FileSuggestions;
import ai.deepcode.javaclient.responses.GetAnalysisResponse;
import ai.deepcode.javaclient.responses.Suggestion;
import ai.deepcode.javaclient.responses.Suggestions;

import static ai.deepcode.core.DCLogger.info;
import static ai.deepcode.core.DCLogger.warn;

public class AnalysisJob extends Job {

  private static final DCLogger dcLogger = DCLogger.getInstance();
  // FIXME
  private static final String loggedToken = "aeedc7d1c2656ea4b0adb1e215999f588b457cedf415c832a0209c9429c7636e";

  public AnalysisJob(String name) {
    super(name);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    for (IProject project : workspace.getRoot().getProjects()) {
      if (!project.isAccessible())
        continue;
      dcLogger.logInfo("------ Active Project: " + project);

      Collection<IResource> filesToProcced = getAllFilesSupported(project);

      final FileHashRequest fileHashRequest = new FileHashRequest(
          filesToProcced.stream().collect(Collectors.toMap(f -> HashContentUtils.getDeepCodedFilePath(f), f -> HashContentUtils.getHash(f))));
      final CreateBundleResponse createBundleResponse = DeepCodeRestApi.createBundle(loggedToken, fileHashRequest);

      dcLogger.logInfo(
          "\nCreate Bundle response: " + "\n status = " + createBundleResponse.getStatusDescription() + "\n bundleID = "
              + createBundleResponse.getBundleId() + "\n missingFiles = " + createBundleResponse.getMissingFiles());
      final String bundleId = createBundleResponse.getBundleId();
      final List<String> missingFiles = createBundleResponse.getMissingFiles();

      List<FileHash2ContentRequest> listHash2Content = new ArrayList<>();
      for (String path : missingFiles) {
        String fileText =
            HashContentUtils.getFileContent(filesToProcced.stream().filter(f -> HashContentUtils.getDeepCodedFilePath(f).equals(path)).findFirst().get());
        listHash2Content.add(new FileHash2ContentRequest(HashContentUtils.getHash(fileText), fileText));
      }

      if (!listHash2Content.isEmpty()) {
        EmptyResponse uploadFilesResponse = DeepCodeRestApi.UploadFiles(loggedToken, bundleId, listHash2Content);
      }

      GetAnalysisResponse response;
      int counter = 0;
      do {
        if (counter > 0)
          delay(1000);
        response = DeepCodeRestApi.getAnalysis(loggedToken, bundleId, 1, false);
        dcLogger.logInfo(response.toString());
        if (response.getStatusCode() != 200 || counter > 10)
          break;
        counter++;
      } while (!response.getStatus().equals("DONE"));

      reportAnalysisResults(filesToProcced, response.getAnalysisResults());

    }
    return Status.OK_STATUS;
  }

  private Collection<IResource> getAllFilesSupported(IProject project) {
    Collection<IResource> filesToProcced = new ArrayList<>();
    try {
      project.accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile) {
            IFile file = (IFile) resource;
            if (isSupportedFile(file)) {
              filesToProcced.add(resource);
            }
            return false;
          }
          return true;
        }
      });
    } catch (CoreException e) {
      warn(e.toString());
    }
    return filesToProcced;
  }

  private void reportAnalysisResults(Collection<IResource> files, AnalysisResults analysisResults) {
    files.forEach(f -> {
      try {
        f.deleteMarkers("ai.deepcode.deepcodemarker", true, IResource.DEPTH_INFINITE);
      } catch (CoreException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    });
    final Suggestions suggestions = analysisResults.getSuggestions();
    if (suggestions == null) {
      warn("Suggestions is empty for: " + analysisResults);
      return;
    }
    for (IResource file : files) {
      FileSuggestions fileSuggestions = analysisResults.getFiles().get(HashContentUtils.getDeepCodedFilePath(file));
      if (fileSuggestions == null) {
        continue;
      }
      for (String suggestionIndex : fileSuggestions.keySet()) {
        final Suggestion suggestion = suggestions.get(suggestionIndex);
        if (suggestion == null) {
          warn(
              "Suggestion not found for suggestionIndex: " + suggestionIndex + "\nanalysisResults: " + analysisResults);
          continue;
        }
        for (FileRange fileRange : fileSuggestions.get(suggestionIndex)) {
          final int startRow = fileRange.getRows().get(0);
          final int endRow = fileRange.getRows().get(1);
          final int startCol = fileRange.getCols().get(0) - 1; // inclusive
          final int endCol = fileRange.getCols().get(1);

          try {
            IMarker m = file.createMarker("ai.deepcode.deepcodemarker");

            m.setAttribute(IMarker.LINE_NUMBER, startRow);
            // file have to be opened in the editor to get it Document
            // ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
            // ITextFileBuffer textFileBuffer = manager.getTextFileBuffer(file.getFullPath(),
            // LocationKind.IFILE);
            // IDocument document = textFileBuffer.getDocument();

            int lineOffset = 0;
            lineOffset = HashContentUtils.getLineOffset(startRow, HashContentUtils.getFileContent(file));
            m.setAttribute(IMarker.CHAR_START, lineOffset + startCol);
            lineOffset = HashContentUtils.getLineOffset(endRow, HashContentUtils.getFileContent(file));
            m.setAttribute(IMarker.CHAR_END, lineOffset + endCol);

            m.setAttribute(IMarker.MESSAGE, "DeepCode: " + suggestion.getMessage());
            m.setAttribute(IMarker.SEVERITY, suggestion.getSeverity() - 1);
          } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
  }

  public static void delay(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      warn("InterruptedException: " + e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  private static Set<String> supportedExtensions = new HashSet<>(Arrays.asList("cc", "htm", "cpp", "cxx", "c", "vue",
      "h", "hpp", "hxx", "es6", "js", "py", "es", "jsx", "java", "tsx", "html", "ts"));
  private static Set<String> supportedConfigFiles = new HashSet<>(Arrays.asList("pylintrc", "ruleset.xml",
      ".eslintrc.json", ".pylintrc", ".eslintrc.js", "tslint.json", ".pmdrc.xml", ".ruleset.xml", ".eslintrc.yml"));
  private static final long MAX_FILE_SIZE = 5242880; // 5MB in bytes

  // FIXME
  private boolean isSupportedFile(IFile file) {
    return (supportedExtensions.contains(file.getFileExtension()) || supportedConfigFiles.contains(file.getName()))
        && file.getLocation().toFile().length() < MAX_FILE_SIZE;
  }

}
