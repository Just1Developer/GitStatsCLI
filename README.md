# The Git Stats CLI

## 1. Setup

You may need to set up some configuration stuff. The Config file in `src/main/java/net/justonedev/Config.java`
contains a method `String getAlias(String _)`. This takes in a String and returns the actual name of the author.
This is useful when you have the same author, but different names because of different PCs / Platforms, etc.\
Simply adjust the switch to map all aliases to your preferred name.

If your `/src/` folder is nested and not at the root or one layer down, you need to configure the `SRC_FOLDER_MAX_DEPTH` value in the Config.java file.

## 2. Running the Program

Running the program is really simple. You start it, and then enter each repositories' filepath that you wish to include in the statistics. When you are done, press enter, effectively adding an empty repository.

### Repository Path Formatting

The rules have changed a little bit: You can now add **multiple projects in one line**. This also means that if you have a space in your path, you need to put the path in quotes, like `"/Users/my user/some folder/"`

## 3. Output

The program outputs a `repository-stats.xlsx` file, switching to `repository-stats-N.xlsx` if that already exists, where N goes from 1 to the integer limit. If you have more than 4.3 Billion files, there are other issues at play.\
The Excel Spreadsheet has one sheet per repository, and for more than 1 repository generates an "All Projects" sheet where all data from all projects is combined.

# Notes and third party disclosure

## Execution speed

Sorry, it might be slow for really large projects. I used it for my projects, totalling 16k lines, and it took only a few seconds.

## Third Party

This project uses Apache POI to generate Excel files, which can be found here: https://mvnrepository.com/artifact/org.apache.poi/poi