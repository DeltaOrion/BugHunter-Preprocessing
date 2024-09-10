package me.jacob;

import java.io.File;
import java.nio.file.Path;

public class Configuration {

    private String fileName;

    private String workingDirectory;

    private String outputFile;

    public String getFileName() {
        return Path.of(workingDirectory,fileName).toFile().getPath();
    }

    public Configuration setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Configuration setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public String getOutputFile() {
        return Path.of(workingDirectory,outputFile).toFile().getPath();
    }

    public Configuration setOutputFile(String outputFile) {
        this.outputFile = outputFile;
        return this;
    }
}
