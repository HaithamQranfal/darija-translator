package ma.aui.translator.llm;

/** Delimiters used to fence the user's text inside the prompt (mitigates prompt injection). */
public final class PromptMarkers {
    public static final String BEGIN = "<<<TEXT";
    public static final String END = "TEXT>>>";

    private PromptMarkers() {
    }

    /** Returns the text between the markers, or the whole prompt if markers are absent. */
    public static String extract(String prompt) {
        int b = prompt.indexOf(BEGIN);
        int e = prompt.lastIndexOf(END);
        if (b >= 0 && e > b) {
            return prompt.substring(b + BEGIN.length(), e).trim();
        }
        return prompt;
    }
}
