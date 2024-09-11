package me.jacob;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Stack;

public class NameMatcher {

    private final CompilationUnit cu;
    private final String longName;
    private String signature;
    private CallableDeclaration<?> result;

    public NameMatcher(CompilationUnit cu, String longName) {
        this.cu = cu;
        this.longName = longName;
    }

    public void calculateMatchingNode() throws Exception {
        var packageDeclaration = cu.getPackageDeclaration();
        var packageName = "";
        if (packageDeclaration.isPresent()) {
            packageName = packageName + packageDeclaration.get().getNameAsString();
        }

        if (!longName.startsWith(packageName)) {
            throw new Exception();
        }

        var startName = longName.substring(packageName.length());
        var visitor = new NameConstructorVisitor();
        cu.accept(visitor, startName);

        signature = visitor.getSignature();
        result = visitor.getResult();
    }

    public CallableDeclaration<?> getResult() {
        return result;
    }

    public String getSignature() {
        return signature;
    }

    public String getLongName() {
        return longName;
    }

    public CompilationUnit getCu() {
        return cu;
    }

    private static class NameConstructorVisitor extends VoidVisitorAdapter<String> {

        private CallableDeclaration<?> result;
        private Stack<Integer> anonymousClassStack = new Stack<>();
        private String signature;

        @Override
        public void visit(final ObjectCreationExpr objectCreationExpr, String name) {
            objectCreationExpr.getAnonymousClassBody().ifPresentOrElse(body -> {
                // Check if the name matches this level of nesting
                var anonymousClassNumber = anonymousClassStack.peek();
                String expectedPrefix = "$" + anonymousClassNumber;
                if (name.startsWith(expectedPrefix)) {
                    anonymousClassStack.push(1);
                    super.visit(objectCreationExpr, name.substring(expectedPrefix.length()));
                    anonymousClassStack.pop();
                }

                anonymousClassStack.push(anonymousClassStack.pop() + 1);
            }, () -> {
                super.visit(objectCreationExpr, name);
            });
        }

        @Override
        public void visit(final ClassOrInterfaceDeclaration classDeclaration, String name) {
            anonymousClassStack.push(1);
            var length = classDeclaration.getNameAsString().length() + 1;
            if (classDeclaration.isNestedType() && name.startsWith("$" + classDeclaration.getNameAsString())) {
                super.visit(classDeclaration, name.substring(length));
            } else if (name.startsWith("." + classDeclaration.getNameAsString())) {
                super.visit(classDeclaration, name.substring(length));
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
            signature = root.getSignature().asString();
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
            signature = constructorDeclaration.getSignature().asString();
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

        public CallableDeclaration<?> getResult() {
            return result;
        }

        public String getSignature() {
            return signature;
        }
    }
}
