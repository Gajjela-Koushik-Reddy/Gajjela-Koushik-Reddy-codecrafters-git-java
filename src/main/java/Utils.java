import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;

/**
 * Utils
 */
public class Utils {

    public static byte[] writeBlob(File CURRENT_DIRECTORY, String fileName) throws Exception {
        File file = new File(CURRENT_DIRECTORY, fileName);

        FileInputStream readFile = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] fileContent = readFile.readAllBytes();

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

        readFile.close();
        writeFile.close();
        return hashCodeByte;
    }

    public static byte[] writeTree(File CURRENT_DIRECTORY) throws Exception {

        File[] fileList = CURRENT_DIRECTORY.listFiles();
        Stream<File> fileListStream = Arrays.stream(fileList);

        List<File> noGitFiles = fileListStream
                .filter(file -> !file.getName().equals(".git"))
                .sorted((o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()))
                .toList();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (File file : noGitFiles) {

            if (file.isFile()) {
                byte[] hash = writeBlob(CURRENT_DIRECTORY, file.getName());
                buffer.write("100644".getBytes());
                buffer.write(" ".getBytes());
                buffer.write(file.getName().getBytes());
                buffer.write("\0".getBytes());
                buffer.write(hash);
            } else {
                byte[] hash = writeTree(file);
                buffer.write("40000".getBytes());
                buffer.write(" ".getBytes());
                buffer.write(file.getName().getBytes());
                buffer.write("\0".getBytes());
                buffer.write(hash);
            }
        }

        byte[] treeItems = buffer.toByteArray();

        return writeRawObject(treeItems);
    }

    private static byte[] writeRawObject(byte[] data) throws Exception {
        String dirHash = "";

        // calculate the dirHash
        MessageDigest message = MessageDigest.getInstance("SHA-1");
        message.update(data);

        byte[] digest = message.digest();

        dirHash = HexFormat.of().formatHex(digest);
        String dirName = new StringBuffer(dirHash).substring(0, 2);
        String fileName = new StringBuffer(dirHash).substring(2);

        File fileCreated = new File("./.git/objects", dirName);
        fileCreated.mkdir();

        File finalFile = new File("./.git/objects/" + dirName, fileName);

        finalFile.createNewFile();

        DeflaterOutputStream writeFile = new DeflaterOutputStream(new FileOutputStream(finalFile));

        writeFile.write("tree".getBytes());
        writeFile.write(" ".getBytes());
        writeFile.write(String.valueOf(data.length).getBytes());
        writeFile.write("\0".getBytes());
        writeFile.write(data);

        writeFile.close();
        return digest;
    }

    public static byte[] commitTree(String treeSHA, String commitSHA, String message)
            throws IOException, NoSuchAlgorithmException {

        ByteArrayOutputStream content = new ByteArrayOutputStream();

        /*
         * tree {tree_sha}
         * {parents}
         * author {author_name} <{author_email}> {author_date_seconds}
         * {author_date_timezone}
         * committer {committer_name} <{committer_email}> {committer_date_seconds}
         * {committer_date_timezone}
         * 
         * {commit message}
         */
        content.write(("tree" + " " + treeSHA + "\n").getBytes());
        content.write(("parent " + commitSHA + "\n").getBytes());
        content.write(("author " + "author name <author@email.com> " + new Date().getTime() + " "
                + ZoneId.systemDefault() + "\n").getBytes());
        content.write(("committer " + "author name <author@email.com> " + new Date().getTime() + " "
                + ZoneId.systemDefault() + "\n").getBytes());
        content.write("\n".getBytes());
        content.write(message.getBytes());
        content.write("\n".getBytes());
        byte[] data = content.toByteArray();

        byte[] hashCodeBytes = MessageDigest.getInstance("SHA-1").digest(data);
        String fileHash = HexFormat.of().formatHex(hashCodeBytes);
        String dirName = new StringBuffer(fileHash).substring(0, 2);
        String fileName = new StringBuffer(fileHash).substring(2);

        new File("./.git/objects/", dirName).mkdir();
        File outputFile = new File("./.git/objects/" + dirName, fileName);
        outputFile.createNewFile();

        DeflaterOutputStream outputStream = new DeflaterOutputStream(new FileOutputStream(outputFile));
        outputStream.write(("commit " + data.length + "\0").getBytes());
        outputStream.write(data);

        outputStream.close();

        return hashCodeBytes;
    }
}