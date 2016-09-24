
'use strict'

var RegisterForm = function() {
    this.form           = document.getElementById("register");
    this.inputName      = document.getElementById("register_name");
    this.inputPassword  = document.getElementById("register_pass");
    this.inputPasswordR = document.getElementById("register_pass_repeat");
    this.inputEmail     = document.getElementById("register_email");
    this.errorOutput    = document.getElementById("register_error");
}
RegisterForm.prototype.process = function() {
    this.errorOutput.innerHTML = "";
    if (!this.inputName.value.match("^[a-zA-Z0-9_\\-]{1,20}$")) {
        outputError("REGISTER_ERROR_NAME_INVAL", this.errorOutput);
        return;
    }
    if (this.inputPassword.value != this.inputPasswordR.value) {
        outputError("REGISTER_ERROR_PASSWORD_MISMATCH", this.errorOutput);
        return;
    }
    if (this.inputPassword.value.length < 6) {
        outputError("REGISTER_ERROR_PASSWORD_TOO_SHORT", this.errorOutput);
        return;
    }
    if (this.inputPassword.value.length > 40) {
        outputError("REGISTER_ERROR_PASSWORD_TOO_LONG", this.errorOutput);
        return;
    }
    /*
     * The local-part of the e-mail address may use any of these ASCII characters:
     *     * Uppercase and lowercase English letters (a-z, A-Z)
     *     * Digits 0 to 9
     *     * Characters ! # $ % & ' * + - / = ? ^ _ ` { | } ~
     *     * Character . (dot, period, full stop) provided that it is not the first or last character, and provided also that it does not appear two or more times consecutively.
     */

    /* TODO valid processing */
    if (!this.inputEmail.value.match("^[^@]+@[^@]+$")) {
        outputError("REGISTER_ERROR_EMAIL_INVALID", this.errorOutput);
        return;
    }
    this.form.submit();
}

var register = new RegisterForm();

