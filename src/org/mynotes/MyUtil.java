/*
 *
 */
package org.mynotes;

public class MyUtil {
    public static String SQLEscape(String input) {
        StringBuilder output = new StringBuilder();
        for (char ch: input.toCharArray()) {
            if (ch == '\'')
                output.append("\'");
//            else if (ch == ';')
//                output.append('\\');
            output.append(ch);
        }
        /* NOTE HTMLEscape */
        return output.toString();
    }
    public static String HTMLEscape(String input) {
        StringBuilder output = new StringBuilder();
        for (char ch: input.toCharArray()) {
            if (ch == '&')
                output.append("&amp;");
            else if (ch == '<')
                output.append("&lt;");
            else if (ch == '>')
                output.append("&gt;");
            else if (ch == '"')
                output.append("&quot;");
            else if (ch == '\'')
                output.append("&#39;");
            else
                output.append(ch);
        }
        return output.toString();
    }
}

