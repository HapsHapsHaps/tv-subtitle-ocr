# OCR Processing
This module processes each input frame with the Tesseract 4 OCR processor.
Contains an interface and a factory which allows for greater abstraction between implementation of OCR engines.
The `IOCRProcessor.java` is an interface which merely states that any OCR engine implementation should have the following method:
```java
List<String> ocrImage(BufferedImage image) throws IOException;
```
This module has been designed with Tesseract 4 in mind, and is currently the only implementing OCR engine class.
However, it does allow for different interpretations of OCR engines, as long as the implementing class of `IOCRProcessor.java` satisfies the ocrImage method.

## Getting started
This module depends currently on [Tesseract 4](https://github.com/tesseract-ocr/tesseract/tree/4.00.00alpha), which can be interchanged with different versions or OCR engines.
This module is supposed to be executed from the [dk.hapshapshaps.classifier.objectdetection.Main module](../main/README.md) in the applications context.
However, some manual tests are implemented, where you may execute this module manually.  


### Dependencies
This module depends on a compiled and installed version of Tesseract 4.
This module were also designed to allow for different interpretations of OCR engines, thus Tesseract 4 can be interchanged with other OCR engines.
To compile Tesseract 4, see [Compiling](https://github.com/tesseract-ocr/tesseract/wiki/Compiling) for dependencies on tesseract and [Compiling – GitInstallation](https://github.com/tesseract-ocr/tesseract/wiki/Compiling-%E2%80%93-GitInstallation)
 

## Running the tests
There are implemented some tests for testing the tesseract installation.
These can be found under [OCRTextRecognitionTest.java](src/test/java/dk/kb/tvsubtitle/ocrprocessing/OCRTextRecognitionTest.java).
Currently, this class only contains one test, which asserts if we can extract text from one specific installation.


## Building, Authors and License
See [Parent README](../README.md)

Last updated: 2018-03-26T10:19