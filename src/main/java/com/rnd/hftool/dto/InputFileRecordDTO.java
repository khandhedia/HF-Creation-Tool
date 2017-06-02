package com.rnd.hftool.dto;

import com.rnd.hftool.enums.InputRecordType;

/**
 * Created by nirk0816 on 6/1/2017.
 */
public class InputFileRecordDTO
{
    private int lineCounter;

    private String line;

    private InputRecordType inputRecordType;

    private String value;

    public int getLineCounter()
    {
        return lineCounter;
    }

    public void setLineCounter(int lineCounter)
    {
        this.lineCounter = lineCounter;
    }

    public String getLine()
    {
        return line;
    }

    public void setLine(String line)
    {
        this.line = line;
    }

    public InputRecordType getInputRecordType()
    {
        return inputRecordType;
    }

    public void setInputRecordType(InputRecordType inputRecordType)
    {
        this.inputRecordType = inputRecordType;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("InputFileRecordDTO{");
        sb.append("lineCounter=").append(lineCounter);
        sb.append(", line='").append(line).append('\'');
        sb.append(", inputRecordType=").append(inputRecordType);
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
