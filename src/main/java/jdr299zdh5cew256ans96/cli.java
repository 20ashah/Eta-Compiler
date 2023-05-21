package main.java.jdr299zdh5cew256ans96;

// TODO: figure out syntax highlighting
import java_cup.runtime.*;
import java_cup.runtime.Symbol;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;

public class cli {

        public static final String[] SUPPORTED_OPTIMIZATIONS = { "cf", "reg", "dce", "copy" };
        private static Options compilerCommands = new Options();
        private static CommandLine parsedCommandLine;
        private static String[] sourceInputFiles;
        public static String[] enabledOptimizations;

        // TODO: better place for this
        public static String[] supportedOptDescriptions = {
                        "constant folding. Evaluate expressions consisting of"
                                        + " only constants",
                        "register allocation. Efficiently allocate x86 registers"
                                        + " using Chaitin's algorithm.",
                        "dead code elimination. Remove unreachable code.",
                        "copy propagation. Replace variable copies." };

        public static void main(String[] args) {
                addCompilerCommands();
                parseAndPerformUserOperation(args);
        }

        private static void addCompilerCommands() {
                addHelpCommand();
                addAbstractAssemblyCommand();
                addPrintOptimizationsCommand();
                addLexToFileCommand();
                addParseToFileCommand();
                addTypeCheckToFileCommand();
                addIRToFileCommand();
                addRunIRCodeCommand();
                addOptimizeIRCommand();
                addControlFlowGraphToFileCommand();
                addSourcePathCommand();
                addLibraryPathCommand();
                addDestinationPathCommand();
                addAssemblyDestinationPathCommand();
                addTargetOSCommand();
                addOptimizationCommand();
        }

        private static void addHelpCommand() {
                Option help = Option.builder(null)
                        .desc("Print a synopsis of options")
                        .longOpt("help")
                        .build();
                compilerCommands.addOption(help);
        }

        private static void addAbstractAssemblyCommand() {
                Option abstractAssembly = Option.builder(null)
                        .desc("Output abstract " +
                                "assembly to .s file")
                        .longOpt("aa")
                        .build();
                compilerCommands.addOption(abstractAssembly);
        }

        private static void addPrintOptimizationsCommand() {
                Option printOptimizations = Option.builder(null)
                        .desc("Print list of supported optimizations")
                        .longOpt("print-opts")
                        .build();
                compilerCommands.addOption(printOptimizations);
        }

        private static void addLexToFileCommand() {
                Option lexToFile = Option.builder(null)
                        .desc("Generate output from lexical analysis")
                        .longOpt("lex")
                        .build();
                compilerCommands.addOption(lexToFile);
        }

        private static void addParseToFileCommand() {
                Option parseToFile = Option.builder(null)
                        .desc("Generate output from syntactic analysis")
                        .longOpt("parse")
                        .build();
                compilerCommands.addOption(parseToFile);
        }

        private static void addTypeCheckToFileCommand() {
                Option typeCheckToFile = Option.builder(null)
                        .desc("Generate output from semantic analysis")
                        .longOpt("typecheck")
                        .build();
                compilerCommands.addOption(typeCheckToFile);
        }

        private static void addIRToFileCommand() {
                Option irToFile = Option.builder(null)
                        .desc("Generate lowered intermediate code")
                        .longOpt("irgen")
                        .build();
                compilerCommands.addOption(irToFile);
        }

        private static void addRunIRCodeCommand() {
                Option runIRCode = Option.builder(null)
                        .desc("Generate and run lowered intermediate code")
                        .longOpt("irrun")
                        .build();
                compilerCommands.addOption(runIRCode);
        }

        private static void addOptimizeIRCommand() {
                Option optimizeIR = Option.builder(null)
                        .argName("phase")
                        .hasArg()
                        .desc("Generate intermediate code at the " +
                                "specified phase of optimization " +
                                "(initial = before optimizations, " +
                                "final = after optimizations)")
                        .longOpt("optir")
                        .build();
                compilerCommands.addOption(optimizeIR);
        }

        private static void addControlFlowGraphToFileCommand() {
                Option generateControlFlowGraph = Option.builder(null)
                        .argName("phase")
                        .hasArg()
                        .desc("Generate control-flow graph at the" +
                                "specified phase of optimization " +
                                "(initial = before optimizations, " +
                                "final = after optimizations)")
                        .longOpt("optcfg")
                        .build();
                compilerCommands.addOption(generateControlFlowGraph);
        }

        private static void addSourcePathCommand() {
                Option sourcePath = Option.builder("sourcepath")
                        .argName("path")
                        .hasArg()
                        .desc("Specify where to find input source files. Default is " +
                                "current directory")
                        .build();
                compilerCommands.addOption(sourcePath);
        }

        private static void addLibraryPathCommand() {
                Option libraryPath = Option.builder("libpath")
                        .argName("path")
                        .hasArg()
                        .desc("Specify where to find library interface files. Default is " +
                                "current directory")
                        .build();
                compilerCommands.addOption(libraryPath);
        }

        private static void addDestinationPathCommand() {
                Option fileDestination = Option.builder("D")
                        .argName("path")
                        .hasArg()
                        .desc("Specify where to place generated diagnostic files")
                        .build();
                compilerCommands.addOption(fileDestination);
        }

        private static void addAssemblyDestinationPathCommand() {
                Option assemblyFileDestination = Option.builder("d")
                        .argName("path")
                        .hasArg()
                        .desc("Specify where to place generated assembly output files")
                        .build();
                compilerCommands.addOption(assemblyFileDestination);
        }

        private static void addTargetOSCommand() {
                Option targetOS = Option.builder("target")
                        .argName("OS")
                        .hasArg()
                        .desc("Specify the operating system for which to generate code " +
                                "(linux, windows, or macOS). " +
                                "Default is linux.")
                        .build();
                compilerCommands.addOption(targetOS);
        }

        private static void addOptimizationCommand() {
                Option optimizations = Option.builder("O")
                        .argName("opt")
                        .hasArg()
                        .optionalArg(true)
                        .desc("Enable specified optimization opt. See" +
                                " --report-ops to get a list of " +
                                "options. Supply no opts (-O) to " +
                                "disable " +
                                "all optimizations. All optimizations" +
                                " are enabled by default.")
                        .build();
                compilerCommands.addOption(optimizations);
        }

        private static void parseAndPerformUserOperation(String[] args) {
                try {
                        parseCommandLineArguments(args);
                        performUserOperation();
                } catch (Exception e) {
                        System.out.println(e.getMessage());
                }
        }

        private static void parseCommandLineArguments(String[] args) {
                padOptimizationArgument(args);
                CommandLineParser parser = new DefaultParser();
                parsedCommandLine = parser.parse(compilerCommands, args);
                sourceInputFiles = parsedCommandLine.getArgs();
        }

        private static void padOptimizationArgument(String[] args) {
                // if the -O command is the last thing directly before
                // the source files, a dummy optimization is inserted to
                // prevent the source file being confused as an
                // optimization
                // TODO: clean
                if (args.length >= 2) {
                        if (args[args.length - 2].trim().equals("-O")) {
                                args[args.length - 2] = args[args.length - 2] + "dum";
                        }
                }
        }

        private static void printOptimizationOptions() {
                int i = 0;
                for (String o : SUPPORTED_OPTIMIZATIONS) {
                        System.out.print(o);
                        if (i < supportedOptDescriptions.length) {
                                String optD = supportedOptDescriptions[i];
                                System.out.println("  " + optD);
                        } else {
                                System.out.println();
                        }
                        i++;
                }
        }

        private static void printCompilerCommandOptions() {
                HelperCli.printHelpOptions(compilerCommands);
        }

        private static void buildCompilerObject() throws Exception {
                String sourceDir = parsedCommandLine.getOptionValue("sourcepath");
                String destDir = parsedCommandLine.getOptionValue("D");
                String libDir = parsedCommandLine.getOptionValue("libpath");
                String assemblyDestDir = parsedCommandLine.getOptionValue("d");

                String targetOS = "linux";
                if (parsedCommandLine.hasOption("target")) {
                        targetOS = parsedCommandLine.getOptionValue(
                                "target");
                }

                boolean optirInitialFlag = false;
                boolean optirFinalFlag = false;
                boolean optcfgInitialFlag = false;
                boolean optcfgFinalFlag = false;

                if (parsedCommandLine.hasOption("optir")) {
                        String[] optirPhases = parsedCommandLine.getOptionValues(
                                "optir");
                        for (String s : optirPhases) {
                                if (s.equals("initial")) {
                                        optirInitialFlag = true;
                                } else if (s.equals("final")) {
                                        optirFinalFlag = true;
                                } else {
                                        throw new Exception("invalid " +
                                                "phase of ir. Must be" +
                                                " either initial or " +
                                                "final.");
                                }
                        }
                }

                if (parsedCommandLine.hasOption("optcfg")) {
                        String[] optCfgPhases = parsedCommandLine.getOptionValues(
                                "optcfg");
                        for (String s : optCfgPhases) {
                                if (s.equals("initial")) {
                                        optcfgInitialFlag = true;
                                } else if (s.equals("final")) {
                                        optcfgFinalFlag = true;
                                } else {
                                        throw new Exception("invalid " +
                                                "phase of ir. Must be" +
                                                " either initial or " +
                                                "final.");
                                }
                        }
                }

                if (!parsedCommandLine.hasOption("O")) {
                        enabledOptimizations =
                                SUPPORTED_OPTIMIZATIONS;
                } else {
                        enabledOptimizations = parsedCommandLine.getOptionValues("O");
                        if (enabledOptimizations == null) {
                                enabledOptimizations = new String[1];
                        }
                }

                boolean lexFlag = parsedCommandLine.hasOption("lex");
                boolean parseFlag = parsedCommandLine.hasOption("parse");
                boolean typecheckFlag = parsedCommandLine.hasOption("typecheck");
                boolean generateIRFlag = parsedCommandLine.hasOption("irgen");
                boolean runIRFlag = parsedCommandLine.hasOption("irrun");
                boolean abstractAssemblyFlag = parsedCommandLine.hasOption("aa");

                HelperCli helperCli = new HelperCli(lexFlag, parseFlag, typecheckFlag, generateIRFlag,
                        runIRFlag, optirInitialFlag, optirFinalFlag, optcfgInitialFlag,
                        optcfgFinalFlag, sourceDir, libDir, destDir,
                        assemblyDestDir,
                        targetOS, abstractAssemblyFlag);
                helperCli.compile(sourceInputFiles);
        }

        private static void performUserOperation() throws Exception {
                if (parsedCommandLine.hasOption("report-opts") && !parsedCommandLine.hasOption("help")) {
                        printOptimizationOptions();
                } else if (parsedCommandLine.hasOption("help") || sourceInputFiles.length == 0) {
                        printCompilerCommandOptions();
                } else {
                        buildCompilerObject();
                }
        }

}