var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
}

function connect() {
    console.log("1")
    stompClient = Stomp.client('ws://localhost:3000/auth-server');
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/user/auth/reg', function (response) {
            console.log(response);
        });
        stompClient.subscribe('/user/auth/token', function (response) {
            console.log(response);
        });
        stompClient.subscribe('/user/auth/chal', function (response) {
            console.log(response);
        });
        stompClient.subscribe('/user/auth/auth', function (response) {
            console.log(response);
        });
    });
}

function register() {
    var user = {
    "username" : $("#username").val(),
    "pubKey" : { "y" : $("#key_y").val(),
                 "g" : $("#key_g").val(),
                 "p" : $("#key_p").val()} };

    stompClient.send("/reg", {}, JSON.stringify(user));
}

function authenticate() {
    var token = $("#token_input").val();
    stompClient.send("/auth", {}, token);
}
function requestChallenge() {
    var username = $("#rq_chal_username").val();
    stompClient.send("/reqChal", {}, username);
}

function answerChallenge() {
    console.log("Answer chal called");
    var answer = {"username" : $("#ans_chal_username").val(),
                    "decChal": $("#ans_chal_text").val()};

    stompClient.send("/ansChal", {}, JSON.stringify(answer));
}

function disconnect() {
    if (stompClient != null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

$(function () {
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#create_user" ).click(function() { register(); });
    $( "#req_chal" ).click(function() { requestChallenge(); });
    $( "#ans_chal" ).click(function() { answerChallenge(); });
    $( "#authenticate_button").click(function() {authenticate(); });
});