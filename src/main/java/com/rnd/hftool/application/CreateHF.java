package com.rnd.hftool.application;

import com.rnd.hftool.dto.InputFileDTO;
import com.rnd.hftool.dto.InputFileRecordDTO;
import com.rnd.hftool.dto.JarRecordDTO;
import com.rnd.hftool.dto.ZipRecordDTO;
import com.rnd.hftool.enums.InputRecordType;
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
import java.util.stream.Collectors;

import static com.rnd.hftool.enums.ArtifactType.CLASS_FILE;
import static com.rnd.hftool.enums.ArtifactType.DIRECTORY;
import static com.rnd.hftool.enums.ArtifactType.REGULAR_FILE;
import static com.rnd.hftool.enums.InputRecordType.BLANK;
import static com.rnd.hftool.enums.InputRecordType.ERRONEOUS;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.log4j.Logger.getLogger;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public class CreateHF
{
    private final static Logger log = getLogger(CreateHF.class);

    private Map<Path, File> moduleJarMap;
    private Map<Path, List<JarRecordDTO>> moduleJarRecordsMap;
    private Path componentPath = null;
    private Path modulePath = null;
    private Path currentPath = null;
    private Path basePath = null;
    private JarUtilities jarUtilities;
    private SearchUtilities searchUtilities;
    private SimpleDateFormat simpleDateFormat;
    private final boolean debugMode;
    private String zipPrefixPath;

    public CreateHF(boolean debugMode, Path currentPath)
    {
        this.debugMode = debugMode;
        this.currentPath = currentPath;
        this.componentPath = currentPath;
        this.modulePath = currentPath;
        this.basePath = currentPath;
        this.zipPrefixPath="";
    }

    public void createHF(InputFileDTO inputFileDTO) throws IOException
    {
        Date startDate = new Date(currentTimeMillis());
        log.info("Create HF : STARTED");

        if (null == inputFileDTO)
        {
            String message = "Create HF: Input Parsed File object is NULL";
            log.error(message);

            Date endDate = new Date(currentTimeMillis());
            long timeDiffInMillis = endDate.getTime() - startDate.getTime();
            log.error("Create HF: FAILED - Total time taken in milliseconds : " + timeDiffInMillis + "\n\n");
            throw new RuntimeException(message);
        }

        try
        {
            init();
            process(inputFileDTO);
            packJars();
            printJarPaths();
            zipJars();
            createSingleZip();
        }
        catch (RuntimeException e)
        {
            Date endDate = new Date(currentTimeMillis());
            long timeDiffInMillis = endDate.getTime() - startDate.getTime();
            log.error("Create HF: FAILED - Total time taken in milliseconds : " + timeDiffInMillis + "\n\n");
            throw e;
        }

        Date endDate = new Date(currentTimeMillis());
        long timeDiffInMillis = endDate.getTime() - startDate.getTime();

        log.info("Create HF : COMPLETED - Total time taken in milliseconds : " + timeDiffInMillis + "\n\n");

        logWarning();
    }

    private void logWarning()
    {
        log.info("Before delivering/deploying the hotfix, double-check that:");
        log.info("1. The printed log doesn't have any ERROR/WARN records. If any, check/correct the Input file, if required and re-create the HF.");
        log.info("2. The content included in ZIP/JAR is exactly what was intended and nothing more/less/different is packed.");
        log.info("3. The content included is JAR is packed at correct path.");
        log.info("Happy HotFixing!!\n\n");
    }

    private void init()
    {
        this.simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        this.simpleDateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
        this.jarUtilities = new JarUtilities(true);
        this.searchUtilities = new SearchUtilities(true);
        this.moduleJarMap = new HashMap<>();
        this.moduleJarRecordsMap = new HashMap<>();
    }

    private void process(InputFileDTO inputFileDTO)
    {
        inputFileDTO.getInputRecords().stream().filter(this::isValidRecord).forEach(this::processBasedOnInputRecordType);
    }

    private boolean isValidRecord(InputFileRecordDTO inputFileRecordDTO)
    {
        InputRecordType inputRecordType = inputFileRecordDTO.getInputRecordType();
        return !(inputRecordType == BLANK || inputRecordType == ERRONEOUS);
    }

    private void processBasedOnInputRecordType(InputFileRecordDTO inputFileRecordDTO)
    {
        switch (inputFileRecordDTO.getInputRecordType())
        {
            case COMPONENT:
                setComponentPath(inputFileRecordDTO);
                break;
            case MODULE:
                setModulePath(inputFileRecordDTO);
                break;
            case BASEPACKAGE:
                setBasePath(inputFileRecordDTO);
                break;
            case REGULARFILE:
                searchAndPrepareRegularFileForPacking(inputFileRecordDTO);
                break;
            case CLASSFILE:
                searchAndPrepareClassFileForPacking(inputFileRecordDTO);
                break;
            case ZIP:
                setZipPrefixPath(inputFileRecordDTO);
                break;
        }
    }

    private void setComponentPath(InputFileRecordDTO inputFileRecordDTO)
    {
        String componentName = inputFileRecordDTO.getValue();
        int lineCounter = inputFileRecordDTO.getLineCounter();
        InputRecordType inputRecordType = inputFileRecordDTO.getInputRecordType();

        log.debug("Line " + lineCounter + ": " + inputRecordType + ": " + quote(componentName));

        //Search Component Directory by Name, starting from the Current Path (Directory from where the tool is run)
        List<Path> localComponentPath = searchUtilities.search(currentPath, componentName, DIRECTORY);
        //If search result is EMPTY, throw RTException
        if (isEmpty(localComponentPath)) { errorNoPathFound(inputRecordType, componentName, lineCounter, currentPath); }
        //If search returns multiple locations, choose the nearest location (having minimum number of path delimiter /)
        if (localComponentPath.size() > 1) { componentPath = warnMultiplePathsFound(inputRecordType, componentName, lineCounter, localComponentPath); }
        //If search returns exactly ONE result, set it as class level componentPath.
        else if (localComponentPath.size() == 1) { componentPath = informConsideredPath(inputRecordType, componentName, lineCounter, localComponentPath); }
    }


    private void setModulePath(InputFileRecordDTO inputFileRecordDTO)
    {
        String moduleName = inputFileRecordDTO.getValue();
        int lineCounter = inputFileRecordDTO.getLineCounter();
        InputRecordType inputRecordType = inputFileRecordDTO.getInputRecordType();

        log.debug("Line " + lineCounter + ": " + inputRecordType + ": " + quote(moduleName));

        //Search Module Directory by Name, starting from Component Path
        List<Path> localModulePath = searchUtilities.search(componentPath, moduleName, DIRECTORY);
        //If search result is EMPTY, throw RTException
        if (isEmpty(localModulePath)) { errorNoPathFound(inputRecordType, moduleName, lineCounter, componentPath); }
        //If search returns multiple locations, choose the nearest location (having minimum number of path delimiter /)
        if (localModulePath.size() > 1) { modulePath = warnMultiplePathsFound(inputRecordType, moduleName, lineCounter, localModulePath ); }
        //If search returns exactly ONE result, set it as class level modulePath.
        else if (localModulePath.size() == 1) { modulePath = informConsideredPath(inputRecordType, moduleName, lineCounter, localModulePath); }
    }

    private void setBasePath(InputFileRecordDTO inputFileRecordDTO)
    {
        String basePackageName = inputFileRecordDTO.getValue();
        int lineCounter = inputFileRecordDTO.getLineCounter();
        InputRecordType inputRecordType = inputFileRecordDTO.getInputRecordType();

        log.debug("Line " + lineCounter + ": " + inputRecordType + ": " + quote(basePackageName));

        //Search Base Directory by Name, starting from Module Path
        List<Path> localPackagePath = searchUtilities.search(modulePath, basePackageName, DIRECTORY);
        //If search result is EMPTY, throw RTException
        if (isEmpty(localPackagePath)) { errorNoPathFound(inputRecordType, basePackageName, lineCounter, modulePath); }
        //If search returns multiple locations, throw RTException
        if (localPackagePath.size() > 1) { basePath = warnMultiplePathsFound(inputRecordType, basePackageName, lineCounter, localPackagePath); }
        //If search returns exactly ONE result, set it as class level basePath.
        else if (localPackagePath.size() == 1) { basePath = informConsideredPath(inputRecordType, basePackageName, lineCounter, localPackagePath ); }
    }

    private void errorNoPathFound(InputRecordType inputRecordType, String recordName, int lineCounter, Path searchPath)
    {
        String msg = inputRecordType + " " + recordName + " could not be found under " + searchPath;
        log.error("Line " + lineCounter + ": " + inputRecordType + ": " + quote(recordName) + " - Error: " + msg);
        throw new RuntimeException(msg);
    }

    private Path warnMultiplePathsFound(InputRecordType inputRecordType, String recordName, int lineCounter, List<Path> foundPaths)
    {
        log.warn("Line " + lineCounter + ": " + inputRecordType + ": " + quote(recordName) + " - Found at multiple places:");
        foundPaths.forEach(log::warn);
        Path path = foundPaths.stream().sorted(sortInNearestPathOrder()).findFirst().get();
        log.warn("Line " + lineCounter + ": " + inputRecordType + ": " + quote(recordName) + " - Considering nearest path: " + path);
        return path;
    }

    private Path informConsideredPath(InputRecordType inputRecordType, String recordName, int lineCounter, List<Path> foundPaths)
    {
        Path path = foundPaths.get(0);
        log.debug("Line " + lineCounter + ": " + inputRecordType + ": " + quote(recordName) + " - Considering path: " + path);
        return path;
    }

    private Comparator<Path> sortInNearestPathOrder()
    {
        return Comparator.comparingInt(o -> StringUtils.countMatches(StringUtils.replaceChars(o.toString(), "\\", "/"), "/"));
    }

    private void searchAndPrepareClassFileForPacking(InputFileRecordDTO inputFileRecordDTO)
    {
        List<Path> pathList;

        String line = inputFileRecordDTO.getLine();
        line = removeJavaOrClassExtension(line);

        if (contains(line, ".")) { pathList = getPathFromFullyQualifiedName(line, inputFileRecordDTO.getLineCounter()); }
        else { pathList = searchUtilities.search(basePath, line, CLASS_FILE); }

        scheduleForAddingInJar(line, inputFileRecordDTO.getLineCounter(), pathList);
    }

    private String removeJavaOrClassExtension(String line)
    {
        if (endsWithIgnoreCase(line, ".class") || endsWithIgnoreCase(line, ".java")) { line = substringBeforeLast(line, "."); }
        return line;
    }

    private List<Path> getPathFromFullyQualifiedName(String line, int lineCounter)
    {
        line = line.replaceAll("\\.", "/");
        String qualifiedPath = basePath + "/" + substringBeforeLast(line, "/");
        String fileName = substringAfterLast(line, "/");
        File startPath = new File(qualifiedPath);
        if (!startPath.exists())
        {
            log.error("Line " + lineCounter + ": Path doesn't exist : " + qualifiedPath);
            return null;
        }

        return searchUtilities.search(startPath.toPath(), fileName, CLASS_FILE);
    }

    private void searchAndPrepareRegularFileForPacking(InputFileRecordDTO inputFileRecordDTO)
    {
        String line = inputFileRecordDTO.getLine();

        List<Path> pathList = searchUtilities.search(basePath, line, REGULAR_FILE);
        scheduleForAddingInJar(line, inputFileRecordDTO.getLineCounter(), pathList);
    }

    private void scheduleForAddingInJar(String line, int lineCounter, List<Path> pathList)
    {
        if (!isNotEmpty(pathList))
        {
            log.error("Line " + lineCounter + ": File: " + line + " NOT found.");
        }
        else
        {
            log.debug("Line " + lineCounter + ": File: " + line + " Found below records:");
            List<JarRecordDTO> jarRecordDTOS = Optional.ofNullable(moduleJarRecordsMap.get(modulePath)).orElse(new ArrayList<>());

            pathList.forEach(path ->
                             {
                                 JarRecordDTO jarRecordDTO = prepareJarRecord(path);
                                 log.debug(jarRecordDTO);
                                 jarRecordDTOS.add(jarRecordDTO);
                             });

            moduleJarRecordsMap.put(modulePath, jarRecordDTOS);
        }
    }

    private JarRecordDTO prepareJarRecord(Path path)
    {
        JarRecordDTO jarRecordDTO = new JarRecordDTO();
        jarRecordDTO.setSourceFile(path.toFile());
        jarRecordDTO.setFilePathWithinJar(replace(basePath.relativize(path).toString(), "\\", "/"));
        return jarRecordDTO;
    }

    private void setZipPrefixPath(InputFileRecordDTO inputFileRecordDTO) {

        String localZipPrefixPath = inputFileRecordDTO.getValue();

        if (StringUtils.isEmpty(localZipPrefixPath))
            return;

        if (endsWith(localZipPrefixPath, "\\") || endsWith(localZipPrefixPath, "/"))
            this.zipPrefixPath = localZipPrefixPath;
        else
            this.zipPrefixPath = localZipPrefixPath + "/";
    }

    private void packJars() throws IOException
    {
        for (Path localModulePath : moduleJarRecordsMap.keySet())
        {
            List<JarRecordDTO> jarRecordDTOS = moduleJarRecordsMap.get(localModulePath);
            if (isNotEmpty(jarRecordDTOS))
            {
                String jarPath = localModulePath.toString() + "\\" + localModulePath.getFileName() + "_" + simpleDateFormat
                        .format(currentTimeMillis()) + ".jar";
                moduleJarMap.put(localModulePath, jarUtilities.createJar(jarPath, jarRecordDTOS));
            }
        }
    }

    private void printJarPaths()
    {
        if (moduleJarMap.keySet().size() == 0) { return; }

        log.info("Jar files created at:");
        for (Path path : moduleJarMap.keySet())
        { log.info(moduleJarMap.get(path)); }
    }

    private void zipJars()
    {
        List<File> jarFiles = moduleJarMap.keySet().stream().map(path -> moduleJarMap.get(path)).collect(Collectors.toList());

        List<ZipRecordDTO> zipRecordDTOS = new ArrayList<>();
        jarFiles.forEach(jarFile -> zipRecordDTOS.add(new ZipRecordDTO(jarFile, jarFile.getName())));

        if (isEmpty(jarFiles)) { return; }

        String zipPath = currentPath.toString() + "\\Hotfix_" + simpleDateFormat.format(currentTimeMillis()) + ".zip";
        try
        {
            jarUtilities.compressFilesToZip(zipRecordDTOS, zipPath);
        }
        catch (IOException e)
        {
            if (debugMode) { e.printStackTrace(); }
            log.error("Error creating zip file of jars: " + e.getMessage());
        }
        log.info("Zip file of jars created at: " + zipPath);
    }

    private void createSingleZip()
    {

        if(StringUtils.isEmpty(zipPrefixPath))
            return;

        List<ZipRecordDTO> zipRecordDTOS = new ArrayList<>();

        moduleJarRecordsMap.keySet().stream().forEach(path -> zipRecordDTOS.addAll(convertToZipRecordDTOS(moduleJarRecordsMap.get(path), zipPrefixPath)));

        String zipPath = currentPath.toString() + "\\SingleZip_" + simpleDateFormat.format(currentTimeMillis()) + ".zip";

        try
        {
            jarUtilities.compressFilesToZip(zipRecordDTOS, zipPath);
        } catch (IOException e)
        {
            if (debugMode)
                e.printStackTrace();
            log.error("Error creating single zip file : " + e.getMessage());
        }
        log.info("Single Zip file created at: " + zipPath);


    }

    private List<ZipRecordDTO> convertToZipRecordDTOS(List<JarRecordDTO> jarRecordDTOS, String zipPrefixPath)
    {

        List<ZipRecordDTO> zipRecordDTOS = new ArrayList<>();
        jarRecordDTOS.forEach(jarRecordDTO -> zipRecordDTOS.add(new ZipRecordDTO(jarRecordDTO.getSourceFile(), zipPrefixPath+jarRecordDTO.getFilePathWithinJar())));
        return zipRecordDTOS;
    }

    private String quote(String aText)
    {
        String QUOTE = "'";
        return QUOTE + aText + QUOTE;
    }
}
