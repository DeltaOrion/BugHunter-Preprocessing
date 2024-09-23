package me.jacob;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;

import java.util.Optional;
import java.util.Stack;

public class NameMatcher {

    private final CompilationUnit cu;
    private final String longName;
    private String signature;
    private BodyDeclaration<?> result;

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

    public BodyDeclaration<?> getResult() {
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

        private BodyDeclaration<?> result;
        private Stack<Integer> anonymousClassStack = new Stack<>();
        private String signature;

        @Override
        public void visit(final EnumConstantDeclaration enumConstantDec, String name) {
            if (enumConstantDec.getClassBody().size() > 0) {
                handleAnonymousClassBody(name, enumConstantDec.getClassBody());
            } else {
                super.visit(enumConstantDec, name);
            }
        }

        @Override
        public void visit(final ObjectCreationExpr objectCreationExpr, String name) {
            objectCreationExpr.getAnonymousClassBody().ifPresentOrElse(body -> {
                handleAnonymousClassBody(name, body);
            }, () -> {
                super.visit(objectCreationExpr, name);
            });
        }

        private void handleAnonymousClassBody(String name, NodeList<BodyDeclaration<?>> body) {
            var anonymousClassNumber = anonymousClassStack.peek();
            String expectedPrefix = "$" + anonymousClassNumber;

            // Check if the name matches this level of nesting
            if (name.startsWith(expectedPrefix)) {
                anonymousClassStack.push(1);
                super.visit(body, name.substring(expectedPrefix.length()));
                anonymousClassStack.pop();
            }

            // Increment the stack to track the next anonymous class
            anonymousClassStack.push(anonymousClassStack.pop() + 1);
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

            result = findRoot(methodDeclaration);
            signature = calcSignature();
        }

        private String calcSignature() {
            if (result instanceof CallableDeclaration<?> callable) {
                return callable.getSignature().asString();
            } else if (result instanceof EnumConstantDeclaration c) {
                return c.getNameAsString();
            }

            return "unknown";
        }

        public BodyDeclaration<?> findRoot(BodyDeclaration<?> node) {
            BodyDeclaration<?> current = node;

            // Traverse upwards through method, constructor, and enum constant declarations
            while (true) {
                Optional<MethodDeclaration> methodAncestor = current.findAncestor(MethodDeclaration.class);
                Optional<ConstructorDeclaration> constructorAncestor = current.findAncestor(ConstructorDeclaration.class);
                Optional<EnumConstantDeclaration> enumConstantAncestor = current.findAncestor(EnumConstantDeclaration.class);

                // Choose the first present ancestor (if any)
                if (methodAncestor.isPresent()) {
                    current = methodAncestor.get();
                } else if (constructorAncestor.isPresent()) {
                    current = constructorAncestor.get();
                } else if (enumConstantAncestor.isPresent()) {
                    current = enumConstantAncestor.get();
                } else {
                    break; // No more ancestors found, so we've reached the root
                }
            }

            return current;
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

            result = findRoot(constructorDeclaration);
            signature = calcSignature();
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
            if (paramName.isArrayType() && paramNameStr.endsWith("[]")) {
                while (paramNameStr.endsWith("[]")) {
                    paramNameStr = paramNameStr.substring(0, paramNameStr.length() - 2);
                }
            }
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
                var resolved = paramName.resolve();

                // If the type is a type parameter, check its bound
                if (resolved.isTypeVariable()) {
                    var typeVariable = resolved.asTypeParameter();
                    if (typeVariable.declaredOnType() || typeVariable.declaredOnConstructor() || typeVariable.declaredOnMethod()) {
                        // Compare the upper bound of the type parameter, default to Object if none
                        var upperBound = getErasureBound(typeVariable);
                        return upperBound.equals(parsedName) || "Object".equals(parsedName);
                    }
                }
            } catch (Exception ignored) {
                // Handle unresolved types
            }


            return false;
        }

        private String getErasureBound(ResolvedTypeParameterDeclaration typeVariable) {
            var astOpt = typeVariable.toAst();
            if (!astOpt.isPresent()) {
                return "Object";
            }

            var ast = astOpt.get();
            if (!(ast instanceof TypeParameter typeParameter)) {
                return "Object";
            }

            if (typeParameter.getTypeBound().isEmpty()) {
                return "Object";
            }

            return typeParameter.getTypeBound().get(0).getNameAsString();
        }

        public BodyDeclaration<?> getResult() {
            return result;
        }

        public String getSignature() {
            return signature;
        }
    }
}
