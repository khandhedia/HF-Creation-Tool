package com.rnd.hftool.utilities;

import com.rnd.hftool.dto.JarRecordDTO;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static org.apache.commons.lang3.StringUtils.replaceChars;
import static org.apache.log4j.Logger.getLogger;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public class JarUtilities
{

    private final boolean debugMode;

    public JarUtilities(boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    private final static Logger log = getLogger(JarUtilities.class);

    public File createJar(String jarPath, List<JarRecordDTO> jarRecordDTOS) throws IOException
    {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(MANIFEST_VERSION, "1.0");

        File jarFile = new File(jarPath);
        FileOutputStream fileOutputStream = new FileOutputStream(jarFile);
        try (JarOutputStream jar = new JarOutputStream(fileOutputStream, manifest))
        {
            addMultipleFilesToJar(jarRecordDTOS, jar);
        }

        return jarFile;
    }

    private void addMultipleFilesToJar(List<JarRecordDTO> jarRecordDTOS, JarOutputStream jar)
    {
        jarRecordDTOS.forEach(jarRecordDTO ->
                              {
                                  try
                                  {
                                      addSingleFileToJar(jar, jarRecordDTO);
                                  }
                                  catch (IOException e)
                                  {
                                      if (debugMode) { e.printStackTrace(); }
                                      log.error("Error adding " + jarRecordDTO + " in jar: " + e.getMessage());
                                  }
                              });
    }

    private void addSingleFileToJar(JarOutputStream jar, JarRecordDTO jarRecordDTO) throws IOException
    {

        File sourceFile = jarRecordDTO.getSourceFile();
        String filePathWithinJar = jarRecordDTO.getFilePathWithinJar().replace("\\","/");
        filePathWithinJar = replaceChars( filePathWithinJar, "\\", "/");

        BufferedInputStream in = null;
        try
        {
            if (sourceFile.isDirectory()) { return; }

            JarEntry entry = new JarEntry(filePathWithinJar);
            entry.setTime(sourceFile.lastModified());
            jar.putNextEntry(entry);
            FileInputStream sourceInputStream = new FileInputStream(sourceFile);
            in = new BufferedInputStream(sourceInputStream);

            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1) { break; }
                jar.write(buffer, 0, count);
            }
            jar.closeEntry();
        }
        finally
        {
            if (in != null) { in.close(); }
        }
    }

    public void compressFilesToZip(List<String> filesList, String zipFilePath) throws IOException
    {
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);

        filesList.forEach(file -> {
            try
            {
                File srcFile = new File(file);
                FileInputStream fis = new FileInputStream(srcFile);

                // begin writing a new ZIP entry, positions the stream to the start of the entry data
                zos.putNextEntry(new ZipEntry(srcFile.getName()));

                int length;

                while ((length = fis.read(buffer)) > 0) { zos.write(buffer, 0, length); }

                zos.closeEntry();

                // close the InputStream
                fis.close();
            }
            catch (IOException e)
            {
                if (debugMode) { e.printStackTrace(); }
                log.error("Error creating zip file : " + e.getMessage());
            }
        });

        zos.close();
    }
}
