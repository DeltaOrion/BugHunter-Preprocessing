package me.jacob;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Stack;

public class NameDestructor {

    private final CompilationUnit cu;
    private final String longName;

    public NameDestructor(CompilationUnit cu, String longName) {
        this.cu = cu;
        this.longName = longName;
    }

    public Node getMatchingNode() throws Exception {
        var packageDeclaration = cu.getPackageDeclaration();
        var packageName = "";
        if (packageDeclaration.isPresent()) {
            packageName = packageName + packageDeclaration.get().getNameAsString();
        }

        if (!longName.startsWith(packageName)) {
            throw new Exception();
        }

        var startName = longName.substring(packageName.length());
        var visitor = new NameConstructorVisitor(longName);
        cu.accept(visitor, startName);
        if (visitor.getResult() == null) {
            System.out.println("Unable to find method for '" + longName + "'");
        } else if (visitor.getResult() instanceof MethodDeclaration dec) {
            System.out.println(longName + "            " + dec.getSignature().asString());
        } else if (visitor.getResult() instanceof ConstructorDeclaration dec) {
            System.out.println(longName + "            " + dec.getSignature().asString());
        }

        return visitor.getResult();
    }

    private static class NameConstructorVisitor extends VoidVisitorAdapter<String> {

        private Node result;
        private Stack<Integer> anonymousClassStack = new Stack<>();
        private final String originalName;

        private NameConstructorVisitor(String originalName) {
            this.originalName = originalName;
        }

        @Override
        public void visit(final ObjectCreationExpr objectCreationExpr, String name) {
            objectCreationExpr.getAnonymousClassBody().ifPresent(body -> {
                // Check if the name matches this level of nesting
                var anonymousClassNumber = anonymousClassStack.peek();
                String expectedPrefix = "$" + anonymousClassNumber;
                if (name.startsWith(expectedPrefix)) {
                    anonymousClassStack.push(1);
                    super.visit(objectCreationExpr, name.substring(expectedPrefix.length()));
                    anonymousClassStack.pop();
                }

                anonymousClassStack.push(anonymousClassStack.pop() + 1);
            });

            super.visit(objectCreationExpr, name);
        }

        @Override
        public void visit(final ClassOrInterfaceDeclaration classDeclaration, String name) {
            anonymousClassStack.push(1);
            var length = classDeclaration.getNameAsString().length() + 1;
            if (classDeclaration.isNestedType() && name.startsWith("$" + classDeclaration.getNameAsString())) {
                super.visit(classDeclaration, name.substring(length));
            } else if (name.startsWith("." + classDeclaration.getNameAsString())) {
                super.visit(classDeclaration, name.substring(length));
            } else {
                //we are looking for an anonymous class now
                super.visit(classDeclaration, name);
            }
            anonymousClassStack.pop();
        }

        @Override
        public void visit(final EnumDeclaration enumDeclaration, String name) {
            anonymousClassStack.push(1);
            var length = enumDeclaration.getNameAsString().length() + 1;
            if (enumDeclaration.isNestedType() && name.startsWith("$" + enumDeclaration.getNameAsString())) {
                super.visit(enumDeclaration, name.substring(length));
            } else if (name.startsWith("." + enumDeclaration.getNameAsString())) {
                super.visit(enumDeclaration, name.substring(length));
            } else {
                //we are looking for an anonymous class now
                super.visit(enumDeclaration, name);
            }
            anonymousClassStack.pop();
        }

        @Override
        public void visit(final MethodDeclaration methodDeclaration, String name) {
            if (!name.startsWith("." + methodDeclaration.getNameAsString())) {
                super.visit(methodDeclaration, name);
                return;
            }

            if (!parameterMatches(methodDeclaration.getParameters(), name)) {
                super.visit(methodDeclaration, name);
                return;
            }

            var root = methodDeclaration;
            while (root.findAncestor(MethodDeclaration.class).isPresent()) {
                root = root.findAncestor(MethodDeclaration.class).get();
            }
            result = root;
            super.visit(methodDeclaration, name);
        }


        @Override
        public void visit(final ConstructorDeclaration constructorDeclaration, String name) {
            if (!name.startsWith(".<init>")) {
                super.visit(constructorDeclaration, name);
                return;
            }

            if (!parameterMatches(constructorDeclaration.getParameters(), name)) {
                super.visit(constructorDeclaration, name);
                return;
            }

            result = constructorDeclaration;
            super.visit(constructorDeclaration, name);
        }

        private boolean parameterMatches(NodeList<Parameter> parameters, String fullSig) {
            int startIndex = fullSig.indexOf('(');
            int endIndex = fullSig.indexOf(')');
            String paramSection = fullSig.substring(startIndex + 1, endIndex);

            // Split the parameter section into individual JVM types
            var parsed = new JvmParameterParser().parseParameters(paramSection);
            for (int i = 0; i < parameters.size(); i++) {
                if (i >= parsed.size()) {
                    return false;
                }

                var paramType = parameters.get(i).getType();
                var param = parameters.get(i);
                var parsedType = parsed.get(i);
                if (!namesEqual(parsedType.getTypeName(), paramType)) {
                    return false;
                }

                if (parsedType.isArray() != (paramType.isArrayType() || param.isVarArgs())) {
                    return false;
                }

                if (!parsedType.isArray() && paramType.getArrayLevel() != (param.isVarArgs() ? 1 : parsedType.getDimensionality())) {
                    return false;
                }
            }

            return true;
        }

        private boolean namesEqual(String parsedName, Type paramName) {
            var paramNameStr = paramName.asString();
            if (paramName.isClassOrInterfaceType() && paramName.asClassOrInterfaceType().getTypeArguments().isPresent()) {
                paramNameStr = paramName.asClassOrInterfaceType().getName().asString();
            }
            if (paramNameStr.equals(parsedName)) {
                return true;
            }

            if (parsedName.contains(".") && paramNameStr.equals(parsedName.substring(parsedName.indexOf(".") + 1))) {
                return true;
            }

            //if is the paramName is generic, and the parsedName is an object, then it is a match, return true
            try {
                var resolved = paramName.resolve().asTypeParameter();
                if (resolved.declaredOnType() || resolved.declaredOnConstructor() || resolved.declaredOnMethod()) {
                    return "Object".equals(parsedName);
                }
            } catch (Exception ignored) {

            }

            return false;
        }

        public Node getResult() {
            return result;
        }
    }
}
