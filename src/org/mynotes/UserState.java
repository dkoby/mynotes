/*
 *
 */
package org.mynotes;

import java.util.ArrayList;
import java.util.logging.Level;

import org.mynotes.MyNotes;
import org.mynotes.Tags;

/*
 *
 */
public class UserState {
    public Tags noteTags;
    public Tags bookTags;

    public UserState() {
        noteTags = new Tags();
        bookTags = new Tags();
    }

}

