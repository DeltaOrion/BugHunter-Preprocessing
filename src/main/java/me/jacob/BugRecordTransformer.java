package me.jacob;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import me.jacob.entities.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BugRecordTransformer implements Runnable {

    //all record inputs should have the same project, file and hash
    private final Collection<BugRecordInput> inputs;

    private final Configuration configuration;

    private final List<BugRecordOutput> nodes;

    private final List<EdgeOutput> edges;

    private String classSourceName;

    private BugRecordInput firstInput;

    public BugRecordTransformer(Collection<BugRecordInput> inputs, Configuration configuration) {
        this.inputs = inputs;
        this.configuration = configuration;
        this.edges = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.firstInput = inputs.stream().findFirst().get();
    }


    @Override
    public void run() {
        try {
            var sourceFile = createSourceFile();
            var parseResult = parse(sourceFile);
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

    private File createSourceFile() throws IOException {
        var originalSourceFile = new File(configuration.getInputDirectory(), firstInput.getSourceFile());
        var id = UUID.randomUUID();
        var newSourceFile = Path.of(configuration.getOutputDirectory(), firstInput.getProject(), "classes", id + ".java").toFile();
        this.classSourceName = Path.of(firstInput.getProject(), "classes", id + ".java").toString();
        copySourceFile(originalSourceFile, newSourceFile);
        return newSourceFile;
    }

    private void copySourceFile(File originalSourceFile, File newSourceFile) throws IOException {
        if (!newSourceFile.exists()) {
            newSourceFile.toPath().getParent().toFile().mkdirs();
        }

        Files.copy(originalSourceFile.toPath(), newSourceFile.toPath());
    }

    private void transform(CompilationUnit cu) {
        try {
            List<SdpMethod> calcNodes = new ArrayList<>();
            for (var input : inputs) {
                var nameMatcher = new NameMatcher(cu, input.getLongName());
                nameMatcher.calculateMatchingNode();
                var result = nameMatcher.getResult();
                if (result == null) {
                    System.out.println("Unable to match " + input.getLongName());
                    continue;
                }
                calcNodes.add(createSdpMethod(input, nameMatcher));
            }

            var transformer = new MethodListTransformer(calcNodes, cu);
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

    private BugRecordOutput convertToOutput(SdpMethod node) throws IOException {
        var output = new BugRecordOutput();
        var methodPath = Path.of(node.getProject(), "methods", node.getId() + ".java");
        writeNodeToFile(node.getSource(), Path.of(configuration.getOutputDirectory(), methodPath.toString()).toString());
        output.setId(node.getId());
        output.setHash(node.getHash());
        output.setParent(node.getParent());
        output.setProject(node.getProject());
        output.setMethodSourceFile(methodPath.toString());
        output.setClassSourceFile(node.getClassSourceFile());
        output.setId(node.getId());
        output.setNumberOfBugs(node.getNumberOfBugs());
        output.setSignature(node.getSignature());
        output.setGraphId(node.getGraphId());
        output.setOldLongName(node.getOldLongName());
        return output;
    }

    private SdpMethod createSdpMethod(BugRecordInput input, NameMatcher nameMatcher) {
        var output = new SdpMethod();
        output.setId(IdGenerator.getNodeId());
        output.setProject(input.getProject());
        output.setHash(input.getHash());
        output.setParent(input.getParent());
        output.setNumberOfBugs(input.getNumberOfBugs());
        output.setOldLongName(input.getLongName());
        output.setSource(nameMatcher.getResult());
        output.setClassSourceFile(classSourceName);
        output.setSignature(input.getParent() + "." + nameMatcher.getSignature());
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

    public Collection<BugRecordOutput> getNodes() {
        return nodes;
    }

    public Collection<EdgeOutput> getEdges() {
        return edges;
    }
}
