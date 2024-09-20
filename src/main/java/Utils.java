import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
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
        byte[] treeHeader = ("tree " + treeItems.length + "\0").getBytes();


        ByteBuffer combined = ByteBuffer.allocate(treeItems.length + treeHeader.length);
        combined.put(treeHeader);
        combined.put(treeItems);

        byte[] treeContent = combined.array();
        byte[] treeSHA = toBinarySHA(treeContent);
        String treePath = shaToPath(HexFormat.of().formatHex(treeSHA));

        File blobFile = new File(treePath);
        blobFile.getParentFile().mkdirs();

        DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(blobFile));
        out.write(treeContent);

        out.close();
        buffer.close();

        return treeSHA;
    }

    private static String shaToPath(String formatHex) {

        String path = String.format("./.git/objects/%s/%s", formatHex.substring(0, 2), formatHex.substring(2));
        return path;
    }

    private static byte[] toBinarySHA(byte[] treeContent) throws Exception {
        byte[] message = MessageDigest.getInstance("SHA-1").digest(treeContent);
        return message;
    }

    // private static String writeRawObject(byte[] data) throws Exception {
    //     String dirHash = "";

    //     // calculate the dirHash
    //     MessageDigest message = MessageDigest.getInstance("SHA-1");
    //     message.update("tree".getBytes());
    //     message.update(" ".getBytes());
    //     message.update(String.valueOf(data.length).getBytes());
    //     message.update("\0".getBytes());
    //     message.update(data);

    //     byte[] digest = message.digest();

    //     dirHash = HexFormat.of().formatHex(digest);
    //     String dirName = new StringBuffer(dirHash).substring(0, 2);
    //     String fileName = new StringBuffer(dirHash).substring(2);

    //     File fileCreated = new File("./.git/objects", dirName);
    //     fileCreated.mkdir();

    //     File finalFile = new File("./.git/objects/" + dirName, fileName);

    //     finalFile.createNewFile();

    //     DeflaterOutputStream writeFile = new DeflaterOutputStream(new FileOutputStream(finalFile));

    //     writeFile.write("tree".getBytes());
    //     writeFile.write(" ".getBytes());
    //     writeFile.write(String.valueOf(data.length).getBytes());
    //     writeFile.write("\0".getBytes());
    //     writeFile.write(digest);

    //     writeFile.close();
    //     return dirHash;
    // }
}