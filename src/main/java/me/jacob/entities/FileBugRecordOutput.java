package me.jacob.entities;

import com.opencsv.bean.CsvBindByName;

public class FileBugRecordOutput {
    @CsvBindByName(column = "id")
    private String Id;
    @CsvBindByName(column = "project")
    private String project;
    @CsvBindByName(column = "class-source-file")
    private String classSourceFile;
    @CsvBindByName(column = "method-source-file")
    private String methodSourceFile;
    @CsvBindByName(column = "hash")
    private String hash;
    @CsvBindByName(column = "long-name")
    private String longName;
    @CsvBindByName(column = "parent")
    private String parent;
    @CsvBindByName(column = "number-of-bugs")
    private int numberOfBugs;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getClassSourceFile() {
        return classSourceFile;
    }

    public void setClassSourceFile(String classSourceFile) {
        this.classSourceFile = classSourceFile;
    }

    public String getMethodSourceFile() {
        return methodSourceFile;
    }

    public void setMethodSourceFile(String methodSourceFile) {
        this.methodSourceFile = methodSourceFile;
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
