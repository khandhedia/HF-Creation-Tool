package com.rnd.hftool.application;

import com.rnd.hftool.dto.InputFileDTO;
import com.rnd.hftool.dto.InputFileRecordDTO;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;

import static com.rnd.hftool.enums.FileTokens.TOKEN_BASE;
import static com.rnd.hftool.enums.FileTokens.TOKEN_COMPONENT;
import static com.rnd.hftool.enums.FileTokens.TOKEN_MODULE;
import static com.rnd.hftool.enums.FileTokens.createToken;
import static com.rnd.hftool.enums.InputRecordType.*;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.log4j.Logger.getLogger;

/**
 * Created by nirk0816 on 6/2/2017.
 */
public class TextInputFileParser
{

    public TextInputFileParser(boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    private int lineCounter;
    private final boolean debugMode;

    private static final String DELIMITER = "=";
    private final static Logger log = getLogger(TextInputFileParser.class);

    public InputFileDTO parseInputFile(File inputFile)
    {
        Date startDate = new Date(currentTimeMillis());
        log.info("Parse Input File : STARTED - Input file " + inputFile.getAbsolutePath());

        this.lineCounter = 0;

        if (!inputFile.exists())
        {
            String message = "Input File " + inputFile + " not accessible. Please check.";
            log.error(message);

            Date endDate = new Date(currentTimeMillis());
            long timeDiffInMillis = endDate.getTime() - startDate.getTime();
            log.info("Parse Input File : FAILED - Total time taken in milliseconds : " + timeDiffInMillis + "\n\n");
            throw new RuntimeException(message);
        }

        InputFileDTO inputFileDTO = new InputFileDTO();
        inputFileDTO.setInputFile(inputFile);
        inputFileDTO.setInputRecords(parseInputFileRecords(inputFile));

        Date endDate = new Date(currentTimeMillis());
        long timeDiffInMillis = endDate.getTime() - startDate.getTime();

        log.info("Parse Input File : COMPLETED - Total time taken in milliseconds : " + timeDiffInMillis);

        return inputFileDTO;
    }

    private LinkedList<InputFileRecordDTO> parseInputFileRecords(File inputFile)
    {
        LinkedList<InputFileRecordDTO> inputFileRecords = new LinkedList<>();
        try
        {
            Scanner scanner = new Scanner(inputFile);
            while (scanner.hasNextLine())
            {
                inputFileRecords.add(processInputFileRecord(scanner.nextLine().trim()));
            }
        }
        catch (IOException e)
        {
            if (debugMode) { e.printStackTrace(); }
            log.error("Parse Input File : FAILED for " + inputFile + "with Error : " + e.getMessage());
        }

        return inputFileRecords;
    }

    private InputFileRecordDTO processInputFileRecord(String line)
    {
        InputFileRecordDTO inputFileRecordDTO = new InputFileRecordDTO();
        inputFileRecordDTO.setLine(line);
        inputFileRecordDTO.setLineCounter(++lineCounter);

        if (isEmpty(line))
        {
            log.warn("Line " + lineCounter + ": Blank. SKIPPED.");
            inputFileRecordDTO.setInputRecordType(BLANK);
        }
        else if (!contains(line, DELIMITER))
        {
            inputFileRecordDTO.setValue(line);

            if (isClassFile(line))
            {
                log.debug("Line " + lineCounter + ": Class File.");
                inputFileRecordDTO.setInputRecordType(CLASSFILE);
            }
            else
            {
                log.debug("Line " + lineCounter + ": Regular File.");
                inputFileRecordDTO.setInputRecordType(REGULARFILE);
            }
        }
        else
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
                log.error("Line " + lineCounter + ": Contains delimiter '" + DELIMITER + "' without any token preceding it. SKIPPED.");
                inputFileRecordDTO.setInputRecordType(ERRONEOUS);
                return inputFileRecordDTO;
            }

            //Extract value
            String value = scanner.hasNext()
                           ? scanner.next().trim()
                           : null;
            if (null == value)
            {
                log.error("Line " + lineCounter + ": Contains delimiter '" + DELIMITER + "' without any value following it. SKIPPED.");
                inputFileRecordDTO.setInputRecordType(ERRONEOUS);
                return inputFileRecordDTO;
            }

            //Act based on token
            switch (createToken(token))
            {
                case TOKEN_COMPONENT:
                    log.debug("Line " + lineCounter + ": Component.");
                    inputFileRecordDTO.setInputRecordType(COMPONENT);
                    break;
                case TOKEN_MODULE:
                    log.debug("Line " + lineCounter + ": Module.");
                    inputFileRecordDTO.setInputRecordType(MODULE);
                    break;
                case TOKEN_BASE:
                    log.debug("Line " + lineCounter + ": Base Package.");
                    inputFileRecordDTO.setInputRecordType(BASEPACKAGE);
                    break;
                case TOKEN_ZIP:
                    log.debug("Line " + lineCounter + ": Zip.");
                    inputFileRecordDTO.setInputRecordType(ZIP);
                    break;
                case UNKNOWN:
                default:
                    log.error(
                            "Line " + lineCounter + ": Token '" + token + "' value is INVALID. Valid values: [" + TOKEN_MODULE + " , " + TOKEN_BASE + " , " + TOKEN_COMPONENT);
                    inputFileRecordDTO.setInputRecordType(ERRONEOUS);
            }

            inputFileRecordDTO.setValue(value);
        }

        return inputFileRecordDTO;
    }


    private boolean isClassFile(String line)
    {
        return endsWithIgnoreCase(line, ".class") || endsWithIgnoreCase(line, ".java");
    }
}
