package me.jacob;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import me.jacob.entities.SdpEdge;
import me.jacob.entities.SdpMethod;

import java.util.*;
import java.util.stream.Collectors;

public class MethodContextFetcher {

    private final List<SdpEdge> edges;
    private List<SdpMethod> inputMethods;
    private final Map<BodyDeclaration<?>, SdpMethod> nodes;
    private final Set<BodyDeclaration<?>> processed;
    private final SdpMethod firstMethod;
    private Map<String, List<BodyDeclaration<?>>> classDeclarations;
    private final CompilationUnit cu;

    public MethodContextFetcher(List<SdpMethod> inputMethods, CompilationUnit cu) {
        this.cu = cu;
        this.edges = new ArrayList<>();
        this.inputMethods = inputMethods;
        this.nodes = inputMethods.stream().collect(Collectors.toMap(SdpMethod::getSource, x -> x, this::merge));
        this.processed = new HashSet<>();
        this.firstMethod = inputMethods.stream().findFirst().get();
        this.classDeclarations = new HashMap<>();
    }

    private SdpMethod merge(SdpMethod a, SdpMethod b) {
        var merged = new SdpMethod();
        merged.setId(a.getId());
        merged.setProject(a.getProject());
        merged.setHash(a.getHash());
        merged.setParent(a.getParent());
        merged.setNumberOfBugs(a.getNumberOfBugs() + b.getNumberOfBugs());
        merged.setSource(a.getSource());
        merged.setClassSourceFile(a.getClassSourceFile());
        merged.setSignature(a.getSignature());
        return merged;
    }

    public void calculate() {
        for (var method : cu.findAll(MethodDeclaration.class)) {
            var list = classDeclarations.computeIfAbsent(method.getNameAsString(), x -> new ArrayList<>());
            list.add(method);
        }

        for (var constructor : cu.findAll(ConstructorDeclaration.class)) {
            var list = classDeclarations.computeIfAbsent(constructor.getNameAsString(), x -> new ArrayList<>());
            list.add(constructor);
        }

        for (var method : inputMethods) {
            dfs(method);
        }
    }

    private void dfs(SdpMethod method) {
        if (processed.contains(method.getSource())) {
            return;
        }

        processed.add(method.getSource());

        var visitor = new MethodDeclarationVisitor(classDeclarations);
        method.getSource().accept(visitor, null);
        if (visitor.isExplicitSuper) {
            method.setValid(false);
            return;
        }

        for (var connection : visitor.connected) {
            var connectionNode = nodes.get(connection);
            if (connectionNode == null) {
                connectionNode = createBuggedMethod(connection);
            }

            nodes.put(connectionNode.getSource(), connectionNode);
            edges.add(new SdpEdge(method, connectionNode));
            dfs(connectionNode);
        }
    }

    private SdpMethod createBuggedMethod(BodyDeclaration<?> connection) {
        var connectionNode = new SdpMethod();
        connectionNode.setId(IdGenerator.getNodeId());
        connectionNode.setProject(firstMethod.getProject());
        connectionNode.setSource(connection);
        connectionNode.setHash(firstMethod.getHash());
        connectionNode.setClassSourceFile(firstMethod.getClassSourceFile());
        connectionNode.setNumberOfBugs(-1);
        connectionNode.setSignature(calcSignature(connection));
        connectionNode.setParent(firstMethod.getParent());

        return connectionNode;
    }

    private String calcSignature(BodyDeclaration<?> result) {
        if (result instanceof CallableDeclaration<?> callable) {
            return callable.getSignature().asString();
        } else if (result instanceof EnumConstantDeclaration c) {
            return c.getNameAsString();
        }

        return "unknown";
    }

    public Map<BodyDeclaration<?>, SdpMethod> getNodes() {
        return nodes;
    }

    public List<SdpEdge> getEdges() {
        return edges;
    }

    private static class MethodDeclarationVisitor extends VoidVisitorAdapter<Void> {

        private final Set<BodyDeclaration<?>> connected = new HashSet<>();
        private boolean isExplicitSuper = false;
        private final Map<String, List<BodyDeclaration<?>>> classDecs;

        private MethodDeclarationVisitor(Map<String, List<BodyDeclaration<?>>> classDecs) {
            this.classDecs = classDecs;
        }


        @Override
        public void visit(final MethodCallExpr methodCall, final Void arg) {
            try {
                var resolved = methodCall.resolve();
                connected.add((MethodDeclaration) resolved.toAst().get());
            } catch (Exception ex) {
                //unfortunately we cant resolve it because of javaparser being overly strict, lets instead look for the same method name
                if (isSameClassScope(methodCall)) {
                    var name = methodCall.getNameAsString();
                    if (classDecs.containsKey(name)) {
                        connected.addAll(classDecs.get(name));
                    }
                }
            }

            super.visit(methodCall, arg);
        }

        private boolean isSameClassScope(MethodCallExpr methodCall) {
            var scopeOpt = methodCall.getScope();
            if (!scopeOpt.isPresent()) {
                return true;
            }

            var scope = scopeOpt.get();
            var zuper = scope.findAll(SuperExpr.class);
            var nameExpr = scope.findAll(NameExpr.class);
            var fieldExpr = scope.findAll(FieldAccessExpr.class);
            var constructorExpr = scope.findAll(ObjectCreationExpr.class);
            var literal = scope.findAll(StringLiteralExpr.class);

            return nameExpr.isEmpty() && fieldExpr.isEmpty() && zuper.isEmpty() && constructorExpr.isEmpty() && literal.isEmpty();
        }


        @Override
        public void visit(final MethodReferenceExpr methodReferenceExpr, final Void arg) {
            try {
                var resolved = methodReferenceExpr.resolve();
                connected.add((MethodDeclaration) resolved.toAst().get());
            } catch (Exception ignored) {

            }

            super.visit(methodReferenceExpr, arg);
        }

        @Override
        public void visit(final ObjectCreationExpr constructorCall, final Void arg) {
            var name = constructorCall.getType().getNameAsString();
            if (classDecs.containsKey(name)) {
                connected.addAll(classDecs.get(name));
            }
        }

        @Override
        public void visit(final ExplicitConstructorInvocationStmt constructorCall, final Void arg) {
            if (constructorCall.isThis()) {
                // Handle 'this' constructor call
                var thisClass = constructorCall.findAncestor(ClassOrInterfaceDeclaration.class);
                if (thisClass.isPresent()) {
                    var name = thisClass.get().getNameAsString();
                    if (classDecs.containsKey(name)) {
                        connected.addAll(classDecs.get(name));
                    }
                }
            } else {
                // Check if 'super()' is the last significant statement in the constructor
                var constructor = constructorCall.findAncestor(ConstructorDeclaration.class);
                if (constructor.isPresent()) {
                    var constructorBody = constructor.get().getBody();
                    var statements = constructorBody.getStatements();

                    // Find the index of the 'super()' call in the list of statements
                    int superIndex = statements.indexOf(constructorCall);

                    // Check if 'super()' is the last statement (no other statements after it)
                    if (superIndex != -1 && superIndex == statements.size() - 1) {
                        // 'super()' is the last statement
                        isExplicitSuper = true;
                    } else {
                        // There are statements after 'super()', mark this as invalid for prediction
                        isExplicitSuper = false;
                    }
                }
            }
        }
    }
}
