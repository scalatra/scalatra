$(function() {
  "use strict";

  var detect = $("#detect");
  var header = $('#header');
  var content = $('#content');
  var input = $('#input');
  var status = $('#status');
  var myName = false;
  var author = null;
  var logged = false;
  var socket = $.atmosphere;
  var subSocket;
//  var transport = 'long-polling';
  var transport = 'websocket';

  var request = {
    url: "/atmosphere/the-chat",
    contentType: "application/json",
    logLevel: 'debug',
    transport: transport,
    trackMessageLength : true,
    fallbackTransport: 'long-polling'
  };

  request.onOpen = function(response) {
    content.html($('<p>', {
      text: 'Atmosphere connected using ' + response.transport
    }));
    input.removeAttr('disabled').focus();
    status.text('Choose name:');
    transport = response.transport;

    if (response.transport == "local") {
      subSocket.pushLocal("Name?");
    }
  };

  request.onReconnect = function(rq, rs) {
    socket.info("Reconnecting")
  };

  request.onMessage = function(rs) {

    // We need to be logged first.
    if (!myName) return;

    var message = rs.responseBody;
    try {
      var json = jQuery.parseJSON(message);
      console.log("got a message")
      console.log(json)
    } catch (e) {
      console.log('This doesn\'t look like a valid JSON object: ', message.data);
      return;
    }

    if (!logged) {
      logged = true;
      status.text(myName + ': ').css('color', 'blue');
      input.removeAttr('disabled').focus();
      subSocket.pushLocal(myName);
    } else {
      input.removeAttr('disabled');
      var me = json.author == author;
      var date = typeof(json.time) == 'string' ? parseInt(json.time) : json.time;
      addMessage(json.author, json.message, me ? 'blue' : 'black', new Date(date));
    }
  };

  request.onClose = function(rs) {
    logged = false;
  };

  request.onError = function(rs) {
    content.html($('<p>', {
      text: 'Sorry, but there\'s some problem with your ' + 'socket or the server is down'
    }));
  };

  subSocket = socket.subscribe(request);

  input.keydown(function(e) {
    if (e.keyCode === 13) {
      var msg = $(this).val();

      // First message is always the author's name
      if (author == null) {
        author = msg;
      }

      var json = {
        author: author,
        message: msg
      };

      subSocket.push(jQuery.stringifyJSON(json));
      $(this).val('');


      if (myName === false) {
        myName = msg;
        logged = true;
        status.text(myName + ': ').css('color', 'blue');
        input.removeAttr('disabled').focus();
        subSocket.pushLocal(myName);
      } else {
//        input.attr('disabled', 'disabled');
        addMessage(author, msg, 'blue', new Date);
      }
    }
  });

  function addMessage(author, message, color, datetime) {
    content.append(
        '<p><span style="color:' + color + '">' + author + '</span> @ ' + +(datetime.getHours() < 10 ? '0' + datetime.getHours() : datetime.getHours()) + ':' + (datetime.getMinutes() < 10 ? '0' + datetime.getMinutes() : datetime.getMinutes()) + ': ' + message + '</p>');
  }
});