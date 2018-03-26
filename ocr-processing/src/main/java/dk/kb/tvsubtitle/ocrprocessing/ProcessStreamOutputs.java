package dk.kb.tvsubtitle.ocrprocessing;
import java.io.BufferedReader;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessStreamOutputs {
    private List<String> standardOutput;
    private List<String> errorInput;
    private final int returnCode;

    public ProcessStreamOutputs(List<String> standardOutput, List<String> errorInput, int returnCode) {
        this.standardOutput = standardOutput;
        this.errorInput = errorInput;
        this.returnCode = returnCode;
    }

    ProcessStreamOutputs(BufferedReader standardOutput, BufferedReader errorInput, int returnCode) {
        this.standardOutput = standardOutput.lines().collect(Collectors.toList());
        this.errorInput = errorInput.lines().collect(Collectors.toList());
        this.returnCode = returnCode;
    }

    public List<String> getStandardOutput() {
        return standardOutput;
    }

    public List<String> getErrorInput() {
        return errorInput;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
