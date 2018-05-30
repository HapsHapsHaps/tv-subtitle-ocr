### dk.hapshapshaps.classifier.objectdetection.Main
This is the module where all the programs main methods are located and thus the point of entry to the program.

## About
This module consist of three packages.
 * dk.kb.subtitle.main
   * externalservice
   * mainprocessor
   * model


#### mainprocessor
This is the package which contains all the main methods, and all the points of entry to execute the program
There are five ways to run the program. 
Two of which takes a text file containing UUIDs of videos which should be processed.
One of which puts the extracted subtitle into the Doms service, and the other saves the extracted output to an SRT file on disk.  
Three of which, acts upon an index (which in this scenario is an Apache Solr service). 
One processes the input UUIDs to srt files, another processes them to Doms, and the last one searches the index for UUIDs with an existing SRT file allocated and saves the UUIDs to a file.
The last case, is there to get information about which videos already have an SRT file, but with future updates to the program, may need to be processed and extract subtitles again.

#### externalservice
This package contains classes and functions to integrate this project into Solr and Doms.
It contains three classes which are responsible for:

Class | Responsibility 
--- | ---
  [DomsClient](src/main/java/dk/kb/tvsubtitleocr/extractor/extractor/externalservice/DomsClient.java) | Responsible for updating DOMS with new information.
  [SrtDomsClient](src/main/java/dk/kb/tvsubtitleocr/extractor/extractor/externalservice/SrtDomsClient.java) | Extension of DomsClient to upload Srt files to DOMS 
  [VideoIndexClient](src/main/java/dk/kb/tvsubtitleocr/extractor/extractor/externalservice/VideoIndexClient.java) | Responsible for searching in Apache Solr
 

#### dk.kb.subtitle.main
Contains three classes which are responsible to process a video throughout the program loop.  

Class | Responsibility 
--- | ---
  [FramePreProcessor](src/main/java/dk/kb/tvsubtitleocr/extractor/FramePreProcessor.java) | Preparing frame for OCR-analysis 
  [VideoMassProcessor](src/main/java/dk/kb/tvsubtitleocr/extractor/extractor/VideoMassProcessor.java) | Spawning and preparing multiple Video Processors
  [VideoProcessor](src/main/java/dk/kb/tvsubtitleocr/extractor/VideoProcessor.java) | Extracting frames, preparing them for OCR and running OCR with given Thread Count

## Configuration
### Testing
When testing the project, one need to set up the config.properties in the [test-resources](../test-resources) module. A sample [config.properties.SAMPLE](../test-resources/src/main/resources/config.properties.SAMPLE) has been provided, just fill in all the variables and rename it to `config.properties`.
If you want to manually test the application, you can head over to [MainTest.java](src/test/java/dk/kb/tvsubtitle/main/MainTest.java) and manually execute `runVideoProcessor()`.
Remember to change `File video = new File("/some/path");` to a video file that is located somewhere on your system.
This will test the VideoProcessor class, with all its sub components.


### Executing

## Usage
When the project has been built with `mvn package` from the top level package. 

## Building, Authors and License
See [Parent README](../README.md)

Last updated: 2018-03-26T10:19