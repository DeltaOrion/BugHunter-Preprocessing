package me.jacob.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UnionFind {
    private Map<Integer, Integer> parent;

    public UnionFind(Set<Integer> nodes) {
        parent = new HashMap<>();
        for (int node : nodes) {
            parent.put(node, node);
        }
    }

    public int find(int x) {
        if (parent.get(x) != x) {
            parent.put(x, find(parent.get(x))); // Path compression
        }
        return parent.get(x);
    }

    public void union(int x, int y) {
        int px = find(x);
        int py = find(y);
        if (px != py) {
            parent.put(px, py);
        }
    }
}
