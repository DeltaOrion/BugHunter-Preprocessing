package me.jacob;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import me.jacob.entities.BugRecordInput;
import me.jacob.entities.BugRecordOutput;
import me.jacob.entities.EdgeOutput;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.run(args);
    }

    public void run(String[] args) {
        var configuration = getConfiguration(args);
        if(configuration.getFileName().endsWith(".java")) {
            preparePredictionData(configuration);
        } else {
            prepareTrainingData(configuration);
        }
    }

    private void prepareTrainingData(Configuration configuration) {
        var records = getRecords(configuration);
        List<BugRecordOutput> nodes = Collections.synchronizedList(new ArrayList<>());
        List<EdgeOutput> edges = Collections.synchronizedList(new ArrayList<>());

        Map<String, List<BugRecordInput>> fileMap = groupSameFile(records);
        var executor = Executors.newFixedThreadPool(10); // Control how many concurrent tasks you want
        var futures = new ArrayList<CompletableFuture<Void>>();

        try {
            for (var recordCollection : fileMap.values()) {
                futures.add(
                        CompletableFuture.runAsync(() -> {
                            try {
                                var transformer = new BugRecordTransformer(recordCollection, configuration);
                                transformer.run();
                                nodes.addAll(transformer.getNodes());
                                edges.addAll(transformer.getEdges());
                            } catch (Exception | Error ex) {
                                ex.printStackTrace();
                            }
                        }, executor)
                );
            }

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            // Ensure proper shutdown of the executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Force shutdown if tasks don't terminate in time
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        writeCsv(configuration, nodes, "nodes.csv");
        writeCsv(configuration, edges, "edges.csv");
    }

    private void preparePredictionData(Configuration configuration) {
        ClassFileTransformer transformer = new ClassFileTransformer(configuration);
        transformer.run();
        writeCsv(configuration, transformer.getNodes(), "nodes.csv");
        writeCsv(configuration, transformer.getEdges(), "edges.csv");
    }

    private <T> void writeCsv(Configuration configuration, List<T> outputs, String name) {
        try (var writer = new FileWriter(new File(configuration.getOutputDirectory(), name))) {
            var csvWriter = new StatefulBeanToCsvBuilder<T>(writer)
                    .build();

            csvWriter.write(outputs);
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, List<BugRecordInput>> groupSameFile(List<BugRecordInput> records) {
        var fileMap = new HashMap<String, List<BugRecordInput>>();
        for (var record : records) {
            var splindex = record.getParent().indexOf("$");
            var adjustedParent = record.getParent().substring(0, splindex < 0 ? record.getParent().length() : splindex);
            var id = record.getProject() + "#" + record.getHash() + "://" + adjustedParent;
            var list = fileMap.computeIfAbsent(id, k -> new ArrayList<>());
            list.add(record);
        }

        return fileMap;
    }

    private Configuration getConfiguration(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Method Extractor").build()
                .defaultHelp(true)
                .description("Extract Method for BugHunter classes.");

        parser.addArgument("-i", "--input")
                .required(true)
                .help("The input to the program. If this is a java file it will prepare data for prediction, and if it is csv it will prepare for training.");

        parser.addArgument("-wd", "--working-directory")
                .setDefault(".")
                .help("The directory containing all of the input files. ");

        parser.addArgument("-o", "--output")
                .setDefault("output")
                .help("The output directory for all the result files");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        return new Configuration()
                .setFileName(ns.getString("input"))
                .setInputDirectory(ns.getString("working_directory"))
                .setOutputDirectory(ns.getString("output"));
    }


    private List<BugRecordInput> getRecords(Configuration configuration) {
        try (var reader = new FileReader(configuration.getFileName())) {
            var csvReader = new CsvToBeanBuilder<BugRecordInput>(reader)
                    .withType(BugRecordInput.class)
                    .build();

            return csvReader.stream().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}