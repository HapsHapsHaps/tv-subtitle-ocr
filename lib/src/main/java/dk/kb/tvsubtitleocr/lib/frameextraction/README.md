# Frame Extraction

This module is designed to extract frames from a given video input and save them into a VideoInformation class which contains a list of all the extracted frames, the UUID of the video (if applicable) along with the duration of the video in milliseconds.
The VideoFrame contains the actual extracted frame, along with a given start and end time for that specific frame.

The main method in this class is:
 * FrameExtractionProcessor.java
 
Which is responsible for extracting the frames and loading them into respectively a single VideoInformation and multiple VideoFrame classes per video. 

## Getting Started

This module has dependencies on ffmpeg and ffprobe.
These are required to be installed before this module can be run.
The path to the executable is required to be set in the [config.properties](). 
These settings would need to be set, however these are the default settings:
```properties
#FrameExtraction
ffmpegPath=/usr/bin/ffmpeg
ffprobePath=/usr/bin/ffprobe

```

### Dependencies

There are two dependencies, which respectively are ffmpeg and ffprobe. 
For ffmpeg installation, head over to [ffmpeg.org](https://www.ffmpeg.org/download.html)

## Running the tests

Currently, there are no tests written for this module.
If requires to test this module, a new test class can be written with the following code:
```java
@Test
void getVideoInformation() {
    //Arrange
    VideoInformation returnInformation;
    IFrameExtractionProcessor frameExtraction = new FrameExtractionProcessor();
    //Act
    returnInformation = frameExtraction.extractFrames(new File("PathToVideo"));
}
```
returnInformation in this scenario will contain the VideoInformation and VideoFrames extracted.

## Building, Authors and License
See [Parent README](../README.md)

Last updated: 2018-03-26T10:19