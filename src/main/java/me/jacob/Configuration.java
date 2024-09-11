package me.jacob;

import java.nio.file.Path;

public class Configuration {

    private String fileName;

    private String inputDirectory;

    private String outputDirectory;

    public String getFileName() {
        return Path.of(inputDirectory,fileName).toFile().getPath();
    }

    public Configuration setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getInputDirectory() {
        return inputDirectory;
    }

    public Configuration setInputDirectory(String inputDirectory) {
        this.inputDirectory = inputDirectory;
        return this;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public Configuration setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }
}
