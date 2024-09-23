package me.jacob;

import com.github.javaparser.ast.CompilationUnit;
import me.jacob.entities.SdpEdge;
import me.jacob.entities.SdpMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodListTransformer {

    private List<SdpMethod> methods = new ArrayList<>();

    private List<SdpEdge> edges = new ArrayList<>();

    private final CompilationUnit compilationUnit;

    public MethodListTransformer(List<SdpMethod> methods, CompilationUnit compilationUnit) {
        this.methods = methods;
        this.compilationUnit = compilationUnit;
        this.edges = new ArrayList<>();
    }

    public void transform(CompilationUnit cu) {
        if (!methods.isEmpty()) {
            var methodContextFetcher = new MethodContextFetcher(methods, cu);
            methodContextFetcher.calculate();

            List<SdpMethod> finalNodes = new ArrayList<>();
            List<SdpEdge> finalEdges = new ArrayList<>();
            Set<Integer> invalid = new HashSet<>();
            for (var node : methodContextFetcher.getNodes().values()) {
                if (node.getSignature().contains("test")) {
                    node.setValid(false);
                    invalid.add(node.getId());
                } else {
                    finalNodes.add(node);
                }
            }

            for (var edge : methodContextFetcher.getEdges()) {
                if (invalid.contains(edge.getSource().getId()) || invalid.contains(edge.getDestination().getId())) {
                    continue;
                }

                finalEdges.add(edge);
            }

            for (Set<SdpEdge> graphEdges : GraphGrouper.groupEdgesIntoGraphs(finalEdges)) {
                var graphId = IdGenerator.getGraphId();
                Set<SdpMethod> graphNodes = new HashSet<>();
                for (SdpEdge edge : graphEdges) {
                    graphNodes.add(finalNodes.stream().filter(x -> x.getId() == edge.getSource().getId()).findFirst().get());
                    graphNodes.add(finalNodes.stream().filter(x -> x.getId() == edge.getDestination().getId()).findFirst().get());
                }

                for (var node : graphNodes) {
                    node.setGraphId(graphId);
                }

                for (var edge : graphEdges) {
                    edge.setGraphId(graphId);
                }
            }

            this.edges = finalEdges;
            this.methods = finalNodes;
        }
    }

    public List<SdpMethod> getMethods() {
        return methods;
    }

    public List<SdpEdge> getEdges() {
        return edges;
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }
}
