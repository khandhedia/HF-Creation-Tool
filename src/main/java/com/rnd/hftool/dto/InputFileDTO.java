package com.rnd.hftool.dto;

import java.io.File;
import java.util.LinkedList;

/**
 * Created by nirk0816 on 6/1/2017.
 */
public class InputFileDTO
{
    private File inputFile;

    private LinkedList<InputFileRecordDTO> inputRecords;

    public File getInputFile()
    {
        return inputFile;
    }

    public void setInputFile(File inputFile)
    {
        this.inputFile = inputFile;
    }

    public LinkedList<InputFileRecordDTO> getInputRecords()
    {
        return inputRecords;
    }

    public void setInputRecords(LinkedList<InputFileRecordDTO> inputRecords)
    {
        this.inputRecords = inputRecords;
    }
}
