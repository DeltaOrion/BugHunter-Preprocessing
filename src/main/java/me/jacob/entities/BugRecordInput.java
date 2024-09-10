package me.jacob.entities;

import com.opencsv.bean.CsvBindByName;

public class BugRecordInput {
    @CsvBindByName(column = "Project")
    private String project;
    @CsvBindByName(column = "Source-file")
    private String sourceFile;
    @CsvBindByName(column = "Hash")
    private String hash;
    @CsvBindByName(column = "Long-Name")
    private String longName;
    @CsvBindByName(column = "Parent")
    private String parent;
    @CsvBindByName(column = "Number-Of-Bugs")
    private int numberOfBugs;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public int getNumberOfBugs() {
        return numberOfBugs;
    }

    public void setNumberOfBugs(int numberOfBugs) {
        this.numberOfBugs = numberOfBugs;
    }
}
