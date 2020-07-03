package ai.deepcode.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import ai.deepcode.javaclient.core.HashContentUtilsBase;
import ai.deepcode.javaclient.core.PlatformDependentUtilsBase;

public class HashContentUtils extends HashContentUtilsBase {

  private final static HashContentUtils INSTANCE = new HashContentUtils(PDU.getInstance());

  private HashContentUtils(@NotNull PlatformDependentUtilsBase platformDependentUtils) {
    super(platformDependentUtils);
  }

  public static HashContentUtils getInstance() {
    return INSTANCE;
  }

  @Override
  public @NotNull String doGetFileContent(@NotNull Object file) {
    try {
      // System.out.println(Paths.get(file.getLocationURI()));
      return Files.readString(Paths.get(PDU.toIFile(file).getLocationURI()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
