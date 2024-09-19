import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Scanner;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.

    // Uncomment this block to pass the first stage

    final String command = args[0];
    final File CURRENT_DIRECTORY = new File(".");

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
          File currentDir = new File(".");

          // read contents of the file and make the hash
          try {
            File file = new File(currentDir, fileName);
            FileInputStream readFile = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] fileContent = readFile.readAllBytes();

            // String fileContent = "blob" + file.length() + "\0" + readFile.nextLine();
            digest.update("blob".getBytes());
            digest.update(" ".getBytes());
            digest.update(String.valueOf(file.length()).getBytes());
            digest.update("\0".getBytes());
            digest.update(fileContent);

            byte[] hashCodeByte = digest.digest();
            String hashCode = HexFormat.of().formatHex(hashCodeByte);
            String writeFolderName = new StringBuffer(hashCode).substring(0, 2);
            String writeFileName = new StringBuffer(hashCode).substring(2);

            // write contents to file
            new File("./.git/objects", writeFolderName).mkdir();
            File finalFile = new File("./.git/objects/" + writeFolderName, writeFileName);

            finalFile.createNewFile();

            DeflaterOutputStream writeFile = new DeflaterOutputStream(new FileOutputStream(finalFile));
            writeFile.write("blob".getBytes());
            writeFile.write(" ".getBytes());
            writeFile.write(String.valueOf(file.length()).getBytes());
            writeFile.write("\0".getBytes());
            writeFile.write(fileContent);

            System.out.print(hashCode);

            readFile.close();
            writeFile.close();

          } catch (Exception e) {
            throw (new RuntimeException(e));
          }
        }
      }
      case "ls-tree" -> {
        final String subCommand = args[1];
        if (subCommand.equals("--name-only")) {
          String hashCode = args[2];
          String folderName = new StringBuffer(hashCode).substring(0, 2);
          String fileName = new StringBuffer(hashCode).substring(2);
          File file = new File(CURRENT_DIRECTORY.toString() + "/.git/objects/" + folderName, fileName);

          try {
            InflaterInputStream decompressedFile = new InflaterInputStream(new FileInputStream(file));

            int c;
            final var decompressedContent = new StringBuilder();

            while ((c = decompressedFile.read()) != -1) {
              decompressedContent.append((char) c);
            }

            String[] allLines = decompressedContent.toString().split("\0");
            ArrayList<String> allLinesArr = new ArrayList<>(List.of(allLines));
            List<String> allContent = allLinesArr.subList(1, allLinesArr.size() - 1);

            for (String line : allContent) {
              String[] value = line.split(" ");
              System.out.println(value[1]);
            }

            decompressedFile.close();
          } catch (IOException e) {
            throw (new RuntimeException(e));
          }
        }
      }
      default -> System.out.println("Unknown command: " + command);
    }
  }
}
