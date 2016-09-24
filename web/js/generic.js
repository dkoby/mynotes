
function outputError(message, control) {
    var reqListener = function() {
        if (this.readyState == 4 && this.status == 200)
            this.errorOutput.innerHTML = this.responseText;
        else
            this.errorOutput.innerHTML = "Error";
    }

    var request = new XMLHttpRequest();
    request.errorOutput = control;
    request.timeout            = generic.timeoutXMLHttp; 
    request.ontimeout          = function() {
        this.errorOutput.innerHTML = "MsgCat timeout";
    }
    request.addEventListener("load", reqListener);
    request.open("GET", "msgcat?message=" + message);
    request.send();
}

var Generic = function() {
    this.showWaitOverlay = function() {
        document.getElementById("overlay_loading").style.visibility="visible";
    }
    this.hideWaitOverlay = function() {
        document.getElementById("overlay_loading").style.visibility="hidden";
    }

    this.timeoutXMLHttp = 30 * 1000;
}

Generic.prototype.imageOnMouseOver = function(img) {
    var path = img.src;

    var ext_index  = path.lastIndexOf('.png');
    img.src = path.slice(0, ext_index) + "_hover" + path.slice(ext_index);
}
Generic.prototype.imageOnMouseOut = function(img) {
    var path = img.src;

    var file_index  = path.lastIndexOf('/');
    var hover_index = path.lastIndexOf('_hover');
    var ext_index   = path.lastIndexOf('.png');

    if (hover_index > 0) {
        img.src = path.slice(0, file_index) + path.slice(file_index, hover_index) + path.slice(ext_index);
    }
}

Generic.prototype.getSearchParameters = function() {
      var prmstr = window.location.search.substr(1);

      function transformToAssocArray( prmstr ) {
          var params = {};
          var prmarr = prmstr.split("&");
          for ( var i = 0; i < prmarr.length; i++) {
              var tmparr = prmarr[i].split("=");
              params[tmparr[0]] = tmparr[1];
          }
          return params;
      }

      return prmstr != null && prmstr != "" ? transformToAssocArray(prmstr) : {};
}

Generic.prototype.overlayClose = function() {
    elements = document.getElementsByClassName("popup");
    for (var i = 0; i < elements.length; i++) {
        elements[i].style.visibility = "hidden";
    }
}

generic = new Generic();

