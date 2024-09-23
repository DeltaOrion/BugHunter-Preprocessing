package me.jacob;

import me.jacob.entities.SdpEdge;
import me.jacob.entities.SdpMethod;
import me.jacob.util.UnionFind;

import java.util.*;

public class GraphGrouper {
    public static List<Set<SdpEdge>> groupEdgesIntoGraphs(List<SdpEdge> edges) {
        // Collect all unique nodes
        Set<Integer> nodes = new HashSet<>();
        for (SdpEdge edge : edges) {
            nodes.add(edge.getSource().getId());
            nodes.add(edge.getDestination().getId());
        }

        // Initialize Union-Find structure
        UnionFind uf = new UnionFind(nodes);

        // Union connected nodes
        for (SdpEdge edge : edges) {
            uf.union(edge.getSource().getId(), edge.getDestination().getId());
        }

        // Group edges by connected component (root parent)
        Map<Integer, Set<SdpEdge>> componentToEdges = new HashMap<>();
        for (SdpEdge edge : edges) {
            int componentId = uf.find(edge.getSource().getId());
            componentToEdges
                    .computeIfAbsent(componentId, k -> new HashSet<>())
                    .add(edge);
        }

        // Return the grouped edges
        return new ArrayList<>(componentToEdges.values());
    }
}