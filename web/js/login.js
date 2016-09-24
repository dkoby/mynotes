
'use strict'

var LoginForm = function() {
    this.form          = document.getElementById("login");
    this.inputUser     = document.getElementById("login_name");
    this.inputPassword = document.getElementById("login_pass");
    this.checkbox      = document.getElementById("login_remcb");
    this.loginError    = document.getElementById("login_error");

    var keyPressListener = function(event) {
        const keyName = event.key;
        if (keyName === 'Enter') {
            this.formObject.enter();
            return;
        }
    }
    this.inputUser.formObject     = this;
    this.inputPassword.formObject = this;

    this.inputUser.addEventListener    ('keypress', keyPressListener, true /* capture event */);
    this.inputPassword.addEventListener('keypress', keyPressListener, true /* capture event */);
}
LoginForm.prototype.enter = function() {
        /*
         * TODO
         *     * Prevalidate form data.
         *     * Send XMLHttpRequest, check response.
         *       Proceed to home page if success, show invalid user/password if necessary.
         */
//    this.outputError("LOGIN_ERROR_INVALID_USER_INPUT");
    this.form.submit();
};
LoginForm.prototype.register = function() {
    /* TODO pass user to register servlet from input form of login page */
    window.location = "register?user=" + this.inputUser.value;
};
LoginForm.prototype.clickRemember = function() {
    if (this.checkbox.checked) {
        this.checkbox.checked = false;
    } else {
        this.checkbox.checked = true;
    }
};
var login = new LoginForm();

//var HttpClient = function() {
//    this.get = function(aUrl, aCallback) {
//        var anHttpRequest = new XMLHttpRequest();
//        anHttpRequest.onreadystatechange = function() { 
//            if (anHttpRequest.readyState == 4 && anHttpRequest.status == 200)
//                aCallback(anHttpRequest.responseText);
//        }
//
//        anHttpRequest.open( "GET", aUrl, true );            
//        anHttpRequest.send( null );
//    }
//}

//    aClient = new HttpClient();
//    aClient.get('register', function(response) {
//        document.innerHtml = response;
//        // do something with response
//    });


