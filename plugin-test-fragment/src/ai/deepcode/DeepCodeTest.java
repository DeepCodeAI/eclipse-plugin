package ai.deepcode;

import static org.junit.Assert.assertTrue;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.log.LoggerFactory;
import ai.deepcode.core.DCLogger;
import ai.deepcode.core.DeepCodeParams;

public class DeepCodeTest {
  
  // !!! Will works only with already logged sessionToken
  protected static final String loggedToken =
      "aeedc7d1c2656ea4b0adb1e215999f588b457cedf415c832a0209c9429c7636e";

  /**
   * Perform pre-test initialization.
   */
  @Before
  public void setUp() {
    System.out.println("-------------------setUp--------------------");
    DeepCodeParams.getInstance().setSessionToken(loggedToken);
  }

  @Test
  public void _01_initialSetupTest() {
    // DCLogger.getInstance().logInfo("-------------------test--------------------")
    System.out.println("-------------------test--------------------");
    System.out.println(DeepCodeParams.getInstance().getSessionToken());
  }

  
  /**
   * Process UI input but do not return for the
   * specified time interval.
   *
   * @param waitTimeMillis the number of milliseconds
   */
  private void delay(long waitTimeMillis) {
     Display display = Display.getCurrent();

     // If this is the UI thread,
     // then process input.

     if (display != null) {
        long endTimeMillis =
           System.currentTimeMillis() + waitTimeMillis;
        while (System.currentTimeMillis() < endTimeMillis)
        {
           if (!display.readAndDispatch())
              display.sleep();
        }
        display.update();
     }
     // Otherwise, perform a simple sleep.
     else {
        try {
           Thread.sleep(waitTimeMillis);
        }
        catch (InterruptedException e) {
           // Ignored.
        }
     }
  }

  /**
   * Wait until all background tasks are complete.
   */
  public void waitForJobs() {
     while (!Job.getJobManager().isIdle())
        delay(1000);
  }
}
