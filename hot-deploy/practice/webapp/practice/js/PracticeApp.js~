Event.observe(window, 'load', function() {
    var validateForm = new Validation('createPersonForm', {immediate: true, onSubmit: false});
    Event.observe('createPerson', 'click', function() {
       if (validateForm.validate()) {
           createPerson();
       }
    });
});

function createPerson() {
    var request = $('createPersonForm').action;
    new Ajax.Request(request, {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            var serverError = getServerError(data);
            if (serverError != "") {
                Effect.Appear('createPersonError', {duration: 0.0});
                $('createPersonError').update(serverError);
            } else {
                Effect.Fade('createPersonError', {duration: 0.0});
                new Ajax.Updater($('personList'), 'UpdatedPersonList', {evalScripts: true});
            }
        }, parameters: $('createPersonForm').serialize(), requestHeaders: {Accept: 'application/json'}
    });
}

getServerError = function (data) {
    var serverErrorHash = [];
    var serverError = "";
    if (data._ERROR_MESSAGE_LIST_ != undefined) {
        serverErrorHash = data._ERROR_MESSAGE_LIST_;

        serverErrorHash.each(function(error) {
            if (error.message != undefined) {
                serverError += error.message;
            }
        });
        if (serverError == "") {
            serverError = serverErrorHash;
        }
    }
    if (data._ERROR_MESSAGE_ != undefined) {
        serverError += data._ERROR_MESSAGE_;
    }
    return serverError;
};