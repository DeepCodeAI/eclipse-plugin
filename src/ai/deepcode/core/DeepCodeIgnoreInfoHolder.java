package ai.deepcode.core;

import org.jetbrains.annotations.NotNull;
import ai.deepcode.javaclient.core.DeepCodeIgnoreInfoHolderBase;

public final class DeepCodeIgnoreInfoHolder extends DeepCodeIgnoreInfoHolderBase {

  private static final DeepCodeIgnoreInfoHolder INSTANCE = new DeepCodeIgnoreInfoHolder();

  public static DeepCodeIgnoreInfoHolder getInstance() {
    return INSTANCE;
  }

  private DeepCodeIgnoreInfoHolder() {
    super(HashContentUtils.getInstance());
  }

  @Override
  protected String getFilePath(@NotNull Object file) {
    return PDU.toIFile(file).getFullPath().toString();
  }

  @Override
  protected String getFileName(@NotNull Object file) {
    return PDU.getInstance().getFileName(file);
  }

  @Override
  protected String getDirPath(@NotNull Object file) {
    return PDU.toIFile(file).getParent().getFullPath().toString();
  }


}
