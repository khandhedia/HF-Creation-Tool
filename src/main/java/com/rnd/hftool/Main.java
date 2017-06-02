package com.rnd.hftool;

import com.rnd.hftool.application.CreateHF;
import com.rnd.hftool.application.TextInputFileParser;
import com.rnd.hftool.dto.InputFileDTO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public class Main
{
    public static void main(String[] args) throws IOException
    {
        try
        {
            if (args.length < 1) { throw new RuntimeException("Please provide input file with module and file name information as the first argument."); }

            Path currentRelativePath = Paths.get(trimToEmpty(getProperty("current.path")));
            String currentPath = currentRelativePath.toAbsolutePath().toString();
            System.out.println("Current Path for HF Tool : " + currentPath);

            if (isEmpty(getProperty("log.level"))) { System.setProperty("log.level", "INFO"); }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            simpleDateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
            System.setProperty("logfile.name", currentPath + "/hftool_" + simpleDateFormat.format(currentTimeMillis()) + ".log");


            TextInputFileParser textInputFileParser = new TextInputFileParser(true);
            InputFileDTO inputFileDTO = textInputFileParser.parseInputFile(new File(args[0]));

            CreateHF createHF = new CreateHF(true, new File(currentPath).toPath());
            createHF.createHF(inputFileDTO);
        }
        catch (RuntimeException e)
        {
            String message = "Exception Occurred: " + e.getMessage() + "\n\n";
            System.out.println(message);
        }
    }
}
