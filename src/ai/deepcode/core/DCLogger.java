package ai.deepcode.core;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import ai.deepcode.javaclient.core.DCLoggerBase;

public class DCLogger extends DCLoggerBase {

  private static final Bundle BUNDLE = FrameworkUtil.getBundle(DCLogger.class);
  private static final ILog LOGGER = Platform.getLog(BUNDLE);

  protected DCLogger() {
    super(INFO_SUPPLIER, WARN_SUPPLIER, () -> {return true;}, () -> {return true;} );
  }
  
  private static final Supplier<Consumer<String>> INFO_SUPPLIER = () -> {return DCLogger::doInfo;};

  private static final void doInfo(String message) {
    LOGGER.info(message);
    //System.out.println(message);
  };
  
  private static final Supplier<Consumer<String>> WARN_SUPPLIER = () -> {return DCLogger::doWarn;};
  
  private static final void doWarn(String message) {
    LOGGER.warn(message);
    //System.out.println(message);
  };
  
  
  private static final DCLogger INSTANCE = new DCLogger();

  public static DCLogger getInstance() {
    return INSTANCE;
  }

  @Override
  protected String getExtraInfo() {
    return "[" + Thread.currentThread().getName() + "]";
  }

}
