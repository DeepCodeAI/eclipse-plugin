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


  private static final Map<IResource, String> FILE2CONTENT_MAP = new ConcurrentHashMap<IResource, String>();

  static String getFileContent(IResource file) {
    return FILE2CONTENT_MAP.computeIfAbsent(file, f -> {
      try {
        // System.out.println(Paths.get(file.getLocationURI()));
        return Files.readString(Paths.get(f.getLocationURI()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  static String getDeepCodedFilePath(IResource file) {
    String filePath = file.getProjectRelativePath().toString();
    if (!filePath.startsWith("/"))
      filePath = "/" + filePath;
    return filePath;
  }

  static int getLineOffset(int lineNum, String fileContent) {
    int prevLineNum = lineNum - 1;
    // fileContent.lines().limit(prevLineNum).forEach(s -> System.out.println(s.length() + s));
    // TODO optimize
    int lineSeparatorLength = fileContent.indexOf("\r\n") == -1 ? 1 : 2;
    // `\n`|`\r`|`\r\n` should be also counted
    return fileContent.lines().limit(prevLineNum).mapToInt(String::length).sum() + prevLineNum * lineSeparatorLength;
  }

  static String getHash(IResource file) {
    return getHash(getFileContent(file));
  }

  static String getHash(String fileText) {
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    byte[] encodedHash = messageDigest.digest(fileText.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(encodedHash);
  }

  // https://www.baeldung.com/sha-256-hashing-java#message-digest
  private static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1)
        hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }

  @Override
  public @NotNull String doGetFileContent(@NotNull Object file) {
    // TODO Auto-generated method stub
    return null;
  }

}
