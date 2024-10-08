package me.jacob;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import me.jacob.entities.ClassRecordOutput;
import me.jacob.entities.EdgeOutput;
import me.jacob.entities.SdpEdge;
import me.jacob.entities.SdpMethod;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClassFileTransformer implements Runnable {

    private final Configuration configuration;

    private final List<ClassRecordOutput> nodes;

    private final List<EdgeOutput> edges;

    private String classSourceName;

    public ClassFileTransformer(Configuration configuration) {
        this.configuration = configuration;
        this.edges = new ArrayList<>();
        this.nodes = new ArrayList<>();
    }


    @Override
    public void run() {
        try {
            var parseResult = parse(new File(configuration.getFileName()));
            parseResult.ifSuccessful(this::transform);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ParseResult<CompilationUnit> parse(File sourceFile) throws FileNotFoundException {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        ParserConfiguration parserConfig = new ParserConfiguration().setSymbolResolver(symbolSolver);

        JavaParser parser = new JavaParser(parserConfig);
        return parser.parse(sourceFile);
    }

    private void transform(CompilationUnit cu) {
        try {
            List<SdpMethod> calcNodes = new ArrayList<>();
            for (var method : cu.findAll(MethodDeclaration.class)) {
                calcNodes.add(createSdpMethod(method, method.getSignature().asString()));
            }

            MethodListTransformer transformer = new MethodListTransformer(calcNodes, cu);
            transformer.transform(cu);
            for (var node : transformer.getMethods()) {
                this.nodes.add(convertToOutput(node));
                System.out.println("Processed " + node.getId() + ", " + node.getSignature());
            }

            for (var edge : transformer.getEdges()) {
                this.edges.add(convertToOutput(edge));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private EdgeOutput convertToOutput(SdpEdge edge) {
        return new EdgeOutput(edge.getSource().getId(), edge.getDestination().getId(), edge.getGraphId());
    }

    private ClassRecordOutput convertToOutput(SdpMethod node) throws IOException {
        var output = new ClassRecordOutput();
        var methodPath = Path.of("methods", node.getId() + ".java");
        writeNodeToFile(node.getSource(), Path.of(configuration.getOutputDirectory(), methodPath.toString()).toString());
        output.setId(node.getId());
        output.setMethodSourceFile(methodPath.toString());
        output.setSignature(node.getSignature());
        output.setGraphId(node.getGraphId());
        return output;
    }

    private SdpMethod createSdpMethod(BodyDeclaration<?> declaration, String signature) {
        var output = new SdpMethod();
        output.setId(IdGenerator.getNodeId());
        output.setSource(declaration);
        output.setSignature(signature);
        return output;
    }

    private void writeNodeToFile(Node node, String fileName) throws IOException {
        var file = new File(fileName);
        if (!file.exists()) {
            file.toPath().getParent().toFile().mkdirs();
            file.createNewFile();
        }

        try (var fileWriter = new FileWriter(file)) {
            try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
                writer.write(node.toString());
            }
        }
    }

    public List<ClassRecordOutput> getNodes() {
        return nodes;
    }

    public List<EdgeOutput> getEdges() {
        return edges;
    }
}
