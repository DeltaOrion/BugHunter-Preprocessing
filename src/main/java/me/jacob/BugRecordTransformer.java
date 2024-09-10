package me.jacob;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import me.jacob.entities.BugRecordInput;
import me.jacob.entities.FileBugRecordOutput;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

public class BugRecordTransformer implements Runnable {

    //all record inputs should have the same project, file and hash
    private final Collection<BugRecordInput> inputs;

    private final Configuration configuration;

    private FileBugRecordOutput output;

    public BugRecordTransformer(Collection<BugRecordInput> inputs, Configuration configuration) {
        this.inputs = inputs;
        this.configuration = configuration;
    }


    @Override
    public void run() {
        File file = new File(configuration.getWorkingDirectory(), inputs.stream().findFirst().get().getSourceFile());
        try {
            ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();

            CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            combinedTypeSolver.add(reflectionTypeSolver);

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
            ParserConfiguration parserConfig = new ParserConfiguration().setSymbolResolver(symbolSolver);

            JavaParser parser = new JavaParser(parserConfig);
            var result = parser.parse(file);
            result.ifSuccessful(this::transform);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void transform(CompilationUnit cu) {
        for (var input : inputs) {
            var nameConstructor = new NameDestructor(cu, input.getLongName());
            try {
                var matchingNode = nameConstructor.getMatchingNode();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
