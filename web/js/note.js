
var NoteManager = function() {
}

NoteManager.prototype.goToPage = function() {
    var input = document.getElementById("pageInput");
    var pageVal = input.value;

    input.style.color = "#000";
    if (isNaN(pageVal) || pageVal < 0) {
        input.style.color = "#f00";
    } else {
        window.location = "home?nav=books&bookid=" + generic.getSearchParameters()["bookid"] + "&pagenum=" + (pageVal - 1);
    }
}

var noteManager = new NoteManager();

document.getElementById("pageInput").addEventListener('keypress', function(event) {
    const keyName = event.key;
    if (keyName === 'Enter') {
        noteManager.goToPage();
        return;
    }
} , true /* capture event */);

window.addEventListener('load', function() {window.scrollTo(0, document.body.scrollHeight);});


