# Subtitle Extraction

This project is created in collaboration with The Royal Library of Denmark, and is about extraction of subtitles from tv program broadcast video material stored in [MedieSteam](http://www2.statsbiblioteket.dk/mediestream/).

## About

The program has a multitude of different ways to be executed. 
One way that it can operate, is that it can get a video file uuid from an index(Apache Solr), and extract frames.

These frames are then pre-processed by merging and manipulating them for a better result when finding contours in the frame. 
The estimated text contours are analysed for presumed subtitles. 
When the subtitle is estimated, the image is adjusted and cropped to the best fitting size. 

This leaves the best possible chances for the OCR-process to read the subtitles correctly.

The OCR-proces leaves a text result, which needs to be post-processed for noise and wild characters.

When the OCR-proces finshed reading all the subtitles and the text is post-processed, a SRT file is generated. 
This SRT is sent to DOMS, ready to use or further processed.

For further information about the workflow in the individual modules, 
we will refer to the _readme_ in each module.

#### Module overview
 * [Common](common/README.md) General toolbox, used throughout the system
 * [Frame Extraction](frame-extraction/README.md) to extract frames from a video
 * [Frame Processing](frame-processing/README.md) to pre process frames before they go into the OCR engine
 * [Main](main/README.md) to run the program
 * [OCR processing](ocr-processing/README.md) to run OCR analysis on the frames extracted and processed
 * [Output](output/README.md) SRT generation
 * [Test Resources](test-resources/README.md) when testing the program, configs and other test resources are loaded from here.
 * [Text Processing](text-processing/README.md) Post processing of text, to be written in SRT files. (Deletes trash characters)


## Getting started
### Prerequisites

_What_ do you need to install for this program to work and _how_ to install them.
 
#### Dependencies

|Dependency|Tested with version|
|---|---|
|Java 8|
|Tesseract 4|
|FFmpeg 3.4|


#### Installing dependencies

Step by step installation guide based on the dependencies for the project.

#### Tesseract 4
Tesseract can be installed by the following guide-links:

[Tesseract Installing](https://github.com/tesseract-ocr/tesseract/wiki)  

[Tesseract Compiling](https://github.com/tesseract-ocr/tesseract/wiki/Compiling)  

##### Setting up Tesseract
Tesseract is using a few different files, that we need to ensure are being set up:

#### TESSDATA

Tessdata needs to be set with the enviroment variable: _"TESSDATA_PREFIX"_ (optional: can be overwritten thorugh the config file.)  
Example for Linux, run in terminal or add to .bashrc:
```
export TESSDATA_PREFIX=/PATH/TO/TESSDATA/
```
Within the folder there must be a language file, matching the language name defined in this applications config file.
The data files will probably have a name formatted as follows: `LANGUAGE.traineddata` where `LANGUAGE` is the alpha 3 code for a language.  
Example for danish in tessdata directory    : `dan.traineddata`  
Example for danish in config.properties     : `dan`  
Pre trained language data can be found on [tesseracts official github](https://github.com/tesseract-ocr/tessdata)

**LD_LIBRARY_PATH** 

In some instances, LD_LIBRARY_PATH might not have been defined in the system.

It can, depending on the system, be set as following:

Create config file at path: _"/etc/ld.so.conf.d/<config name>.conf"_.

The content of file will be: _"/usr/local/lib"_.

Then save and run the following command to have it take affect:
```
sudo ldconfig
```
#### FFmpeg
FFmpeg needs to be installed for the program to work as well. Most linux distributions got it preinstalled.
FFmpeg can be acquired from the following link:

[FFmpeg](https://www.ffmpeg.org/)

Most of the packages contains both _ffmpeg_ and _ffprobe_. The program need both of these to be installed to run.

FFmpeg standard path:  
_"/usr/bin/ffmpeg"_

FFprobe standard path:  
_"/usr/bin/ffmprobe"_

Both of these paths need to set in the programs config file.

#### Running the program
There are a number of different ways to run the program.
But first and foremost, it needs to be packaged into the final executable program with `mvn package` in the root module.
This allows the program to be executed outside a development environment. 

This  will give the file _"main/target/main-xxxx-package.tar.gz"_, which will include a production config in its config directory, that will need to have it's settings defined.  
When the packaging is done, the program can be executed.
There are are multiple ways to execute the program inside the `main/target/main-XXX/bin` directory.

| Shell file                   | Purpose           | Arguments | 
|------------------------------|-------------------|---------- |
| runProcessFileListToDoms.sh  | Takes a list of UUIDs and processes them to SRT files on Doms | **-in**: Input file with UUIDs **-f**: Force override of SRT, if a subtitle is already set |
| runProcessFileListToFiles.sh | Takes a list of UUIDs and processes them to SRT files on disk | **-in**: Input file with UUIDs **-f**: Force override of SRT, if a subtitle is already set |
| runProcessIndexToDoms.sh     | Processes videos from Solr Index. Processes 50 videos at a time. If no argument is set, processes all videos. Output is put on Doms| **-m**: Optional - Max amount of files to process, will run in till this is met |
| runProcessIndexToFileList.sh | Fetches videos with the SRT flag set. If no max is set, fetches all videos | **-out**: Output file, where to write the files with SRT file. **-m**: Max amount of UUIDs to fetch |
| runProcessIndexToFiles.sh    | Processes videos from Solr Index. Processes 50 videos at a time. If no argument is set, processes all videos. Output is put in disk | **-m**: Optional - Max amount of files to process, will run in till this is met | 




### Developing the program

The dependencies are now done and before the program is ready to be worked on and tested, we need to configure it. 
This is done in `test-resources/src/main/resources/config.properties`, that should be created based on the `config.properties.SAMPLE` file.

`main/src/main/config/config.properties`, that should be created based on the `config.properties.SAMPLE` file.

If there is a need to experiment and test the program, remember to copy the `config.properties` files to
`test-resources/src/main/resources/config.properties`.

#### config.properties explanation
As of 2018-03-23, the `config.properties` look like this:

|Property           | Function      |Required   |
|-------------------|---------------|---------  |
| sharedWorkDir    | Path to directory where the program can put temporary and output files | yes |
| videoSourceDir   | Input directory for videos with UUIDs (as on Doms) | yes|
| workerThreads=2   | How many threads the program should run with | yes|
| debug=false   |If the program should run in debug mode or not|yes|
|   |  |                                        |
| # FrameExtraction |  |                        |
| ffmpegPath=/usr/bin/ffmpeg| Path to ffmpeg | yes |
| ffprobePath=/usr/bin/ffprobe| Path to ffprobe| yes |
|   |  |                                        |
| # OCRProcessorFactory |  |                    |
| tesseractDataFolderPath  | Path to tessdata directory. Not required if [TESSDATA](#TESSDATA) is set | no |
| tesseractPageSegmentation=6 | [See commandline usage](https://github.com/tesseract-ocr/tesseract/wiki/Command-Line-Usage#tesseract---help)  | yes |
| tesseractOCREngineMode=3  | [See commandline usage](https://github.com/tesseract-ocr/tesseract/wiki/Command-Line-Usage#tesseract---help) | yes |
| tesseractTrainedDataLanguage=dan  | Trained language data| yes|
|   |  |                                        |
| Solr - Index    |  ||
| indexServerUrl=   | Solr URL for indexing| yes |
|   | |                                         |
| Fedora Repository - Doms    | |             |
| domsServerAddress    | URL address for Doms Repository| yes |
| domsUserName | Username for Doms repository| yes |
| domsPassword | Password for Doms repository| yes |




#### VisualVM
When using VisualVM to debug the application, remember to add the following parameter as Java VM options when launching this program:
```
-Xverify:none
```

## Authors

**_Programmers:_**
* Andreas Reng Mogensen
* Jacob Pedersen
* Silas Jeppe Christensen

**_Supervisors associated with the Royal Danish Library - Aarhus department:_**
* Kim Teglgaard Christensen
* Kåre Fiedler Christiansen
* Asger Askov Blekinge

Last updated: 2018-03-26T10:19