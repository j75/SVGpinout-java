package eu.f.m;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Marian-N. I.
 */
public class SVGpinout {
    private static final Logger LOGGER = LoggerFactory.getLogger(SVGpinout.class);
    private static final long ONE_MEGA = 1024L * 1024L;

    static void main(String... args) throws ParseException {
        if (args.length == 0) {
            LOGGER.info("Usage: SVGpinout [pinout csv | folder csv]");
            System.exit(0);
        }

        Options options = new Options();
        options.addOption("o", "outdir", true, "output folder")
                .addOption("l", "logodir", true, "Logos folder")
                .addOption("n", "nologo", false, "no logo in output file")
                .addOption("h", "help", false, "print help")
                .addOption("p", "packages", true, "alternate packages.csv file")
                .addOption("d", "display", false, "display packages.csv file")
                .addOption("b", "loop", false, "run in loop")
                .addOption("r", "repeat", true, "repeat 'x' times")
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
            LOGGER.debug("will use the alternate packages file {}", packagesFile);
            packages = new ICPackages(packagesFile, display);
        } else {
            packages = new ICPackages(display);
        }
        int nbTimes = 1;
        if (cmd.hasOption('r')) {
            int nbRepeats = Integer.parseInt(cmd.getOptionValue('r'));
            if (nbRepeats > 0) {
                nbTimes = nbRepeats;
                LOGGER.info("will run {} times", nbTimes);
            }
        }
        if (packages.getNbOfPackages() < 1) {
            LOGGER.warn ("No package definitions found!");
            System.exit(2);
        }
        boolean loop = cmd.hasOption('b');
        LOGGER.debug("run in loop ? {}", loop);
        boolean stats = cmd.hasOption('s');
        long start = 0;
        MemoryMXBean memoryBean = null;
        if (stats) {
            start = System.nanoTime();
            memoryBean = ManagementFactory.getMemoryMXBean();
        }
        try {
            do {
                run(packages, cmd, fileOrDir);
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
        LOGGER.info("Usage: {} [options] <CSV file or directory>",
                SVGpinout.class.getName().toLowerCase());
        LOGGER.info("  If filename is 'all' then a folder named 'csv' will be used");
        LOGGER.info("");
        LOGGER.info("Possible options are:");
        LOGGER.info("         [-o | --outdir <output directory>]");
        LOGGER.info("         [-l | --logodir <logos directory>]");
        LOGGER.info("         [-n | --nologo]");
        LOGGER.info("         [-p | --packages <alternate packages.csv file>]");
        LOGGER.info("         [-d | --display <packages.csv file>]");
        LOGGER.info("         [-s | --statistics]");
        LOGGER.info("         [-b | --loop]");
        LOGGER.info("         [-r | --repeat <# of times> (should be > 0)]");
        LOGGER.info("         [-h | --help] => display this help");
        LOGGER.info("");
        LOGGER.info("To increase log details add '-Dorg.slf4j.simpleLogger.defaultLogLevel=debug' option");
        System.exit(3);
    }

    private static void run (ICPackages packages, CommandLine cmd, File fileOrDir)
    {
        AbstractDrawChipSVG drawChip = new BatikDrawChip(packages);
        if (cmd.hasOption('o')) {
            String outDir = cmd.getOptionValue('o');
            LOGGER.debug("output directory = '{}'", outDir);
            drawChip.setOutputDir(outDir);
        }
        if (cmd.hasOption('n')) {
            LOGGER.debug("no logo file will be used");
            drawChip.setNoLogo ();
        } else {
            if (cmd.hasOption('l')) {
                String logosDir = cmd.getOptionValue('l');
                LOGGER.debug("will use the alternate logos directory '{}'", logosDir);
                drawChip.setLogoDir(logosDir);
            }
        }

        if (fileOrDir.isFile()) {
                drawChip.setChipFile(fileOrDir);
                drawChip.generate();
        } else if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            if (files == null || files.length < 1) {
                LOGGER.warn("Folder '{}' is empty!", fileOrDir.getAbsolutePath());
                return;
            }
            for (File chipFile : files) {
                if (chipFile.isDirectory()) {
                    LOGGER.warn("'{}' is directory, skipping", chipFile.getAbsolutePath());
                    continue;
                }
                LOGGER.trace("processing file '{}'", chipFile.getAbsolutePath());
                try {
                    drawChip.setChipFile(chipFile);
                    drawChip.generate();
                } catch (RuntimeException e) {
                    LOGGER.warn("cannot process file '{}': {}", chipFile.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
}
