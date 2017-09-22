**Dir cleanup

***Objective:

This program sets out to delete files from  directory 
based on specified criteria.

***Running:

The program can be invoked as follows,
```java
java -jar ftpcleanup-0.1.1.jar
```
This program takes two main option groups.

+ Delete:
    
    This feature requires following options,
    + `-d` identifies the operation as delete
    + `-dd` directory from which to delete files
    + `-sd` start date for files
    + `-ed` end date for files
    
    These options will delete all files matching the criteria.
    Specifically where the file date extracted from its name, is
    within the range specified by `-sd` and `-ed` *inclusive*
    
+ Generate:

    This feature generates sample files for testing and
    requires following options,
    + `-g` identifies the operation as generate
    + `-c` count of files to generate
    + `-dt` date to add to file name
    + `-tgt` target directory to store files in
    + `-src` source directory to take a sample file from
    
   
