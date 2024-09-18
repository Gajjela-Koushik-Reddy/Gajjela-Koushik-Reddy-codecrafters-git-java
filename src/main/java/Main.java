import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Scanner;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.

    // Uncomment this block to pass the first stage

    final String command = args[0];

    switch (command) {
      case "init" -> {
        final File root = new File(".git");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");

        try {
          head.createNewFile();
          Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
          System.out.println("Initialized git directory");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      case "cat-file" -> {
        final String subCommand = args[1];

        if (subCommand.equals("-p")) {
          final String hashCode = args[2];
          String folderName = new StringBuffer(hashCode).substring(0, 2);
          String fileName = new StringBuffer(hashCode).substring(2);
          File blobFile = new File("./.git/objects/" + folderName + "/" + fileName);

          try {
            InputStream fileContents = new InflaterInputStream(new FileInputStream(blobFile));
            Scanner reader = new Scanner(fileContents);

            String firstLine = reader.nextLine();
            System.out.print(firstLine.substring(firstLine.indexOf('\0') + 1));

            while (reader.hasNextLine()) {
              System.out.print(reader.nextLine());
            }

            reader.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }

      case "hash-object" -> {
        final String subCommand = args[1];

        if (subCommand.equals("-w")) {
          String fileName = args[2];
          File file = new File(fileName);

          // read contents of the file and make the hash
          try {
            Scanner readFile = new Scanner(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String fileContent = "blob" + file.length() + "\0" + readFile.nextLine();

            String hashCode = digest.digest(fileContent.getBytes()).toString();
            String writeFolderName = new StringBuffer(hashCode).substring(0, 2);
            String writeFileName = new StringBuffer(hashCode).substring(2);

            // write contents to file
            new File("./.git/objects", writeFolderName).mkdir();
            File finalFile = new File("./.git/objects" + writeFolderName, writeFileName);
            

            finalFile.createNewFile();
            Files.write(finalFile.toPath(), fileContent.getBytes());

            System.out.print(hashCode);

            readFile.close();

          } catch (Exception e) {
            throw (new RuntimeException(e));
          }
        }
      }
      default -> System.out.println("Unknown command: " + command);
    }
  }
}
