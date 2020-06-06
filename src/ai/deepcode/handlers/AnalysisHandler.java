package ai.deepcode.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import ai.deepcode.javaclient.DeepCodeRestApi;
import ai.deepcode.javaclient.requests.FileContent;
import ai.deepcode.javaclient.requests.FileContentRequest;
import ai.deepcode.javaclient.responses.CreateBundleResponse;
import ai.deepcode.javaclient.responses.GetAnalysisResponse;

public class AnalysisHandler extends AbstractHandler {

	private static final String loggedToken = "aeedc7d1c2656ea4b0adb1e215999f588b457cedf415c832a0209c9429c7636e";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		for (IProject project : workspace.getRoot().getProjects()) {
			if (!project.isAccessible())
				continue;
			System.out.println("------ Active Project: " + project);

			List<FileContent> filesToProcced = new ArrayList<FileContent>();
			try {
				project.accept(new IResourceVisitor() {
					@Override
					public boolean visit(IResource resource) throws CoreException {
						if (resource instanceof IFile) {
							IFile file = (IFile) resource;
							if (isSupportedFile(file)) {
								String filePath = file.getProjectRelativePath().toString();
								System.out.println(filePath);
								String fileContent = getFileContent(file);
								System.out.println(fileContent);

								if (!filePath.startsWith("/"))
									filePath = "/" + filePath;
								filesToProcced.add(new FileContent(filePath, fileContent));
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
					new FileContentRequest(filesToProcced));
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

		}
		return null;
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

	private String getFileContent(IFile file) {
		try {
			// System.out.println(Paths.get(file.getLocationURI()));
			return Files.readString(Paths.get(file.getLocationURI()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
