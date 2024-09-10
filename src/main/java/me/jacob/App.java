package me.jacob;

import com.opencsv.bean.CsvToBeanBuilder;
import me.jacob.entities.BugRecordInput;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class App {
    Pattern pattern = Pattern.compile("$");

    public static void main(String[] args) {
        App app = new App();
        app.run(args);
    }

    public void run(String[] args) {
        var configuration = getConfiguration(args);
        var records = getRecords(configuration);

        Map<String, List<BugRecordInput>> fileMap = groupSameFile(records);
        for (var recordCollection : fileMap.values()) {
            var transformer = new BugRecordTransformer(recordCollection, configuration);
            transformer.run();
        }
    }

    private Map<String, List<BugRecordInput>> groupSameFile(List<BugRecordInput> records) {
        var fileMap = new HashMap<String, List<BugRecordInput>>();
        for (var record : records) {
            var adjustedParent = pattern.split(record.getParent())[0];
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
                .help("The csv file containing all bug records");

        parser.addArgument("-wd", "--working-directory")
                .setDefault(".")
                .help("the working directory of the dataset");

        parser.addArgument("-o", "--output")
                .setDefault("output");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        return new Configuration()
                .setFileName(ns.getString("input"))
                .setWorkingDirectory(ns.getString("working_directory"))
                .setOutputFile(ns.getString("output"));
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