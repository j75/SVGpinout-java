package eu.f.m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * List of all known packages.
 *
 * @author Marian-N. I.
 */
class ICPackages {
    private static final String PKG_CSV = "/packages.csv";
    private static final int NB_OF_FIELDS = 9;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, ICPackage> packagesMap = new HashMap<>();

    /**
     *  see <a href="https://stackoverflow.com/questions/15749192/how-do-i-load-a-file-from-resource-folder">How do I load a file from resource folder?</a>
     */
    ICPackages(boolean display) {
        //ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = getClass ().getResourceAsStream(PKG_CSV)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    List<String[]> packages = getPackagesFromReader(reader, display);
                    logNbOfLines (packages, PKG_CSV);
                    setPackagesMap(packages);
                }
                catch (IOException e) {
                    logger.warn("exception thrown when parsing packages file '{}'", PKG_CSV, e);
                    throw new UncheckedIOException(e);
                }
            }
            else {
                logger.warn("no InputStream for resource {}", PKG_CSV);
                throw new IllegalStateException("no InputStream for resource!");
            }

        } catch (IOException e) {
            logger.warn("exception thrown", e);
            throw new UncheckedIOException(e);
        }
    }

    ICPackages(String fileName, boolean display) {
        File packagesFile = new File(fileName);
        if (!packagesFile.canRead()) {
            logger.warn("Cannot read '{}' packages file", packagesFile.getAbsolutePath());
            throw new IllegalArgumentException("cannot read packages file");
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(packagesFile))) {
            List<String[]> packages = getPackagesFromReader (reader, display);
            logNbOfLines (packages, packagesFile.getAbsolutePath());
            setPackagesMap(packages);
        }
        catch (IOException e) {
            logger.warn("exception thrown for file {}", packagesFile.getAbsolutePath(), e);
            throw new UncheckedIOException(e);
        }
    }

    int getNbOfPackages() {
        logger.info("Loaded {} package definitions", packagesMap.size());
        return packagesMap.size();
    }

    Optional<ICPackage> getPackage (String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        ICPackage p = packagesMap.get(name);
        logger.debug("package: name = '{}', value = {}", name, p);
        return Optional.ofNullable(p);
    }

    //////////////////
    private void logNbOfLines (List<String[]> packages, String filePath) {
        logger.debug("read {} lines from '{}' file", packages.size(), filePath);
    }

    private List<String[]> getPackagesFromReader (BufferedReader reader, boolean display) throws IOException {
        String line;
        List<String[]> packages = new ArrayList<>();
        if (display) {
            IO.println("packages file content:");
            IO.println("------------------------------------------------------------------");
        }
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                logger.debug("blank line found");
                continue;
            }
            if (display) {
                IO.println(line);
            }
            String[] fields = line.split(",");
            if (fields.length < NB_OF_FIELDS) {
                logger.warn("line '{}' => only {} fields", line, fields.length);
                continue;
            }
            packages.add(fields);
        }
        if (display) {
            IO.println("");
        }
        return packages;
    }

    private void setPackagesMap(List<String[]> packages) {
        for (String[] icPackage : packages) {
            if (icPackage.length < 9) {
                logger.warn("CSV line too short: {}", icPackage.length);
                continue;
            }
            if (!Character.isDigit(icPackage[1].charAt(0))) {
                logger.debug("no digit in second field, maybe first line? '{},{},...'", icPackage[0], icPackage[1]);
                continue;
            }
            String name = icPackage[0];
            int dotSize = Integer.parseInt(icPackage[6]);
            if (dotSize == 0) {
                logger.warn("Unsupported package: '{}' because dot size is 0", name);
                continue;
            }
            ICPackage ic = new ICPackage(name, Integer.parseInt(icPackage[1]),
                    Integer.parseInt(icPackage[2]),
                    Integer.parseInt(icPackage[3]),
                    Integer.parseInt(icPackage[4]),
                    Integer.parseInt(icPackage[5]),
                    dotSize, Integer.parseInt(icPackage[7]),
                    Float.parseFloat(icPackage[8])
                    );
            logger.trace("IC package = {}", ic);
            packagesMap.put (ic.name(), ic);
        }
    }
}
