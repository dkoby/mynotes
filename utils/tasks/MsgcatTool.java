/*
 *
 */
package tasks;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import java.util.ArrayList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 *
 */
public class MsgcatTool extends Task {
    private String dstDir;
    private String srcDir;
    /**
     *
     */
    @Override
    public void execute() {
        if (dstDir == null)
            throw new BuildException("No \"dstdir\" attribute specified");
        if (srcDir == null)
            srcDir = "msgcat";

        FileSystem fileSystem = FileSystems.getDefault();
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(fileSystem.getPath(srcDir), "*.properties")) {
            for (Path entry: stream) {
                Path srcFileName = entry.getFileName();
                Path dstFileName = fileSystem.getPath(dstDir, srcFileName.toString());

                /* convert only updated files */
                if (dstFileName.toFile().exists()
                        && (entry.toFile().lastModified() <= dstFileName.toFile().lastModified()))
                    continue;

                log("convert \"" + srcFileName.toString() + "\"");
                try {
                    convert(entry, dstFileName);
                } catch (BuildException e) {
                    throw new BuildException("conversion failed, " + entry + ", " + dstFileName + ": "+ e);
                }
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
    /**
     *
     */
    public void setDstdir(String dstDir) {
        this.dstDir = dstDir;
    }
    /**
     *
     */
    public void setSrcdir(String srcDir) {
        this.srcDir = srcDir;
    }
    /*
     *
     */
    private void convert(Path src, Path dst) throws BuildException {
        Process process;

        try {
            ArrayList<String> args = new ArrayList<>();

            args.add("native2ascii");
            args.add("-encoding");
            args.add("UTF-8");
            args.add(src.toString());
            args.add(dst.toString());

            process = Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
        } catch (IOException e) {
            throw new BuildException(e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                log("> " + line);
            }
        } catch (Exception e) { }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new BuildException("interrupted" + e);
        }
        if (process.exitValue() != 0) {
            throw new BuildException("conversion failed");
        }
    }
}

