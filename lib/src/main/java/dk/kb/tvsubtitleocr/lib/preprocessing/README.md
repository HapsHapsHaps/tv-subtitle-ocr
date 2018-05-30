# Frame Processing

The Frame Processing modules purpose is to find and extract the subtitle from a given frame.
This is done by firstly converting the frame to a gray scaled image. 
Then running the gray scaled frame through a Morphology edge detection algorithm, and then converting the frame to black and white by Thresholding.
This filters everything besides the edges of the text, and other noise in the image.

Another feature of the Frame Processing Module is to generate a RectangularData class, which main purpose is to contain data about the extracted contours.
Because not all contours are relevant, and because this module is run mainly from other modules, it is relevant to check which data is associated with a given frame.
See [Merge Frames in FramePreProcessor.java](../main/src/main/java/dk/kb/tvsubtitle/main/FramePreProcessor.java) for the implementing class, or [Documentation for dk.hapshapshaps.classifier.objectdetection.Main Module](../main/README.md).

The three main classes in this module is:
 * FrameProcessorOpenCV.java
 * RectangularData.java  
 * MergeImages.java

FrameProcessorOpenCV's responsibility is to find contours in a given frame, to detect where about a possible subtitle is.
Then return a RectangularData class to be calculated upon.
MergeImages contains two static methods.
The first one merges two or more images together, by taking the average pixel value on the same position, and returning a new image with all the averages.
This is useful when trying to process the image with an OCR engine, to blur out anything besides the subtitle itself.
It also improves the subtitle extraction method, which extracts a snippet of the subtitle.

## Getting Started

This module contains no dk.hapshapshaps.classifier.objectdetection.Main methods, because it is intended to be run as a library from another module, as a result it can not be run directly. 
However, when testing this module, a dk.hapshapshaps.classifier.objectdetection.Main method in the test directory were created, see [TestRun.java](src/test/java/dk/kb/tvsubtitle/frameprocessing/TestRun.java) to run module for itself. 

### Dependencies

This module uses JavaCV as its only outside dependency. 

```xml
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacv-platform</artifactId>
    <version>1.4</version>
</dependency>
```

## Running the tests

When processing Images in different ways, it is hard to have a test case, that you want to verify.
Therefor, all testing in this module has been done manually, by writing images to the disk, to see if the output matches somewhat the desired goal.
These tests can be found in [TestRun.java](src/test/java/dk/kb/tvsubtitle/frameprocessing/TestRun.java), 
[TestRunOpenCV.java](src/test/java/dk/kb/tvsubtitle/frameprocessing/TestRunOpenCV.java).
They are manual tests, which are meant to be run from the given main method inside these classes.


## Building, Authors and License
See [Parent README](../README.md)

Last updated: 2018-03-26T10:19
