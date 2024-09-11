package me.jacob.entities;

import java.util.Objects;

public class SdpEdge {

    private final SdpMethod source;
    private final SdpMethod destination;

    public SdpEdge(SdpMethod source, SdpMethod destination) {
        this.source = source;
        this.destination = destination;
    }

    public SdpMethod getSource() {
        return source;
    }

    public SdpMethod getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SdpEdge sdpEdge = (SdpEdge) o;
        return Objects.equals(source, sdpEdge.source) && Objects.equals(destination, sdpEdge.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination);
    }
}
