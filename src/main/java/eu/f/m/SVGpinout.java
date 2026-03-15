package eu.f.m;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.concurrent.*;

/**
 * @author Marian-N. I.
 */
public class SVGpinout {
    private static final Logger LOGGER = LoggerFactory.getLogger(SVGpinout.class);
    private static final long ONE_MEGA = 1024L * 1024L;

    /**
     * Execution mode
     */
    private enum ExecMode {
        SERIAL,
        PARALLEL,
        FUTURES
    }

    static void main(String... args) throws ParseException {
        if (args.length == 0) {
            LOGGER.info("Usage: SVGpinout [pinout csv | folder csv]");
            System.exit(0);
        }

        Options options = new Options();
        options.addOption("o", "outdir", true, "output folder")
                .addOption("l", "logodir", true, "Logos folder")
                .addOption("n", "nologo", false, "no logo in output file")
                .addOption("c", "cache", false, "cache logo file")
                .addOption("h", "help", false, "print help")
                .addOption("p", "packages", true, "alternate packages.csv file")
                .addOption("d", "display", false, "display packages.csv file")
                .addOption("b", "loop", false, "run in loop")
                .addOption("r", "repeat", true, "repeat 'x' times")
                .addOption("x", "execution", true, "parallel or serial (default) folder processing")
                .addOption("s", "statistics", false, "statistics");
        // Create a parser
        CommandLineParser parser = new DefaultParser();
        LOGGER.debug("{} params", args.length);
        CommandLine cmd = parser.parse(options, Arrays.stream(args)
                .filter (s -> s != null && !s.trim().isEmpty())
                .toArray(String[]::new));
        if (cmd.hasOption('h')) {
            usage();
        }

        String[] remainingArgs = cmd.getArgs();
        if (remainingArgs == null || remainingArgs.length < 1) {
            usage();
        }
        File fileOrDir = getWorkingFile(cmd.getArgs()[0]);

        ICPackages packages;
        boolean display = cmd.hasOption('d');
        if (cmd.hasOption('p')) {
            String packagesFile = cmd.getOptionValue('p');
            LOGGER.info("will use the alternate packages file {}", packagesFile);
            packages = new ICPackages(packagesFile, display);
        } else {
            packages = new ICPackages(display);
        }
        if (packages.getNbOfPackages() < 1) {
            LOGGER.warn ("No package definitions found!");
            System.exit(2);
        }

        int nbTimes = 1;
        if (cmd.hasOption('r')) {
            int nbRepeats = Integer.parseInt(cmd.getOptionValue('r'));
            if (nbRepeats > 0) {
                nbTimes = nbRepeats;
                LOGGER.info("will run {} times", nbTimes);
            }
        }

        ExecMode mode = ExecMode.SERIAL;
        if (cmd.hasOption('x')) {
            String execOption = cmd.getOptionValue('x');
            mode = parseExecModeOption (execOption);
        }

        boolean loop = cmd.hasOption('b');
        if (loop) {
            LOGGER.info("will run in loop");
        }
        boolean cache = cmd.hasOption('c');
        if (cache) {
            LOGGER.info("will cache logo file(s)");
        }

        boolean stats = cmd.hasOption('s');
        long start = 0;
        MemoryMXBean memoryBean = null;
        if (stats) {
            start = System.nanoTime();
            memoryBean = ManagementFactory.getMemoryMXBean();
        }
        try {
            do {
                run(packages, cmd, fileOrDir, mode, cache);
                if (loop) {
                    LOGGER.trace("running in loop");
                } else {
                    --nbTimes;
                    LOGGER.trace("{} more iteration(s) to do", nbTimes);
                }
            } while (loop || (nbTimes > 0));
        }
         finally {
            if (stats) {
                displayStats (start, memoryBean);
            }
        }
    }

    private static File getWorkingFile (String file) {
        if (file == null || file.isBlank()) {
            LOGGER.warn("No file/directory name to use!");
            System.exit(1);
        }
        File f;
        if ("all".equals(file)) {
            f = new File ("csv");
            if (f.isDirectory() && f.canRead()) {
                LOGGER.debug("for compatibility will use the '{}' folder",f.getAbsolutePath());
                return f;
            }
        }
        f = new File (file);
        if ((f.isFile() && f.canRead()) || f.isDirectory()) {
            LOGGER.debug("will process '{}'", f.getAbsolutePath());
        } else {
            LOGGER.info("Cannot read {}", f.getAbsolutePath());
            System.exit(2);
        }
        return f;
    }

    private static void displayStats(long debut, MemoryMXBean memoryBean) {
        IO.println("-------------------------------");
        System.out.printf("Total time execution: %d msec.%n", TimeUnit.MILLISECONDS
                .convert(System.nanoTime() - debut, TimeUnit.NANOSECONDS));
        // Heap memory usage
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        // Non-heap memory usage
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();

        IO.println("-------------------------------");
        IO.println("Heap Memory Usage:");
        printMemoryUsage(heapMemory);
        IO.println("Non-heap Memory Usage:");
        printMemoryUsage(nonHeapMemory);
    }

    /**
     * @see <a href="https://www.javaspring.net/blog/during-execution-how-can-a-java-program-tell-how-much-memory-it-is-using/">How to Check Java Program Memory Usage During Execution: Simple Methods</a>
     * @param usage {@link MemoryUsage}
     */
    private static void printMemoryUsage(MemoryUsage usage) {
        System.out.println("  init: " + usage.getInit() / ONE_MEGA + " MB");
        System.out.println("  used: " + usage.getUsed() / ONE_MEGA + " MB");
        System.out.println("  committed: " + usage.getCommitted() / ONE_MEGA + " MB");
        System.out.println("  max: " + usage.getMax() / ONE_MEGA + " MB");
    }

    private static void usage() {
        IO.println(String.format ("Usage: %s [options] <CSV file or directory>", SVGpinout.class.getName().toLowerCase()));
        IO.println("  If filename is 'all' then a folder named 'csv' will be used");
        IO.println("");
        IO.println("Possible options are:");
        IO.println("         [-o | --outdir <output directory>]");
        IO.println("         [-l | --logodir <logos directory>]");
        IO.println("         [-n | --nologo]");
        IO.println("         [-p | --packages <alternate packages.csv file>]");
        IO.println("         [-d | --display <packages.csv file>]");
        IO.println("         [-s | --statistics]");
        IO.println("         [-b | --loop]");
        IO.println("         [-r | --repeat <# of times> (should be > 0)>");
        IO.println("         [-x | --execution <mode>: only for folder processing");
        IO.println("                     mode is one of 'par', 'fut' or 'ser'");
        IO.println("                     'par' => parallel, 'fut' => using CompletableFuture, 'ser' => serial (default)");
        IO.println("         [-c | --cache] use logos cache (for loop, repeats or folder processing)");
        IO.println("         [-h | --help] => display this help");
        IO.println("");
        IO.println("To increase log details add '-Dorg.slf4j.simpleLogger.defaultLogLevel=debug' option");
        System.exit(3);
    }

    private static void run (ICPackages packages, CommandLine cmd, File fileOrDir, ExecMode mode, boolean cache)
    {
        BatikDrawChip drawChip = new BatikDrawChip();
        if (cache) {

            drawChip.useCache();
        }
        if (cmd.hasOption('o')) {
            String outDir = cmd.getOptionValue('o');
            LOGGER.info("output directory = '{}'", outDir);
            drawChip.setOutputDir(outDir);
        }
        if (cmd.hasOption('n')) {
            LOGGER.info("no logo file will be used");
            drawChip.setNoLogo ();
        } else {
            if (cmd.hasOption('l')) {
                String logosDir = cmd.getOptionValue('l');
                LOGGER.info("will use the alternate logos directory '{}'", logosDir);
                drawChip.setLogoDir(logosDir);
            }
        }
        drawChip.setChipPackages(packages);

        if (fileOrDir.isFile()) {
            drawChip.generate(fileOrDir);
        } else if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            if (files == null || files.length < 1) {
                LOGGER.warn("Folder '{}' is empty!", fileOrDir.getAbsolutePath());
                return;
            }
            LOGGER.info("will execute in {} mode", mode);
            switch (mode) {
                case PARALLEL -> processDirectoryParallel(files, drawChip);
                case FUTURES -> processDirectoryExecutor (files, drawChip);
                default -> processDirectorySerial (files, drawChip);
            }
        }
    }

    private static void processDirectorySerial(File[] files, BatikDrawChip batikDrawChip) {
         for (File chipFile : files) {
             if (chipFile.isDirectory()) {
                 warnDirectory (chipFile);
             } else {
                 LOGGER.trace("processing file '{}'", chipFile.getAbsolutePath());
                 BatikDrawChip drawChip = new BatikDrawChip(batikDrawChip);
                 drawChip.generate(chipFile);
             }
         }
    }

    private static void processDirectoryParallel(File[] files, BatikDrawChip batikDrawChip) {
        Arrays.stream(files).parallel().filter(f -> {
                    if (f.isDirectory()) {
                        warnDirectory (f);
                        return false;
                    }
                    return true;
                })
                .forEach(f -> {
                    BatikDrawChip drawChip = new BatikDrawChip(batikDrawChip);
                    drawChip.generate(f);
                });
    }

    private static void processDirectoryExecutor(File[] files, BatikDrawChip batikDrawChip) {
        try (ExecutorService execService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            CompletableFuture<?>[] futures = Arrays.stream(files).filter(f -> {
                if (f.isDirectory()) {
                    warnDirectory (f);
                    return false;
                }
                return true;
            }).map(f -> (Runnable) () -> {
                BatikDrawChip drawChip = new BatikDrawChip(batikDrawChip);
                drawChip.generate(f);
            }).toList()
                    .stream()
                    .map(task -> CompletableFuture.runAsync(task, execService))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
            execService.shutdown();
        } catch (RuntimeException e) {
            LOGGER.warn("error executing tasks!", e);
        }
    }

    private static ExecMode parseExecModeOption(String execOption) {
        LOGGER.debug("execution mode = '{}'", execOption);
        if (execOption == null || execOption.isBlank()) {
            return ExecMode.SERIAL;
        }
        if (execOption.toLowerCase().startsWith("par")) {
            return ExecMode.PARALLEL;
        }
        if (execOption.toLowerCase().startsWith("fut")) {
            return ExecMode.FUTURES;
        }
        return ExecMode.SERIAL;
    }

    private static void warnDirectory (File d) {
        LOGGER.warn("'{}' is directory, skipping", d.getAbsolutePath());
    }
}
