package ai.deepcode.core;

import ai.deepcode.javaclient.core.DCLoggerBase;

public class DCLogger extends DCLoggerBase {

  protected DCLogger() {
    super(() -> System.out::println, () -> System.out::println, () -> {return true;}, () -> {return true;} );
  }

  private static final DCLogger INSTANCE = new DCLogger();

  public static DCLogger getInstance() {
    return INSTANCE;
  }

  @Override
  protected String getExtraInfo() {
    return "[" + Thread.currentThread().getName() + "] \n";
  }

}
