package dk.kb.tvsubtitleocr.lib.postprocessing;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TextProcessor {
    private final static String regexStringWhitelist = "[^a-zA-Z0-9æøåÆØÅ .,!?]";
    private final static String regexStringNumbers = "[^0-9";
    private final static String[] wordsWithoutVowels = {
            "bh", "bs", "cd", "cg", "cl", "cm", "cv", "dg", "dj", "dl", "dm", "fm", "fx", "hd", "hf", "hg", "hh", "hk",
            "hl", "hm", "kb", "kg", "km", "lp", "mb", "mf", "mg", "ml", "mm", "ms", "mt", "pc", "pr", "qhd", "tb", "tf",
            "tv", "vm",
            "ddt", "dtp", "dvd", "hhx", "htx", "kgm", "lsd", "mpv", "pcb", "prr", "prs", "pst", "pvc", "stx", "tfs", "vvs",
            "hdtv"};

    public TextProcessor() {

    }

    public List<String> removeInvalidChars(List<String> text) {
        List<String> result = new LinkedList<>();
        text.forEach(n -> result.add(removeInvalidChars(n)));
        return result;
    }

    public String removeInvalidChars(String s) {
        String result = s.replaceAll(regexStringWhitelist, "");
        return result;
    }

    public List<String> removeInvalidText(List<String> text) {
        List<String> result = new LinkedList<>();

        for (String string : text) {
            if(string.isEmpty()) continue;

            StringBuilder smallResult = new StringBuilder();
            string = string.trim();

            String[] split = string.split(" ");
            List<String> strings = Arrays.asList(split);

            int words = 0;
            int numberSets = 0;

            for ( String ss : strings) {
                if(containsAVowel(ss) || StringUtils.equalsAnyIgnoreCase(ss, wordsWithoutVowels)) {
                    smallResult.append(ss);
                    smallResult.append(" ");
                    words++;
                }
                else if(containsNumbers(ss, 1)) {
                    smallResult.append(ss);
                    smallResult.append(" ");
                    numberSets++;
                }
            }

            string = smallResult.toString().trim();
            boolean noNumberWithoutAWord = (words > 0 && numberSets < 1) || (words > 0);

            if ( ! string.isEmpty() && noNumberWithoutAWord && string.length() > 1) {
                result.add(string);
            }
        }

        if(result.isEmpty()) {
            result = null;
        }

        return result;
    }

    protected String removeNumbers(String s) {
        return StringUtils.removeAll(s, regexStringNumbers);
    }

    protected boolean containsAVowel(String s) {
        if (s.isEmpty()) return false;

        final char[] vowelsChars = {'a', 'e', 'i', 'o', 'u', 'y', 'æ', 'ø', 'å'};
        int vowels = countInstancesOfChars(s, vowelsChars);

        return vowels > 0;
    }

    protected boolean containsNumbers(String s, int minimumAmount) {
        if(s.isEmpty()) return false;

        final char[] numbers = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
        int numbersCount = countInstancesOfChars(s, numbers);

        return numbersCount >= minimumAmount;
    }

    protected int countInstancesOfChars(String s, char[] values) {
        int instances = 0;

        for(char character : values) {
            instances += StringUtils.countMatches(s, character);
        }

        return instances;
    }

}
