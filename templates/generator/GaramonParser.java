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
        if (args.length < 3 || !args[0].equals("-d")) {
            System.err.println("Usage: Main -d <outputDirectory> <file1.java> ... <filen.java>");
            System.exit(1);
        }

        Path outDir = Path.of(args[1]);
        NativeLoader.load();

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

        for (int i = 2; i < args.length; i++) {
            processFile(Path.of(args[i]), outDir, tmplOne, tmplCons);
        }
    }

    private static void processFile(Path input, Path outDir, String tmplOne, String tmplCons) throws Exception {
        String data = Files.readString(input);
        String result = GaramonParser.process(data, tmplOne, tmplCons);
        String base = input.getFileName().toString();
        Path outFile = outDir.resolve(base);
        Files.writeString(outFile, result);
        System.out.println("Saved parser output to: " + outFile);

    }
}
