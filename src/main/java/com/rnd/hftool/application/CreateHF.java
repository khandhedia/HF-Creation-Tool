package com.rnd.hftool.application;

import com.rnd.hftool.dto.JarRecord;
import com.rnd.hftool.enums.ArtifactType;
import com.rnd.hftool.enums.FileTokens;
import com.rnd.hftool.utilities.JarUtilities;
import com.rnd.hftool.utilities.SearchUtilities;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import static com.rnd.hftool.enums.ArtifactType.CLASS_FILE;
import static com.rnd.hftool.enums.FileTokens.createToken;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public class CreateHF
{
    private static final String DELIMITER = "=";
    private final static Logger log = Logger.getLogger(CreateHF.class);
    private Map<Path, File> moduleJarMap;
    private Map<Path, List<JarRecord>> moduleJarRecordsMap;
    private Path componentPath = null;
    private Path modulePath = null;
    private Path currentPath = null;
    private Path basePath = null;
    private JarUtilities jarUtilities;
    private SearchUtilities searchUtilities;
    private int lineCounter;
    private SimpleDateFormat simpleDateFormat;
    private boolean debugMode;

    public CreateHF(boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    public void createHF(Path currentPath, File inputFile) throws IOException
    {
        Date startDate = new Date(System.currentTimeMillis());
        log.info("Create HF : STARTED - Input file " + inputFile.getAbsolutePath());

        init(currentPath);
        processInputFile(inputFile);
        packJars();
        printJarPaths();
        createZip();

        Date endDate = new Date(System.currentTimeMillis());
        long timeDiffInMillis = endDate.getTime() - startDate.getTime();

        log.info("Create HF : COMPLETED - Total time taken in milliseconds : " + timeDiffInMillis+"\n\n\n\n");

        log.info("Before delivering/deploying the hotfix, double-check that:");
        log.info("1. The printed log doesn't have any ERROR/WARN records. If any, check/correct the Input file as required.");
        log.info("2. The content included in ZIP/JAR is exactly what was intended and nothing more/less/different is packed.");
        log.info("3. The content included is JAR is packed at correct path.");
        log.info("Happy HotFixing!!\n\n");
    }

    private void init(Path currentPath)
    {
        this.currentPath = currentPath;
        this.componentPath = currentPath;
        this.modulePath = currentPath;
        this.basePath = currentPath;
        this.simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        this.simpleDateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
        this.jarUtilities = new JarUtilities(true);
        this.searchUtilities = new SearchUtilities(true);
        this.lineCounter = 0;
        this.moduleJarMap = new HashMap<>();
        this.moduleJarRecordsMap = new HashMap<>();
    }

    private void processInputFile(File inputFile) throws IOException
    {
        Scanner scanner = new Scanner(inputFile);
        while (scanner.hasNextLine()) { processLine(scanner.nextLine().trim()); }
    }

    private void processLine(String line) throws IOException
    {
        //Increment line counter
        lineCounter++;

        //Empty Lines Ignored
        if (isEmpty(line))
        {
            log.info("Line " + lineCounter + ": Blank. SKIPPED.");
            return;
        }

        //If line contains delimiter, it needs further checks
        if (contains(line, DELIMITER))
        {
            //Configure Scanner
            Scanner scanner = new Scanner(line);
            scanner.useDelimiter(DELIMITER);

            //Extract token
            String token = scanner.hasNext()
                           ? scanner.next()
                           : null;
            if (null == token)
            {
                log.info("Line " + lineCounter + ": Contains delimiter '"+DELIMITER+"' without any token preceding it. SKIPPED.");
                return;
            }

            //Extract value
            String value = scanner.hasNext()
                           ? scanner.next().trim()
                           : null;
            if (null == value)
            {
                log.info("Line " + lineCounter + ": Contains delimiter '"+DELIMITER+"' without any value following it. SKIPPED.");
                return;
            }

            log.info("Line " + lineCounter + ": Processing.");

            //Act based on token
            switch (createToken(token))
            {
                case COMPONENT:
                    setComponentPath(value);
                    break;
                case MODULE:
                    setModulePath(value);
                    break;
                case BASE:
                    setBasePath(value);
                    break;
                case UNKNOWN:
                default:
                    log.error(
                            "Line " + lineCounter + ": Token '" + token + "' value is INVALID. Valid values: " + FileTokens.MODULE + " or " + FileTokens.BASE);
            }

        }
        //else it is assumed to be a class file or regular file
        else
        {
            if (isClassFile(line)) { searchAndPackClassFile(line); }
            else { searchAndPackRegularFile(line); }
        }
    }


    private void setComponentPath(String componentName)
    {
        log.debug("Line " + lineCounter + ": Component: " + quote(componentName));

        //Search Component Directory by Name, starting from the Current Path (Directory from where the tool is run)
        List<Path> localComponentPath = searchUtilities.search(currentPath, componentName, ArtifactType.DIRECTORY);

        //If search result is NULL, throw RTException
        if (null == localComponentPath)
        {
            String msg = "Component " + componentName + " could not be found under " + currentPath;
            log.error("Line " + lineCounter + ": Component: " + quote(componentName) + " - Error: " + msg);
            throw new RuntimeException(msg);
        }

        //If search returns multiple locations, choose the nearest location (having minimum number of path delimiter /)
        if (localComponentPath.size() > 1)
        {
            log.warn("Line " + lineCounter + ": Component: " + quote(componentName) + " - Found at multiple places:");
            localComponentPath.forEach(path -> log.warn(path));
            componentPath = localComponentPath.stream().sorted(Comparator.comparingInt(o -> StringUtils.countMatches(o.toString(), "/"))).findFirst().get();
            log.warn("Line " + lineCounter + ": Component: " + quote(componentName) + " - Considering path: " + componentPath);
        }

        //If search returns exactly ONE result, set it as class level componentPath.
        else if (localComponentPath.size() == 1)
        {
            componentPath = localComponentPath.get(0);
            log.debug("Line " + lineCounter + ": Component: " + quote(componentName) + " - Considering path: " + componentPath);
        }
    }

    private void setModulePath(String moduleName)
    {
        log.debug("Line " + lineCounter + ": Module: " + quote(moduleName));

        //Search Module Directory by Name, starting from Component Path
        List<Path> localModulePath = searchUtilities.search(componentPath, moduleName, ArtifactType.DIRECTORY);

        //If search result is NULL, throw RTException
        if (null == localModulePath)
        {
            String msg = "Module " + moduleName + " could not be found under " + componentPath;
            log.error("Line " + lineCounter + ": Module: " + quote(moduleName) + " - Error: " + msg);
            throw new RuntimeException(msg);
        }

        //If search returns multiple locations, choose the nearest location (having minimum number of path delimiter /)
        if (localModulePath.size() > 1)
        {
            log.warn("Line " + lineCounter + ": Module: " + quote(moduleName) + " - Found at multiple places:");
            localModulePath.forEach(path -> log.warn(path));
            modulePath = localModulePath.stream().sorted(Comparator.comparingInt(o -> StringUtils.countMatches(o.toString(), "/"))).findFirst().get();
            log.warn("Line " + lineCounter + ": Module: " + quote(moduleName) + " - Considering path: " + modulePath);

        }

        //If search returns exactly ONE result, set it as class level modulePath.
        else if (localModulePath.size() == 1)
        {
            modulePath = localModulePath.get(0);
            log.debug("Line " + lineCounter + ": Module: " + quote(moduleName) + " - Considering path: " + modulePath);
        }
    }

    private void setBasePath(String basePackage)
    {
        log.debug("Line " + lineCounter + ": Base Package: " + quote(basePackage));

        //Search Base Directory by Name, starting from Module Path
        List<Path> localPackagePath = searchUtilities.search(modulePath, basePackage, ArtifactType.DIRECTORY);

        //If search result is NULL, throw RTException
        if (null == localPackagePath)
        {
            String msg = "Base package " + basePackage + " could not be found under " + modulePath;
            log.error("Line " + lineCounter + ": Base Package: " + quote(basePackage) + " - Error: " + msg);
            throw new RuntimeException(msg);
        }

        //If search returns multiple locations, throw RTException
        if (localPackagePath.size() > 1)
        {
            log.warn("Line " + lineCounter + ": Base Package: " + quote(basePackage) + " - Found at multiple places:");
            localPackagePath.forEach(path -> log.warn(path));
            basePath = localPackagePath.stream().sorted(Comparator.comparingInt(o -> StringUtils.countMatches(o.toString(), "/"))).findFirst().get();
            log.warn("Line " + lineCounter + ": Base Package: " + quote(basePackage) + " - Considering path: " + basePath);
        }

        //If search returns exactly ONE result, set it as class level basePath.
        else if (localPackagePath.size() == 1)
        {
            basePath = localPackagePath.get(0);
            log.debug("Line " + lineCounter + ": Base Package: " + quote(basePackage) + " - Considering path: " + basePath);
        }
    }


    private boolean isClassFile(String line)
    {
        return endsWithIgnoreCase(line, ".class") || endsWithIgnoreCase(line, ".java");
    }

    private void searchAndPackClassFile(String line) throws IOException
    {
        List<Path> pathList;

        line = removeJavaOrClassExtension(line);

        if (contains(line, ".")) { pathList = getPathFromFullyQualifiedName(line); }
        else { pathList = searchUtilities.search(basePath, line, CLASS_FILE); }

        scheduleForAddingInJar(line, pathList);
    }

    private String removeJavaOrClassExtension(String line)
    {
        if (endsWithIgnoreCase(line, ".class") || endsWithIgnoreCase(line, ".java")) { line = substringBeforeLast(line, "."); }
        return line;
    }

    private List<Path> getPathFromFullyQualifiedName(String line)
    {
        line = line.replaceAll("\\.", "/");
        String qualifiedPath = basePath + "/" + substringBeforeLast(line, "/");
        String fileName = substringAfterLast(line, "/");
        File startPath = new File(qualifiedPath);
        if(!startPath.exists())
        {
            log.error("Line "+lineCounter+": Path doesn't exist : " + qualifiedPath);
            return null;
        }

        return searchUtilities.search(startPath.toPath(), fileName, CLASS_FILE);
    }

    private void searchAndPackRegularFile(String line) throws IOException
    {
        List<Path> pathList = searchUtilities.search(basePath, line, ArtifactType.REGULAR_FILE);
        scheduleForAddingInJar(line, pathList);
    }

    private void scheduleForAddingInJar(String line, List<Path> pathList)
    {
        if (!isNotEmpty(pathList))
        {
            log.error("Line " + lineCounter + ": File: " + line + " NOT found.");
        }
        else
        {
            log.debug("Line " + lineCounter + ": File: " + line + " Found below records:");
            List<JarRecord> jarRecords = Optional.ofNullable(moduleJarRecordsMap.get(modulePath)).orElse(new ArrayList<>());

            pathList.stream().forEach(path -> {
                JarRecord jarRecord = prepareJarRecord(path);
                log.debug(jarRecord);
                jarRecords.add(jarRecord);
            });

            moduleJarRecordsMap.put(modulePath, jarRecords);
        }
    }

    private JarRecord prepareJarRecord(Path path)
    {
        JarRecord jarRecord = new JarRecord();
        jarRecord.setSourceFile(path.toFile());
        jarRecord.setFilePathWithinJar(basePath.relativize(path).toString());
        return jarRecord;
    }

    private void packJars() throws IOException
    {
        for (Path localModulePath : moduleJarRecordsMap.keySet())
        {
            List<JarRecord> jarRecords = moduleJarRecordsMap.get(localModulePath);
            if (isNotEmpty(jarRecords))
            {
                String jarPath = localModulePath.toString() + "\\" + localModulePath.getFileName() + "_" + simpleDateFormat
                        .format(currentTimeMillis()) + ".jar";
                moduleJarMap.put(localModulePath, jarUtilities.createJar(jarPath, jarRecords));
            }
        }
    }

    private void printJarPaths()
    {
        if(moduleJarMap.keySet().size() == 0)
            return;

        log.info("Jar files created at:");
        for (Path path : moduleJarMap.keySet())
        { log.info(moduleJarMap.get(path)); }
    }

    private void createZip()
    {
        List<String> jarPaths = moduleJarMap.keySet().stream().map(path -> moduleJarMap.get(path).toPath().toString()).collect(Collectors.toList());

        if (isEmpty(jarPaths)) { return; }

        String zipPath = currentPath.toString() + "\\Hotfix_" + simpleDateFormat.format(currentTimeMillis()) + ".zip";
        try
        {
            jarUtilities.compressFilesToZip(jarPaths, zipPath);
        }
        catch (IOException e)
        {
            if(debugMode)
                e.printStackTrace();
            log.error("Error creating zip file : " + e.getMessage());
        }
        log.info("Zip file created at: " + zipPath);
    }

    private String quote(String aText)
    {
        String QUOTE = "'";
        return QUOTE + aText + QUOTE;
    }
}
