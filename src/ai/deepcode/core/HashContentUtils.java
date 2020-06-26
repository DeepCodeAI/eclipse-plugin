package ai.deepcode.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.IResource;
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
