package me.jacob.entities;

import com.opencsv.bean.CsvBindByName;

public class ClassRecordOutput {

    @CsvBindByName(column = "id")
    private int id;

    @CsvBindByName(column = "method-source-file")
    private String methodSourceFile;

    @CsvBindByName(column = "signature")
    private String signature;

    @CsvBindByName(column = "graphId")
    private int graphId;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMethodSourceFile() {
        return methodSourceFile;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setMethodSourceFile(String methodSourceFile) {
        this.methodSourceFile = methodSourceFile;
    }

    public int getGraphId() {
        return graphId;
    }

    public void setGraphId(int graphId) {
        this.graphId = graphId;
    }
}
