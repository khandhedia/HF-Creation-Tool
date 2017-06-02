package com.rnd.hftool.dto;

import java.io.File;

/**
 * Created by nirk0816 on 5/29/2017.
 */
public class JarRecordDTO
{
    private File sourceFile;

    private String filePathWithinJar;

    public File getSourceFile()
    {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile)
    {
        this.sourceFile = sourceFile;
    }

    public String getFilePathWithinJar()
    {
        return filePathWithinJar;
    }

    public void setFilePathWithinJar(String filePathWithinJar)
    {
        this.filePathWithinJar = filePathWithinJar;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("JarRecordDTO{");
        sb.append("sourceFile=").append(sourceFile);
        sb.append(", filePathWithinJar='").append(filePathWithinJar).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
