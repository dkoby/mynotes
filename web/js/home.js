
var FormPost = function(formData, errorOutput, backPage) {
    var _this = this;

    this.backPage = backPage;

    generic.showWaitOverlay();

    var request = new XMLHttpRequest();
    request.errorOutput  = errorOutput;
    request.timeout      = generic.timeoutXMLHttp; 
    request.ontimeout    = function() {
        this.errorOutput.innerHTML = "Timeout";
    }
    request.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                if (this.responseText != "success") {
                    this.errorOutput.innerHTML = this.responseText;
                } else {
                    generic.overlayClose();
                    window.location = _this.backPage;
                }
            } else {
                this.errorOutput.innerHTML = "Internal error";
            }
            generic.hideWaitOverlay();
        }
    }
    request.open("POST", "formaction");
    request.send(formData);
}

function click_button_create_book() {
    document.getElementById("bookDialogName").value = "";
    document.getElementById("bookDialogTags").value = "";
    document.getElementById("bookDialogDescription").value = "";
    document.getElementById("bookDialogEditBookId").value = "";
    document.getElementById("bookDialogIcon").value = "";

    document.getElementById("overlay").style.visibility="visible";
    document.getElementById("bookDialog").style.visibility="visible";
    document.getElementById("bookDialogName").focus();
}
function click_button_create_book_proceed() {
    FormPost(new FormData(document.getElementById("makebook")),
            document.getElementById("bookDialogErrorOutput"), "home?nav=books");
}

function click_button_delete_book(bookid) {
    var bookDeleteDialog = document.getElementById("bookDeleteDialog");
    bookDeleteDialog.bookId = bookid;

    document.getElementById("overlay").style.visibility="visible";
    bookDeleteDialog.style.visibility="visible";
    document.getElementById("bookDeleteDialogButton").focus();
}
function click_button_delete_book_proceed() {
    var formData = new FormData();
    formData.append("type", "deletebook");
    formData.append("bookid", document.getElementById("bookDeleteDialog").bookId);

    FormPost(formData, document.getElementById("bookDeleteDialogErrorOutput"), "home?nav=books");
}

function click_button_create_note() {
    document.getElementById("noteDialogMessage").value = "";
    document.getElementById("noteDialogTags").value = "";
    document.getElementById("noteDialogEditNoteId").value = "";
    document.getElementById("noteDialogImage").value = "";

    document.getElementById("overlay").style.visibility="visible";
    document.getElementById("noteDialog").style.visibility="visible";
    document.getElementById("noteDialogMessage").focus();
}

function click_button_create_note_proceed(bookId) {
    FormPost(new FormData(document.getElementById("makenote")),
            document.getElementById("noteDialogErrorOutput"), "home?nav=books&bookid=" + bookId);
}

function click_button_delete_note(noteId, bookId) {
    var noteDeleteDialog = document.getElementById("noteDeleteDialog");

    noteDeleteDialog.noteId = noteId;
    noteDeleteDialog.bookId = bookId;

    document.getElementById("overlay").style.visibility="visible";
    noteDeleteDialog.style.visibility="visible";
    document.getElementById("noteDeleteDialogButton").focus();
}

function click_button_delete_note_proceed() {
    var noteDeleteDialog = document.getElementById("noteDeleteDialog");

    var formData = new FormData();
    formData.append("type", "deletenote");
    formData.append("noteid", noteDeleteDialog.noteId);

    FormPost(formData,
            document.getElementById("noteDialogErrorOutput"),
            "home?nav=books&bookid=" + noteDeleteDialog.bookId);
}

function click_button_edit_note(noteId) {
    var editDialog = new EditDialog(
            /* dialogName */
            "noteDialog",
            /* fields */
            {
                "Message" : "getnote?noteid=" + noteId + "&field=message",
                "Tags"    : "getnote?noteid=" + noteId + "&field=tags",
            },
            /* initFunction */
            [function() {
                document.getElementById("noteDialogImage").value = "";
                document.getElementById("noteDialogEditNoteId").value = noteId;
            }],
            /* succeedFunction */
            [function() {
                document.getElementById("noteDialogMessage").focus();
            }]
            );
}

function click_button_edit_book(bookId) {
    var editDialog = new EditDialog(
            /* dialogName */
            "bookDialog",
            /* fields */
            {
                "Name"        : "getbook?bookid=" + bookId + "&field=name"   ,
                "Description" : "getbook?bookid=" + bookId + "&field=description" ,
                "Tags"        : "getbook?bookid=" + bookId + "&field=tags",
            },
            /* initFunction */
            [function() {
                document.getElementById("bookDialogIcon").value = "";
                document.getElementById("bookDialogEditBookId").value = bookId;
            }],
            /* succeedFunction */
            [function() {
                document.getElementById("bookDialogName").focus();
            }]
            );
}

/*
 *
 */
function stop_propagation(e) {
    if (!e)
        e = window.event;

    if (e.stopPropagation)
        e.stopPropagation();
    else
        e.cancelBubble = true;
}

/*
 * Pup-up edit dialog. Request server for fields data and fill them.
 *
 * ARGS
 *     dialogName      name of dialog (div id)
 *     fields          field name and field request
 *     initFunction    array with init function and args
 *     succeedFunction function called on success
 */
var EditDialog = function(dialogName, fields, initFunction, succeedFunction) {
    initFunction[0].apply(this, initFunction.slice(1));

    for (var field in fields) {
//        console.log("FIELD " + field + " = " + fields[field]);
        document.getElementById(dialogName + field).value = "";
    }

    generic.showWaitOverlay();

    var readyNum = 0;
    var error    = false;
    var wasError = false;
    var requests = new Array();

    var showResult = function() {
        if (error) {
            if (!wasError) {
                wasError = true;
                for (var i in requests)
                    requests[i].abort();
                generic.hideWaitOverlay();
                generic.overlayClose();
                alert("Error");
            }
            return;
        }

        if (++readyNum >= requests.length) {
            generic.hideWaitOverlay();
            document.getElementById("overlay").style.visibility="visible";
            document.getElementById(dialogName).style.visibility="visible";
            succeedFunction[0].apply(this, succeedFunction.slice(1));
        }
    }

    var processStateChange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                this.dataOutput.value = this.responseText;
            } else {
                error = true;
            }
            /* XXX is status always equals to 0 on timeout? */
            showResult();
        }
    }

    for (var field in fields) {
        var request = new XMLHttpRequest();

        request.dataOutput = document.getElementById(dialogName + field);
        request.serverRequest = fields[field];

        requests.push(request);
    }

    for (var i in requests) {
        var request = requests[i];

        request.onreadystatechange = processStateChange;
        request.timeout = generic.timeoutXMLHttp; 
        request.open("GET", request.serverRequest);
        request.send();
    }
}

var TagControl = function(tagType, backPage) {
    this.tagType = tagType;
    this.backPage = backPage;
}
TagControl.prototype.exec = function(action, tag) {
    var _this = this;

    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                window.location = _this.backPage;
            }
        }
    }
    request.timeout = generic.timeoutXMLHttp; 

    var parameters = "tagcontrol?type=" + _this.tagType + "&" + action;
    if (tag != undefined)
        parameters += "&tag=" + tag;
    request.open("GET", parameters);
    request.send();
}

var bookTagControl = new TagControl("booktag", "home?nav=books");
var noteTagControl = new TagControl("notetag", "home?nav=books&bookid=" + generic.getSearchParameters()["bookid"]);


/* XXX */
document.body.addEventListener('keypress',
    function(event) {
        const keyName = event.key;
        if (keyName == 'Escape') {
            generic.overlayClose();
            return;
        }
    }, true /* capture event */);


function follow_link(link) {
    window.location = link;
}


