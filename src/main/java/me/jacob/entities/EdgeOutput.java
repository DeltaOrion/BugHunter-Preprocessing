package me.jacob.entities;

import com.opencsv.bean.CsvBindByName;

import java.util.Objects;

public class EdgeOutput {

    @CsvBindByName(column = "source")
    private int source;

    @CsvBindByName(column = "destination")
    private int destination;

    @CsvBindByName(column = "graphId")
    private int graphId;

    public EdgeOutput(int source, int destination, int graphId) {
        this.source = source;
        this.destination = destination;
        this.graphId = graphId;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public int getGraphId() {
        return graphId;
    }

    public void setGraphId(int graphId) {
        this.graphId = graphId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgeOutput that = (EdgeOutput) o;
        return source == that.source && destination == that.destination;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination);
    }
}
