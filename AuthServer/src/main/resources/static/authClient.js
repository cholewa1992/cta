var stompClient = null;
var serverIdentifier = null;
var token = null;
var auth_interval = 3000;
var refresh_at = 1500;
var timeOutTime = 10000;
var current_user = null;
var cancelled_auth = false;
var server_connection = null;
var awaitingAuthenticatorRegResponse = false;
var authenticatorConnected = false;
awaitingAuthenticatorChalResponse = false;

var AUTH_SERVICE = '00001506-0000-1000-8000-00805f9b34fb';
var REGISTER = '00001a00-0000-1000-8000-00805f9b34fb';
var SERVICE_ID = '00002800-0000-1000-8000-00805f9b34fb';
var USER_ID = '00002801-0000-1000-8000-00805f9b34fb';
var CHALLENGE = '00002802-0000-1000-8000-00805f9b34fb';
var AUTHENTICATE = '00001a01-0000-1000-8000-00805f9b34fb';

$( document ).ready(function() {
	// Connects to the server
	connect();
});

function connect() {
	stompClient = Stomp.client('ws://localhost:3000/auth-server');
	stompClient.connect({}, function (frame) {
		console.log('Connected: ' + frame);

		stompClient.subscribe('/user/auth/regInfo', function (response) {
			handleRegInfoResponse(response)
		});
		stompClient.subscribe('/user/auth/reg', function (response) {
			handleRegResponse(response)
		});
		stompClient.subscribe('/user/auth/chal', function (response) {
			handleChalResponse(response)
		});
		stompClient.subscribe('/user/auth/token', function (response) {
			handleTokenResponse(response)
		});
		stompClient.subscribe('/user/auth/auth', function (response) {
			handleAuthResponse(response)
		});
		writeToLog("CLIENT -> Connected to SERVER successfully!");
	});
}

async function handleRegInfoResponse(response) {
	var body = JSON.parse(response.body);

	if (body.success) {
		writeToLog("SERVER -> Server identifier: '" + body.content.serverIdentifier + "'");
		writeToLog("SERVER -> The username '" + body.content.username + "' is available.");
		writeToLog("CLIENT -> Proceeding with registration. Initiating contact with AUTHENTICATOR...");

		serverIdentifier = body.content.serverIdentifier;
		if (authenticatorConnected) {
			sendRegistrationReqToAuthenticator(serverIdentifier, body.content.username);
			await sleep(timeOutTime);
			if (awaitingAuthenticatorRegResponse) {
				writeTimeOutError();
			}
		}
		else {
			writeDisconnectError();
		}
	}
	else {
		writeToLog("SERVER -> " + body.errorMessage);
	}
	resetLogin();
}

function writeTimeOutError() {
	writeToLog("CLIENT -> AUTHENTICATOR did not respond within " + timeOutTime + " ms. Try again!");
}

function writeDisconnectError() {
	writeToLog("CLIENT -> AUTHENTICATOR was disconnected from client. Please pair again to reconnect.");
}

function handleRegResponse(response) {
	var body = JSON.parse(response.body);

	if (body.success) {
		writeToLog("SERVER -> User '" + body.content.username + "' registered successfully with public key: ");
		writeToLog("\t SERVER -> Y = " + parseBytesToBigInt(base64ToArrayBuffer(body.content.pubKey.y)));

		writeToLog("\t SERVER -> Y = " + parseBytesToBigInt(base64ToArrayBuffer(body.content.pubKey.y)));
		writeToLog("\t SERVER -> P = " + parseBytesToBigInt(base64ToArrayBuffer(body.content.pubKey.p)));
		writeToLog("\t SERVER -> G = " + parseBytesToBigInt(base64ToArrayBuffer(body.content.pubKey.g)));
	}
	else {
		writeToLog("SERVER -> " + body.errorMessage);
	}
	resetLogin();
}

async function handleChalResponse(response) {
	var body = JSON.parse(response.body);

	if (body.success) {
		writeToLog("SERVER -> Generated new challenge (base64-encoded): " + body.content);
		writeToLog("CLIENT -> Forwarding challenge to AUTHENTICATOR...");

		if (authenticatorConnected) {
			sendChallengeToAuthenticator(serverIdentifier, current_user, body.content);
			await sleep(timeOutTime);
			if (awaitingAuthenticatorChalResponse) {
				writeToLog("CLIENT -> AUTHENTICATOR did not respond within " + timeOutTime + " ms. Trying again...");
				sendChallengeToAuthenticator(serverIdentifier, current_user, body.content);
				await sleep(timeOutTime);
				if (awaitingAuthenticatorChalResponse) {
					writeToLog("CLIENT -> AUTHENTICATOR did still not respond respond after " + timeOutTime + " ms more. The problem is likely with the AUTHENTICATOR.");
					resetLogin();
				}



			}
		}
		else {
			writeDisconnectError();
			resetLogin();
		}

	}
	else {
		writeToLog("SERVER -> " + body.errorMessage);
		resetLogin();
	}
}

function handleTokenResponse(response) {
	var body = JSON.parse(response.body);

	if (body.success) {
		writeToLog("SERVER -> Challenge solved successfully. Generated new authentication token: " + body.content);
		writeToLog("CLIENT -> Initiating authentication session with server, by sending token");
		token = body.content;
		continuousAuthentication(token);
	}
	else {
		writeToLog("SERVER -> " + body.errorMessage);
		resetLogin();
	}
}

function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

async function handleAuthResponse(response) {
	var body = JSON.parse(response.body);

	if (body.success) {
		writeToLog("SERVER -> " + body.content + ". This session will expire in: " + body.millisecondsBeforeExpiration + " ms");
		await sleep(auth_interval);

		if (cancelled_auth) {
			resetLogin();
		}
		else {
			if (body.millisecondsBeforeExpiration - auth_interval < refresh_at) {
				requestNewChallenge();
			}
			else {
				continuousAuthentication(token);
			}
		}
	}
	else {
		writeToLog("SERVER -> " + body.errorMessage);
		resetLogin();
	}
}

function registerBtnClicked() {

	var username = $("#username_input").val();
	current_user = username;
	if (username) {
		writeToLog("CLIENT -> Requesting user registration from SERVER");
		stompClient.send("/regInfo", {}, username);
		$("#username_input").prop('disabled', true);
		$("#register_btn").prop('disabled', true);
		$("#login_btn").prop('disabled', true);
		$("#pair_btn").prop('disabled', true);
		$("#cancel_btn").prop('disabled', true);
	}

}

function loginBtnClicked() {

	current_user = $("#username_input").val();
	if (current_user) {
		$("#username_input").prop('disabled', true);
		$("#register_btn").prop('disabled', true);
		$("#login_btn").prop('disabled', true);
		$("#pair_btn").prop('disabled', true);
		$("#cancel_btn").prop('disabled', false);
		requestNewChallenge();
	}
}

function cancelBtnClicked() {
	cancelled_auth = true;
	resetLogin();
	writeToLog("CLIENT -> User cancelled the authentication session");
}


async function handleCharacteristicValueChanged(event) {
	console.log(event);

	var value = event.target.value;

	if (event.target.uuid == REGISTER) {
		awaitingAuthenticatorRegResponse = false;
		completeRegistration(value);
	}
	if (event.target.uuid == AUTHENTICATE) {
		awaitingAuthenticatorChalResponse = false;
		completeChallengeSolving(value);
	}
}

function subscribe(server, service_uuid, characteristic_uuid, handler){
	return server.getPrimaryService(service_uuid)
		.then(service => service.getCharacteristic(characteristic_uuid))
		.then(characteristic => characteristic.startNotifications())
		.then(characteristic => characteristic.addEventListener('characteristicvaluechanged', handler))
		.then(() => server);
}

var bluetoothDevice;

function pairBtnClicked() {
	navigator.bluetooth.requestDevice({ filters: [{ services: [ AUTH_SERVICE ] }] })
		.then(device => {

			device.addEventListener('gattserverdisconnected', onDisconnected);
			bluetoothDevice = device;
			connectBtDevice();

		}).catch(error => { console.log(error); });
}

function connectBtDevice() {
	exponentialBackoff(3 /* max retries */, 2 /* seconds delay */,
		function toTry() {
			time('Connecting to Bluetooth Device... ');

			return bluetoothDevice.gatt.connect()
			.then(server => subscribe(server, AUTH_SERVICE, REGISTER, handleCharacteristicValueChanged))
			.then(server => subscribe(server, AUTH_SERVICE, AUTHENTICATE, handleCharacteristicValueChanged))
			.then(server => server_connection = server)
			.then(() => { writeToLog("CLIENT -> Connected to AUTHENTICATOR successfully."); })

		},
		function success() {
			console.log('> Bluetooth Device connected. Try disconnect it now.');
			authenticatorConnected = true; 
		},
		function fail() {
			time('Failed to reconnect.');
			authenticatorConnected = false;
		});
}

function onDisconnected(event) {
	console.log("onDisconnected");
	console.log('Device ' + bluetoothDevice.name + ' is disconnected.');
	connectBtDevice();
}

function exponentialBackoff(max, delay, toTry, success, fail) {
	toTry().then(result => success(result))
		.catch(_ => {
			if (max === 0) {
				return fail();
			}
			time('Retrying in ' + delay + 's... (' + max + ' tries left)');
			setTimeout(function() {
				exponentialBackoff(--max, delay * 2, toTry, success, fail);
			}, delay * 1000);
		});
}

function time(text) {
	console.log('[' + new Date().toJSON().substr(11, 8) + '] ' + text);
}

function resetLogin() {
	$("#username_input").prop('disabled', false);
	$("#register_btn").prop('disabled', false);
	$("#login_btn").prop('disabled', false);
	$("#pair_btn").prop('disabled', false);
	$("#cancel_btn").prop('disabled', true);
	cancelled_auth = false;
}

function requestNewChallenge() {
	writeToLog("CLIENT -> Requesting new challenge from server");
	console.log(current_user);
	stompClient.send("/reqChal", {}, current_user);
}


function writeDescriptor(characteristic, descriptor_uuid, msg){
	let encoder = new TextEncoder('utf-8');
	return characteristic.getDescriptor(descriptor_uuid)
		.then(descriptor => descriptor.writeValue(encoder.encode(msg)))
		.then(() => characteristic)
}

function writeDescriptorWithArray(characteristic, descriptor_uuid, array){
	return characteristic.getDescriptor(descriptor_uuid)
		.then(descriptor => descriptor.writeValue(array))
		.then(() => characteristic)
}



function sendRegistrationReqToAuthenticator(serverIdentifier, username) {

	if(server_connection != null && server_connection.connected){
		server_connection.getPrimaryService(AUTH_SERVICE)
			.then(service => service.getCharacteristic(REGISTER))
			.then(characteristic => writeDescriptor(characteristic, SERVICE_ID, serverIdentifier))
			.then(characteristic => writeDescriptor(characteristic, USER_ID, username))
			.then(characteristic => characteristic.writeValue(Uint8Array.of(0)))
			.then(() => { console.log("Register write send"); awaitingAuthenticatorRegResponse = true; })
			.catch(error => { console.log(error); });
	}
}

function sendChallengeToAuthenticator(serverIdentifier, username, challenge) {
	if(server_connection != null && server_connection.connected){
		server_connection.getPrimaryService(AUTH_SERVICE)
			.then(service => service.getCharacteristic(AUTHENTICATE))
			.then(characteristic => writeDescriptor(characteristic, SERVICE_ID, serverIdentifier))
			.then(characteristic => writeDescriptor(characteristic, USER_ID, username))
			.then(characteristic => writeDescriptorWithArray(characteristic, CHALLENGE, base64ToArrayBuffer(challenge)))
			.then(characteristic => characteristic.writeValue(Uint8Array.of(0)))
			.then(() => { console.log("Authenticate write send"); awaitingAuthenticatorChalResponse = true; })
			.catch(error => { console.log(error); });
	}
}


function arrayBufferTo64EncodeString(buffer) {
	return btoa(
		new Uint8Array(buffer)
		.reduce((data, byte) => data + String.fromCharCode(byte), '')
	);
}

function base64ToArrayBuffer(base64) {
	var u8arr = new Uint8Array(atob(base64).split("").map(function(c) {
		return c.charCodeAt(0); }));

	return u8arr.buffer;
}



function completeRegistration(dataView) {
	var base64PubKey = arrayBufferTo64EncodeString(dataView.buffer);

	writeToLog("AUTHENTICATOR -> Successfully created new ElGamal keypair and combined public key");
	writeToLog("CLIENT -> Forwarding public key for user: '" + current_user + "' to " + serverIdentifier);

	var user = {
		"username" : current_user,
		"pubKeyB64": base64PubKey };


	stompClient.send("/reg", {}, JSON.stringify(user));
}

function completeChallengeSolving(dataView) {

	var decryptedCipherB64 = arrayBufferTo64EncodeString(dataView.buffer);

	if (decryptedCipherB64) {
		writeToLog ("AUTHENTICATOR -> Decrypted challenge cipher to cleartext: (base64-encoded) " + decryptedCipherB64);
		var answer = {"username" : current_user,
			"decChal": decryptedCipherB64};

		writeToLog("CLIENT -> Sending the decrypted challenge cipher to server...");

		stompClient.send("/ansChal", {}, JSON.stringify(answer));
	}
	else {
		resetLogin();
		writeToLog ("CLIENT -> AUTHENTICATOR did not respond with a solution to the challenge.");
	}
}

function continuousAuthentication(token) {
	stompClient.send("/auth", {}, token);
}

function parseBytesToBigInt(bytes) {
	console.log(bytes);
	var result = new BigInteger(0);
	var multiplier = new BigInteger(1);
	for(var i = bytes.length - 1; i >= 0; i--) {
		result = result.add(multiplier.multiply(bytes[i]));
		multiplier = multiplier.multiply(256);
	}
	console.log(result);
	return result;
}

function writeToLog(text) {
	var $logArea = $('#log_txt');
	$logArea.val($logArea.val() + text + "\n");
	$logArea.scrollTop($logArea[0].scrollHeight);
}
function clearLogBtnClicked() {
	$('#log_txt').val('');
}
$(function () {
	$( "#register_btn" ).click(function() { registerBtnClicked(); });
	$( "#clear_log_btn" ).click(function() { clearLogBtnClicked(); });
	$( "#login_btn").click(function() { loginBtnClicked(); });
	$( "#cancel_btn").click(function() { cancelBtnClicked(); });
	$( "#pair_btn").click(function() {pairBtnClicked(); })
});
