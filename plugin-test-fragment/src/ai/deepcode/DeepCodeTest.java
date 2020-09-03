package ai.deepcode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import ai.deepcode.core.AnalysisData;
import ai.deepcode.core.DeepCodeParams;
import ai.deepcode.core.LoginUtils;
import ai.deepcode.core.RunUtils;

public class DeepCodeTest {

  // !!! Will works only with already logged Token
  protected static final String loggedToken = System.getenv("DEEPCODE_API_KEY");
  protected static final String loggedToken_DeepCoded = System.getenv("DEEPCODE_API_KEY_STAGING");

  /**
   * Perform pre-test initialization.
   */
  @Before
  public void setUp() {
    showInfo("-------------------setUp--------------------");
    DeepCodeParams.getInstance().setSessionToken(loggedToken);
    showInfo("Token set to: " + DeepCodeParams.getInstance().getSessionToken());
  }

  @Test
  public void _10_LoginTest() {
    showInfo("\n-------------------10_Login_Tests--------------------");
    // waitForJobs();
    // boolean isLogged[] = {false};
    // new BackgroundJob("LoginTest", () -> {
    // //delay(5000);
    // isLogged[0] = LoginUtils.getInstance().checkLogin(null, false);
    // }).schedule();
    // waitForJobs();
    assertTrue("Not logged to deepcode.ai", LoginUtils.getInstance().checkLogin(null, false));

    showInfo("-------------------testMalformedToken--------------------");
    DeepCodeParams.getInstance().setSessionToken("blablabla");
    assertFalse("Login with malformed Token should fail", LoginUtils.getInstance().checkLogin(null, false));

    showInfo("-------------------testNotLoggedToken--------------------");
    LoginUtils.getInstance().requestNewLogin(null, false);
    waitForJobs();
    assertFalse("Login with newly requested but not yet logged token should fail",
        LoginUtils.getInstance().checkLogin(null, false));
  }

  @Test
  public void _20_ProjectTest() throws CoreException {
    showInfo("\n-------------------20_Project_Tests--------------------");

    String name = "DeepCodeTestProject";
    IProjectDescription projectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(name);
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);

    project.create(projectDescription, new NullProgressMonitor());
    project.open(new NullProgressMonitor());
    showInfo("Project `DeepCodeTestProject` opened.");

    IFile file = project.getFile("cppTestFile.cpp");
    InputStream source = getClass().getClassLoader().getResourceAsStream("testData/sampleFile.cpp");
    file.create(source, IFile.FORCE, null);
    showInfo("`cppTestFile.cpp` file created.");

    waitForJobs();

    // check project with NOT given consent should not be analyzed
    showInfo("-------------------testNoAnalysisForProjectWithoutConsent--------------------");
    assertFalse("Consent should NOT be in given at this stage.", DeepCodeParams.getInstance().consentGiven(project));
    Set<Object> allCachedProject = AnalysisData.getInstance().getAllCachedProject();
    assertTrue("Project without given Consent should NOT be in cache.", allCachedProject.isEmpty());

    DeepCodeParams.getInstance().setConsentGiven(project);
    showInfo("Project `DeepCodeTestProject` CONSENT given.");
    
    RunUtils.getInstance().asyncAnalyseProjectAndUpdatePanel(project);

    waitForJobs();

    showInfo("-------------------testProjectInCache--------------------");
    allCachedProject = AnalysisData.getInstance().getAllCachedProject();
    assertTrue("Current Project should be in cache.",
        allCachedProject.size() == 1 && allCachedProject.contains(project));

    showInfo("-------------------testFileInCache--------------------");
    assertTrue("Test file is not in cache", AnalysisData.getInstance().isFileInCache(file));
    final Set<Object> filesWithSuggestions = AnalysisData.getInstance().getAllFilesWithSuggestions(project);
    assertFalse("List of Files with suggestions is empty", filesWithSuggestions.isEmpty());
    assertTrue("Test file has no suggestions in cache", filesWithSuggestions.contains(file));
  }



  /**
   * Process UI input but do not return for the specified time interval.
   *
   * @param waitTimeMillis the number of milliseconds
   */
  private void delay(long waitTimeMillis) {
    Display display = Display.getCurrent();

    // If this is the UI thread,
    // then process input.

    if (display != null) {
      long endTimeMillis = System.currentTimeMillis() + waitTimeMillis;
      while (System.currentTimeMillis() < endTimeMillis) {
        if (!display.readAndDispatch())
          display.sleep();
      }
      display.update();
    }
    // Otherwise, perform a simple sleep.
    else {
      try {
        Thread.sleep(waitTimeMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Wait until all background tasks are complete.
   */
  private void waitForJobs() {
    while (!Job.getJobManager().isIdle())
      delay(1000);
  }

  private void showInfo(String message) {
    // DCLogger.getInstance().logInfo(message)
    System.out.println("\n" + getCurrentTime() + message);
  }

  private static final SimpleDateFormat mmssSSS = new SimpleDateFormat("mm:ss,SSS");

  private String getCurrentTime() {
    return "[" + mmssSSS.format(System.currentTimeMillis()) + "] ";
  }

  private class BackgroundJob extends Job {

    private Runnable runnable;

    public BackgroundJob(String title, @NotNull Runnable runnable) {
      super(title);
      this.runnable = runnable;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      runnable.run();;
      return Status.OK_STATUS;
    }

  }
}
