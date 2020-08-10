package ai.deepcode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.text.SimpleDateFormat;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import ai.deepcode.core.DeepCodeParams;
import ai.deepcode.core.LoginUtils;

public class DeepCodeTest {

  // !!! Will works only with already logged Token
  protected static final String loggedToken = "7803ae6756d34b5cec056616fd59f4d6e499fce7fc3ce6db5cfd07f6e893e23a";
  protected static final String loggedToken_DeepCoded = "3323bb63463aed49e194fcfe5455da9f338763ef2d8a6e2516ab5c81a184fa93";

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
    showInfo("-------------------Login_Test--------------------");
//    waitForJobs();
//    boolean isLogged[] = {false};
//    new BackgroundJob("LoginTest", () -> {
//      //delay(5000);
//      isLogged[0] = LoginUtils.getInstance().checkLogin(null, false);
//    }).schedule();
//    waitForJobs();
    assertTrue("Not logged to deepcode.ai", LoginUtils.getInstance().checkLogin(null, false));
    
    showInfo("-------------------testMalformedToken--------------------");
    DeepCodeParams.getInstance().setSessionToken("blablabla");
    assertFalse(
        "Login with malformed Token should fail",
        LoginUtils.getInstance().checkLogin(null, false));
    
    showInfo("-------------------testNotLoggedToken--------------------");
    LoginUtils.getInstance().requestNewLogin(null, false);
    waitForJobs();
    assertFalse(
        "Login with newly requested but not yet logged token should fail",
        LoginUtils.getInstance().checkLogin(null, false));

    DeepCodeParams.getInstance().setSessionToken(loggedToken);
    showInfo("Token set to: " + DeepCodeParams.getInstance().getSessionToken());    
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
        // Ignored.
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
