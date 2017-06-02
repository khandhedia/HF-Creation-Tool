ReadMe.txt


===================================================================================================
                       Read this guide to use HotFix tool effectively.
===================================================================================================

******************** This utility should have been delivered as a zip file. ************************

Contents:
1. Executable Jar - "hftool.jar"
2. Sample input file - "inputfile.txt"
3. lib - Supporting libraries for the tool
4. .bat file to run the hotfix tool.

*************************************** How to use? ************************************************

1. Extract the Zip file contents in a directory; which is parent to the base package of the files to be packed.
2. Configure the inputfile.txt to suit your requirements. Read 'Input File Configuration Guide' section below.
3. Run hftool.bat.
4. A ZIP file containing all the module specific jars will be created in the same directory where HF Tool is placed.
5. An hftool_xxx.log will be created in the same directory where HF Tool is placed.

********************************* Additional customizations ****************************************

1. By default, the utility is running with INFO logging level.
   To change the logging mode, edit the .bat file and provide System Property -Dlog.level.
   Example, -Dlog.level=DEBUG, -Dlog.level=ERROR

2. By default, the input file is expected to be present in same directory where the jar file resides, and has name "inputfile.txt".
   To use the input file stored at some different location, please provide the fully qualified path of the file in Unix Format (using / delimiter) or Windows Format (using \\ delimiter)  in the .bat file.

************************************ Input File Configuration Guide ********************************

1. This utility can be placed at any directory level above the base path of the file to be packed.
    Example, to pack com.rnd.too.hftool.CreateHF.class, the utility needs to be placed in any parent directory of 'com'.

2. As close the utility is placed to the base package of the file to be packed,
    lesser is the content to be put into the inputfile.txt, as well as,
    lesser is the time to prepare the hotfix.

3. inputfile can use following tokens: component, module or base.

    Example,

    component=component_directory
    module=module_directory
    base=base_directory

    These tokens help setting the search scope for the files to be packed.

4. inputfile contains individual information on every line, and is processed from line 1 onwards.

5. The delimiter for the lines containing token must be "=".

6. All the files to be included in the jar must NOT contain any token. They must be suffixed with its extension.

        Java class files can be provided with .java or .class extension (case-insensitive).

7. Inner classes of the Java class files will be included automatically along with outer class.

8. The jar file will be created in directory specified as 'module', and will be named as 'module'_xxxx.jar.
    In case, when 'module' directory is not specified, it will internally consider 'module'=Directory from where HFTool is triggered.

    This would mean that number of jars created would be same as number of 'module' specified in the inputfile.

9. The zip file containing all created jars will be placed in the same directory from where HFTool is triggered.

10. 'base' refers to the exact parent directory of the base package of the class/file to be packed.

11. To provide more flexibility, the tool can be placed at any physical location and the "Location of HF Tool:" can be passed as the system property in .bat file.
  -Dcurrent.path=The fully qualified path of the file in Unix Format (using / delimiter) or Windows Format (using \\ delimiter)
  If this system property is not specified, the current path is considered as the location of the physical location of the tool itself.

************************************* Example configuration ****************************************

Directory Structure:

ParentDirectory:
    Directory1:
        Directory11:
            src
                main
                    java
                        com.rnd.too.hftool.CreateHF.java
                    resources
                        log4j.properties
            target
                classes
                    com.rnd.too.hftool.CreateHF.class
                generated-sources
                    com.rnd.too.hftool.CreateHF.java
                lib
                    hf.jar
        Directory12:
            src
                main
                    java
                        com.rnd.too.hftool.CreateJar.java
                    resources
                        jar.properties
            target
                classes
                    com.rnd.too.hftool.CreateJar.class
                generated-sources
                    com.rnd.too.hftool.CreateJar.java
                lib
                    createjar.jar
    Directory2:
        Directory21:
            src
                main
                    java
                        com.rnd.too.hftool.SearchUtilities.java
                        com.rnd.too.hftool.NewSearchUtilities.java
                    resources
                        search.properties
                        example.properties
            target
                classes
                    com.rnd.too.hftool.SearchUtilities.class
                    com.rnd.too.hftool.NewSearchUtilities.class
                generated-sources
                    com.rnd.too.hftool.SearchUtilities.java
                    com.rnd.too.hftool.NewSearchUtilities.java
                lib
                    searchutils.jar


1. Packing all the class files and properties files listed above.
=================================================================

Location of HF Tool: ParentDirectory.

inputfile.txt contents:

    component=Directory1
    module=Directory11
    base=resources
    log4j.properties
    base=classes
    CreateHF.class (or CreateHF.java or com.rnd.too.hftool.CreateHF.class or com.rnd.too.hftool.CreateHF.java)
    module=Directory12
    base=resources
    jar.properties
    base=classes
    CreateJar.class
    component=Directory2
    module=Directory21
    base=resources
    search.properties
    example.properties
    base=classes
    SearchUtilities.class
    NewSearchUtilities.class


2. Packing only the class files and properties file listed in Directory21.
==========================================================================

Option 1:
---------

Location of HF Tool: ParentDirectory.

inputfile.txt contents:

    component=Directory2
    module=Directory21
    base=resources
    search.properties
    example.properties
    base=classes
    SearchUtilities.class
    NewSearchUtilities.class

Option 2:
---------

Location of HF Tool: Directory2

inputfile.txt contents:

    module=Directory21
    base=resources
    search.properties
    example.properties
    base=classes
    SearchUtilities.class
    NewSearchUtilities.class

3. Packing only class files listed in Directory11
=================================================

Option 1:
---------

Location of HF Tool: Directory 1

inputfile.txt contents:

    component=Directory1
    module=Directory11
    base=classes
    CreateHF.class (or CreateHF.java or com.rnd.too.hftool.CreateHF.class or com.rnd.too.hftool.CreateHF.java)

Option 2:
---------

Location of HF Tool: Directory 11

inputfile.txt contents:

    module=Directory11
    base=classes
    CreateHF.class (or CreateHF.java or com.rnd.too.hftool.CreateHF.class or com.rnd.too.hftool.CreateHF.java)

Option 3:
---------

Location of HF Tool: Directory11 > target > classes.

inputfile.txt contents:

    CreateHF.class (or CreateHF.java or com.rnd.too.hftool.CreateHF.class or com.rnd.too.hftool.CreateHF.java)

********************************************** Contact *********************************************

 For any assistance using this utility, contact Nirav Khandhedia @ nirav.khandhedia@netcracker.com

********************************************* Happy HotFixing! *************************************