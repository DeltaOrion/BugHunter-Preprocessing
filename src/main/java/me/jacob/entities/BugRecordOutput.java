package me.jacob.entities;

import com.opencsv.bean.CsvBindByName;

public class BugRecordOutput {
    @CsvBindByName(column = "id")
    private int id;
    @CsvBindByName(column = "project")
    private String project;
    @CsvBindByName(column = "class-source-file")
    private String classSourceFile;
    @CsvBindByName(column = "method-source-file")
    private String methodSourceFile;
    @CsvBindByName(column = "hash")
    private String hash;
    @CsvBindByName(column = "signature")
    private String signature;
    @CsvBindByName(column = "parent")
    private String parent;
    @CsvBindByName(column = "number-of-bugs")
    private int numberOfBugs;
    @CsvBindByName(column = "graphId")
    private int graphId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getGraphId() {
        return graphId;
    }

    public void setGraphId(int graphId) {
        this.graphId = graphId;
    }
}
