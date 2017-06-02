package com.rnd.hftool.utilities;

import com.rnd.hftool.application.CreateHF;
import com.rnd.hftool.enums.ArtifactType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.log4j.Logger.getLogger;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public class SearchUtilities
{
    private final boolean debugMode;
    private final static Logger log = getLogger(CreateHF.class);


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
                    return isFileNotContainedInHiddenPath(path) && file.isFile() && !file.isHidden() && (equalsIgnoreCase(fileName, artifactName + ".class") || startsWith(fileName, artifactName + "$")) && endsWith(fileName, ".class");
                };
                break;
            case DIRECTORY:
                biPredicate = (Path path, BasicFileAttributes bfa) -> {
                    File file = path.toFile();
                    return isFileNotContainedInHiddenPath(path) && file.isDirectory() && !file.isHidden() && equalsIgnoreCase(file.getName(), artifactName);
                };
                break;
            case REGULAR_FILE:
                biPredicate = (Path path, BasicFileAttributes bfa) -> {
                    File file = path.toFile();
                    return isFileNotContainedInHiddenPath(path) && file.isFile() && !file.isHidden() && equalsIgnoreCase(file.getName(), artifactName);
                };
                break;
        }

        List<Path> pathList = null;
        try
        {
            pathList = Files.find(startPath, 999, biPredicate, FOLLOW_LINKS).collect(toList());
        }
        catch (IOException e)
        {
            if (debugMode) { e.printStackTrace(); }
            log.error("Search for " + artifactName + " ended with error: " + e.getMessage());
        }
        return pathList;

    }

    private boolean isFileNotContainedInHiddenPath(Path path)
    {
        return !(path.toString().contains("\\.") || path.toString().contains("/."));
    }

}
