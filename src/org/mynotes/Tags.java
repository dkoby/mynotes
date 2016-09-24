/*
 *
 */
package org.mynotes;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

/*
 *
 */
public class Tags {
    private ArrayList<String> list;

    /*
     *
     */
    public Tags() {
        list = new ArrayList<>();
    }
    /*
     *
     */
    public Tags(Tags tags) {
        list = new ArrayList<>(tags.get());
    }
    /*
     *
     */
    public void add(String tag) {
        if (!haveTag(tag))
            list.add(tag);
    }
    /*
     *
     */
    public ArrayList<String> get() {
        return list;
    }
    /*
     *
     */
    public boolean isEmpty() {
        return list.size() > 0 ? false : true;
    }
    /*
     *
     */
    public void remove(String tag) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(tag)) {
                list.remove(i);
                break;
            }
        }
    }
    /*
     *
     */
    public boolean haveTag(String tag) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(tag)) {
                return true;
            }
        }
        return false;
    }
    /*
     *
     */
    public void clear() {
        list.clear();
    }
    /**
     * Compile tags from string.
     */
    public static Tags compileTags(String tagString) {
        Tags tags = new Tags();

        int start = 0;
        while (true) {
            Matcher matcher = Pattern.compile("\\s*?(#[^#\\s,]+)").matcher(tagString);
            if (!matcher.find(start))
                break;

            String tag = matcher.group(1);

            if (tag.length() > 1)
                tags.add(tag);
            start = matcher.end();
        }
        return tags;
    }
    /**
     * Concat tags to string.
     */
    public String concat() {
        StringBuilder output = new StringBuilder();

        for (String tag: list)
            output.append(tag + " ");

        /* trim last space */
        if (list.size() > 0)
            output.setLength(output.length() - 1);

        return output.toString();
    }
}

