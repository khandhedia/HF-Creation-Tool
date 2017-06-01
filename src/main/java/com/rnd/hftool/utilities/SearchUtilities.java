package com.rnd.hftool.utilities;

import com.rnd.hftool.application.CreateHF;
import com.rnd.hftool.enums.ArtifactType;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public class SearchUtilities
{
    private boolean debugMode;
    private final static Logger log = Logger.getLogger(CreateHF.class);


    public SearchUtilities(boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    public List<Path> search(Path startPath, String artifactName, ArtifactType artifactType)
    {
        BiPredicate<Path, BasicFileAttributes> biPredicate = null;

        switch (artifactType)
        {
            case CLASS_FILE:
                biPredicate = (Path path, BasicFileAttributes bfa) -> {
                    File file = path.toFile();
                    String fileName = file.getName();
                    return !isFileContainedInHiddenPath(path) && file.isFile() && !file.isHidden() && (StringUtils
                            .equalsIgnoreCase(fileName, artifactName + ".class") || StringUtils.startsWith(fileName, artifactName + "$")) && StringUtils
                            .endsWith(fileName, ".class");
                };
                break;
            case DIRECTORY:
                biPredicate = (Path path, BasicFileAttributes bfa) -> {
                    File file = path.toFile();
                    return !isFileContainedInHiddenPath(path) && file.isDirectory() && !file.isHidden() && StringUtils.equals(file.getName(), artifactName);
                };
                break;
            case REGULAR_FILE:
                biPredicate = (Path path, BasicFileAttributes bfa) -> {
                    File file = path.toFile();
                    return !isFileContainedInHiddenPath(path) && file.isFile() && !file.isHidden() && StringUtils.equals(file.getName(), artifactName);
                };
                break;
        }

        List<Path> pathList = null;
        try
        {
            pathList = Files.find(startPath, 999, biPredicate, FileVisitOption.FOLLOW_LINKS).collect(Collectors.toList());
        }
        catch (IOException e)
        {
            if (debugMode) { e.printStackTrace(); }
            log.error("Search for " + artifactName + " ended with error: " + e.getMessage());
        }
        return pathList;

    }

    private boolean isFileContainedInHiddenPath(Path path)
    {
        return path.toString().contains("\\.") || path.toString().contains("/.");
    }

}
