package org.garamon.GENERIC;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.garamon.GENERIC.Mvec_h.garamon_parser;

public final class GaramonParser {

    /** Call garamon_parser from native lib */
    public static String process(String data, String tmplOne, String tmplCons) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sData = arena.allocateFrom(data);
            MemorySegment sOne = arena.allocateFrom(tmplOne);
            MemorySegment sCons = arena.allocateFrom(tmplCons);

            // Native call will return const char* allocated on library side
            // The memory will not be freed, but this not a problem because of the process
            // TTL
            MemorySegment cResult = garamon_parser(sData, sOne, sCons);
            if (cResult == null || cResult.address() == 0) {
                throw new IllegalStateException("garamon_parser returned NULL");
            }

            return cResult.getString(0);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean inplace = false;
        java.util.List<Path> files = new java.util.ArrayList<>();

        for (String arg : args) {
            if (arg.equals("--inplace")) {
                inplace = true;
            } else if (arg.startsWith("-")) {
                System.err.println("Unknown argument: " + arg);
                printUsageAndExit();
            } else {
                files.add(Path.of(arg));
            }
        }

        if (files.isEmpty()) {
            printUsageAndExit();
        }

        // Templates reused for all files
        String tmplOne = """
                /**
                 * @return a multivector that contains only the unit basis k-vector project_name_blade.
                 */
                public static Mvec eproject_name_blade(){
                    return new Mvec(Mvec_h.Mvec_eproject_name_blade());
                }
                """;
        String tmplCons = "    int Eproject_name_blade = project_xor_index_blade;\n";

        for (Path inFile : files) {
            try {
                System.err.println("Parsing " + inFile);
                processFile(inFile, inplace, tmplOne, tmplCons);
            } catch (Exception e) {
                System.err.println("Error processing file " + inFile + ": " + e.getMessage());
            }
        }
    }

    private static void processFile(Path inFile, boolean inplace, String tmplOne, String tmplCons) throws Exception {
        String data = Files.readString(inFile);
        String result = GaramonParser.process(data, tmplOne, tmplCons);
        if (inplace) {
            Files.writeString(inFile, result);
        } else {
            System.out.println(result);
        }
    }

    private static void printUsageAndExit() {
        String usage = """
                Garamon java parser for algebra GENERIC

                Usage: [--inplace] file1 .. filen
                  --inplace: Modify files in place. If not set, prints to stdout.
                  file1 .. filen: One or more files to process.
                """;
        System.err.println(usage);
        System.exit(1);
    }
}
