package me.jacob.entities;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;

import java.util.Objects;

public class SdpMethod {

    private int id;
    private BodyDeclaration<?> source;
    private boolean isValid = true;
    private String project;
    private String classSourceFile;
    private String hash;
    private String signature;
    private String parent;
    private int numberOfBugs;

    public SdpMethod() {
    }

    public int getId() {
        return id;
    }

    public SdpMethod setId(int id) {
        this.id = id;
        return this;
    }

    public BodyDeclaration<?> getSource() {
        return source;
    }

    public void setSource(BodyDeclaration<?> source) {
        this.source = source;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
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

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SdpMethod that = (SdpMethod) o;
        return Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(source);
    }
}
