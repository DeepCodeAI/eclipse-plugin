package ai.deepcode.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import ai.deepcode.javaclient.DeepCodeRestApi;
import ai.deepcode.javaclient.requests.FileContent;
import ai.deepcode.javaclient.requests.FileContentRequest;
import ai.deepcode.javaclient.responses.AnalysisResults;
import ai.deepcode.javaclient.responses.CreateBundleResponse;
import ai.deepcode.javaclient.responses.FileRange;
import ai.deepcode.javaclient.responses.FileSuggestions;
import ai.deepcode.javaclient.responses.GetAnalysisResponse;
import ai.deepcode.javaclient.responses.Suggestion;
import ai.deepcode.javaclient.responses.Suggestions;

public class AnalysisHandler extends AbstractHandler {

	private static final String loggedToken = "aeedc7d1c2656ea4b0adb1e215999f588b457cedf415c832a0209c9429c7636e";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		for (IProject project : workspace.getRoot().getProjects()) {
			if (!project.isAccessible())
				continue;
			System.out.println("------ Active Project: " + project);

			Map<IResource, FileContent> filesToProcced = new HashMap<IResource, FileContent>();
			try {
				project.accept(new IResourceVisitor() {
					@Override
					public boolean visit(IResource resource) throws CoreException {
						if (resource instanceof IFile) {
							IFile file = (IFile) resource;
							if (isSupportedFile(file)) {
								String filePath = getDeepCodedFilePath(file);
								System.out.println(filePath);
								String fileContent = getFileContent(file);
								System.out.println(fileContent);

								filesToProcced.put(resource, new FileContent(filePath, fileContent));
							}
							return false;
						}
						return true;
					}
				});
			} catch (CoreException e) {
				System.out.println(e);
			}

			final CreateBundleResponse createBundleResponse = DeepCodeRestApi.createBundle(loggedToken,
					new FileContentRequest(new ArrayList<FileContent>(filesToProcced.values())));
			System.out.println("\nCreate Bundle request: " + createBundleResponse);
			final String bundleId = createBundleResponse.getBundleId();

			GetAnalysisResponse response;
			int counter = 0;
			do {
				if (counter > 0)
					delay(1000);
				response = DeepCodeRestApi.getAnalysis(loggedToken, bundleId, 1, false);
				System.out.println(response.toString());
				if (response.getStatusCode() != 200 || counter > 10)
					break;
				counter++;
			} while (!response.getStatus().equals("DONE"));

			reportAnalysisResults(filesToProcced.keySet(), response.getAnalysisResults());

		}
		return null;
	}

	private void reportAnalysisResults(Set<IResource> files, AnalysisResults analysisResults) {
//		files.forEach(f -> {
//			try {
//				f.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
//			} catch (CoreException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		});
		final Suggestions suggestions = analysisResults.getSuggestions();
		if (suggestions == null) {
			System.out.println("Suggestions is empty for: " + analysisResults);
			return;
		}
		for (IResource file : files) {
			FileSuggestions fileSuggestions = analysisResults.getFiles().get(getDeepCodedFilePath(file));
			if (fileSuggestions == null) {
				continue;
			}
			for (String suggestionIndex : fileSuggestions.keySet()) {
				final Suggestion suggestion = suggestions.get(suggestionIndex);
				if (suggestion == null) {
					System.out.println("Suggestion not found for suggestionIndex: " + suggestionIndex
							+ "\nanalysisResults: " + analysisResults);
					continue;
				}
				for (FileRange fileRange : fileSuggestions.get(suggestionIndex)) {
					final int startRow = fileRange.getRows().get(0);
					final int endRow = fileRange.getRows().get(1);
					final int startCol = fileRange.getCols().get(0) - 1; // inclusive
					final int endCol = fileRange.getCols().get(1);

					try {
						IMarker m = file.createMarker("ai.deepcode.deepcodemarker");
						
//						m.setAttribute(IMarker.LINE_NUMBER, startRow);
						ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
						ITextFileBuffer textFileBuffer = manager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
						IDocument document = textFileBuffer.getDocument();
						
						try {
							int lineOffset = 0;
							lineOffset = document.getLineOffset(startRow - 1 ); // The first line has the line number 0.
							m.setAttribute(IMarker.CHAR_START, lineOffset + startCol);
							lineOffset = document.getLineOffset(endRow - 1); // The first line has the line number 0.
							m.setAttribute(IMarker.CHAR_END, lineOffset + endCol);
						} catch (BadLocationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
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

	private String getDeepCodedFilePath(IResource file) {
		String filePath = file.getProjectRelativePath().toString();
		if (!filePath.startsWith("/"))
			filePath = "/" + filePath;
		return filePath;
	}

	public static void delay(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			System.out.println("InterruptedException: " + e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	private static Set<String> supportedExtensions = new HashSet<>(Arrays.asList("cc", "htm", "cpp", "cxx", "c", "vue",
			"h", "hpp", "hxx", "es6", "js", "py", "es", "jsx", "java", "tsx", "html", "ts"));
	private static Set<String> supportedConfigFiles = new HashSet<>(
			Arrays.asList("pylintrc", "ruleset.xml", ".eslintrc.json", ".pylintrc", ".eslintrc.js", "tslint.json",
					".pmdrc.xml", ".ruleset.xml", ".eslintrc.yml"));
	private static final long MAX_FILE_SIZE = 5242880; // 5MB in bytes

	private boolean isSupportedFile(IFile file) {
		return (supportedExtensions.contains(file.getFileExtension()) || supportedConfigFiles.contains(file.getName()))
				&& file.getLocation().toFile().length() < MAX_FILE_SIZE;
	}

	private static final Map<IResource, String> FILE2CONTENT_MAP = new ConcurrentHashMap<IResource, String>();

	private String getFileContent(IResource file) {
		return FILE2CONTENT_MAP.computeIfAbsent(file, f -> {
			try {
				// System.out.println(Paths.get(file.getLocationURI()));
				return Files.readString(Paths.get(f.getLocationURI()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
