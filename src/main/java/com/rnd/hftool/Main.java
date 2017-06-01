package com.rnd.hftool;

import com.rnd.hftool.application.CreateHF;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.lang.System.currentTimeMillis;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public class Main
{
    public static void main(String[] args) throws IOException
    {
        Path currentRelativePath = Paths.get("");
        String currentPath = currentRelativePath.toAbsolutePath().toString();

        if(args.length < 1)
            throw new RuntimeException("Please provide input file with module and file name information as the first argument.");

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        simpleDateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
        System.setProperty("logfile.name", currentPath + "/hftool_" + simpleDateFormat.format(currentTimeMillis()) + ".log");

        CreateHF createHF = new CreateHF(true);
        createHF.createHF(new File(currentPath).toPath(), new File(args[0]));
    }
}
