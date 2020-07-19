package ai.deepcode.quickfix;

import java.util.regex.Pattern;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.jetbrains.annotations.NotNull;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.HashContentUtils;
import ai.deepcode.core.PDU;

public class IgnoreSugestion implements IMarkerResolutionGenerator {

  @Override
  public IMarkerResolution[] getResolutions(IMarker marker) {
    return new IMarkerResolution[] {new QuickFix("DeepCode: Ignore this particular suggestion."),
        new QuickFix("DeepCode: Ignore this suggestion in current file (" + marker.getResource().getName() + ")")};
  }
}


class QuickFix implements IMarkerResolution {
  private String label;
  private final boolean isFileIntention;

  QuickFix(String label) {
    this.isFileIntention = label.contains("current file");
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public void run(IMarker marker) {
    final String fullSuggestionId = marker.getAttribute("fullSuggestionId", "");
    final int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1) - 1;
    final IFile file = PDU.toIFile(marker.getResource());
    int insertPosition = PDU.getInstance().getLineStartOffset(file, lineNumber);

    final String lineText = getLineText(file, lineNumber);

    String prefix = getLeadingSpaces(lineText) + getLineCommentPrefix(file);
    String postfix = "\n";

    if (lineNumber > 0) {
      String prevLine = getLineText(file, lineNumber - 1);
      final Pattern ignorePattern = Pattern.compile(".*" + getLineCommentPrefix(file) + ".*deepcode\\s?ignore.*");
      if (ignorePattern.matcher(prevLine).matches()) {
        prefix = ",";
        postfix = "";
        insertPosition -= 1;
      }
    }
    final int finalInsertPosition = insertPosition;
    final String finalPostfix = postfix; 

    final String[] splitedId = fullSuggestionId.split("%2F");
    final String suggestionId = splitedId[splitedId.length - 1];

    final String ignoreCommand = prefix + (prefix.endsWith(" ") ? "" : " ") + (isFileIntention ? "file " : "")
        + "deepcode ignore " + suggestionId + ": ";
    final String ignoreDescription = "<please specify a reason of ignoring this>";

    IWorkbench iworkbench = PlatformUI.getWorkbench();
    if (iworkbench == null) {
      DCLogger.getInstance().logWarn("IWorkbench is NULL");
      return;
    }

    // Run in UI thread
    iworkbench.getDisplay().asyncExec(() -> {
      IWorkbenchWindow iworkbenchwindow = iworkbench.getActiveWorkbenchWindow();
      if (iworkbenchwindow == null) {
        DCLogger.getInstance().logWarn("IWorkbenchWindow is NULL");
        return;
      }
      IWorkbenchPage page = iworkbenchwindow.getActivePage();
      if (page == null) {
        DCLogger.getInstance().logWarn("IWorkbenchPage is NULL");
        return;
      }

      IEditorPart editorPart = null;
      try {
        editorPart = IDE.openEditor(page, file, true);
      } catch (PartInitException e) {
        DCLogger.getInstance().logWarn(e.getMessage() + e.getStackTrace());
      }
      if (editorPart == null) {
        DCLogger.getInstance().logWarn("IEditorPart is NULL");
        return;
      }
      ITextEditor editor = (ITextEditor) editorPart.getAdapter(ITextEditor.class);
      if (editor == null) {
        DCLogger.getInstance().logWarn("ITextEditor is NULL");
        return;
      }

      // actually change the source file
      IDocumentProvider provider = editor.getDocumentProvider();
      IDocument document = provider.getDocument(editor.getEditorInput());
      try {
        document.replace(finalInsertPosition, 0, ignoreCommand + ignoreDescription + finalPostfix);
      } catch (BadLocationException e) {
        DCLogger.getInstance().logWarn(e.getMessage() + e.getStackTrace());;
      }

      // select description
      int caretOffset = finalInsertPosition + ignoreCommand.length();
      editor.selectAndReveal(caretOffset, ignoreDescription.length());
    });
  }

  @NotNull
  private static String getLineText(@NotNull Object file, int line) {
    String fileContent = HashContentUtils.getInstance().getFileContent(file);
    return fileContent.lines().skip(line).findFirst().orElseThrow();
  }

  private static final String DEFAULT_LINE_COMMENT_PREFIX = "//";

  // TODO make it not hard-coded
  @NotNull
  private static String getLineCommentPrefix(@NotNull Object file) {
    if (PDU.toIFile(file).getFileExtension().equals("py"))
      return "#";
    // htm, html : HTML comment begins with <!–– and the comment closes with ––>
    return DEFAULT_LINE_COMMENT_PREFIX;
  }

  @NotNull
  private static String getLeadingSpaces(@NotNull String lineText) {
    int index = 0;
    while (index < lineText.length() && Character.isWhitespace(lineText.charAt(index)))
      index++;
    return lineText.substring(0, index);
  }
}
