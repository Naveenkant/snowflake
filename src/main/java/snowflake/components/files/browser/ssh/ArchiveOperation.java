package snowflake.components.files.browser.ssh;

import snowflake.common.ssh.SshClient;
import snowflake.utils.PathUtils;
import snowflake.utils.SshCommandUtils;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArchiveOperation {
    private Map<String, String> extractCommands;
    private Map<String, String> compressCommands;

    public ArchiveOperation() {
        extractCommands = new LinkedHashMap<>();
        extractCommands.put(".tar", "cat \"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tar.gz",
                "gunzip -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tgz",
                "gunzip -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tar.bz2",
                "bzip2 -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tbz2",
                "bzip2 -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tbz",
                "bzip2 -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tar.xz",
                "xz -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".txz",
                "xz -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".zip", "unzip -o \"%s\" -d \"%s\" ");

        compressCommands = new LinkedHashMap<>();
        compressCommands.put("tar", "tar cvf - %s|cat>\"%s\"");
        compressCommands.put("tar.gz", "tar cvf - %s|gzip>\"%s\"");
        compressCommands.put("tar.bz2", "tar cvf - %s|bzip2 -z>\"%s\"");
        compressCommands.put("tar.xz", "tar cvf - %s|xz -z>\"%s\"");
        compressCommands.put("zip", "zip -r - %s|cat>\"%s\"");
    }

    public boolean isSupportedArchive(String fileName) {
        for (String key : extractCommands.keySet()) {
            if (fileName.endsWith(key)) {
                return true;
            }
        }
        return false;
    }

    public String getExtractCommand(String fileName) {
        for (String key : extractCommands.keySet()) {
            if (fileName.endsWith(key)) {
                return extractCommands.get(key);
            }
        }
        return null;
    }

    public boolean extractArchive(SshClient client, String archivePath, String targetFolder, AtomicBoolean stopFlag) {
        String command = getExtractCommand(archivePath);
        if (command == null) {
            System.out.println("Unsupported file: " + archivePath);
            return false;
        }
        command = String.format(command, archivePath, targetFolder);
        System.out.println("Invoke command: " + command);
        StringBuilder output = new StringBuilder();
        boolean ret = SshCommandUtils.exec(client, command, stopFlag, output);
        System.out.println("output: " + output.toString());
        return ret;
    }

    public boolean createArchive(SshClient client, List<String> files, String targetFolder, AtomicBoolean stopFlag) {
        String text = files.size() > 1 ? PathUtils.getFileName(targetFolder) : files.get(0);
        JTextField txtFileName = new JTextField(text);
        JTextField txtTargetFolder = new JTextField(targetFolder);
        JComboBox<String> comboBox = new JComboBox<>(compressCommands.keySet().toArray(new String[0]));
        if (JOptionPane.showOptionDialog(null,
                new Object[]{"Archive name", txtFileName, "Target folder", txtTargetFolder, "Archive type", comboBox},
                "Create archive", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION) {

            StringBuilder sb = new StringBuilder();
            for (String s : files) {
                sb.append(" \"" + s + "\"");
            }

            String ext = comboBox.getSelectedItem() + "";

            String compressCmd = String.format(compressCommands.get(ext),
                    sb.toString(),
                    PathUtils.combineUnix(txtTargetFolder.getText(),
                            txtFileName.getText() + "." + ext));
            String cd = String.format("cd \"%s\";", txtTargetFolder.getText());
            System.out.println(cd + compressCmd);
            StringBuilder output = new StringBuilder();
            boolean ret = SshCommandUtils.exec(client, cd + compressCmd, stopFlag, output);
            System.out.println("output: " + output.toString());
            return ret;
        }
        return true;
    }
}
