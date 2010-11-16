var smallButtonSelector = 'div#peweproxy_messageboard_container a.__peweproxy_msgboard_button.small';
var bigButtonSelector = 'div#peweproxy_messageboard_container a.__peweproxy_msgboard_button.big';
var renewSmallButton = true;
var messages_per_page = 5;
var actual_from = 0;

var peweproxy_url = 'adaptive-proxy/messageboard_call.html';

var temp = function($) {
    $(document).ready(function(){
        var userPreferences = getUserPreferences();
        if (userPreferences.messageboard_nick == null || userPreferences.messageboard_nick == ""){
            $('#peweproxy_messageboard_nick_info').html("Nemáte nastavené žiadne meno.");
            $('#peweproxy_messageboard_nick span').html("");
            $('#peweproxy_messageboard_change_nick input').val("");
        } else {
            $('#peweproxy_messageboard_nick_info').html("Prispievate pod menom ");
            $('#peweproxy_messageboard_nick span').html(userPreferences.messageboard_nick);
            $('#peweproxy_messageboard_change_nick input').val(userPreferences.messageboard_nick);
        }
        $('#peweproxy_messageboard_nick a').click(function(){
            $('#peweproxy_messageboard_nick').hide();
            $('#peweproxy_messageboard_change_nick').show();
            $('#peweproxy_messageboard_change_nick input').select();
            return false;
        });
        $('#peweproxy_messageboard_text').focus(function(){
            if (this.value=='Sem vložte váš príspevok.') {
                this.value='';
                $(this).removeClass('empty');
            }
        }).blur(function(){
            if (this.value=='') {
                this.value='Sem vložte váš príspevok.';
                $(this).addClass('empty');
            }
        });
        setInterval("peweproxy_getMessages("+actual_from+")",8000);
        $('#peweproxy_messageboard_preloader').ajaxStart(function(){
          $(this).show();
        });
        $('#peweproxy_messageboard_preloader').ajaxStop(function(){
          $(this).hide();
        });
        if (userPreferences.visibility == 1){
            renewSmallButton = false;
            $('#peweproxy_messageboard').show();
            $(bigButtonSelector).addClass('hidden');
            $(smallButtonSelector).addClass('hidden');   
            peweproxy_getMessages(0);
        }
        $(smallButtonSelector).mouseover(function(){
            $(bigButtonSelector).css('background-position','30px').removeClass('hidden').animate({
                'background-position' : '0px'
            }, 'fast', function(){
                $(this).html(getMessageCount());
            });
        //(this).addClass('hidden');
        });
        $(bigButtonSelector).mouseout(function(){
            $(this).html('');
            if (renewSmallButton){
                $(smallButtonSelector).removeClass('hidden');
            }
            $(this).animate({
                'background-position' : '30px'
            }, 'fast', function(){
                $(this).addClass('hidden');
            });
        });
        $(bigButtonSelector).click(function(){
            setShown(true);
            $(this).blur();
            renewSmallButton = false;
            $('#peweproxy_messageboard').hide().removeClass('hidden').fadeIn('fast');
            $(bigButtonSelector).addClass('hidden');
            $(smallButtonSelector).addClass('hidden');     
            peweproxy_getMessages(0);
            return false;
        });
        $('div#peweproxy_messageboard a.__peweproxy_messageboard_closebutton').click(function(){
            setShown(false);
            $(this).blur();
            renewSmallButton = true;
            $(smallButtonSelector).removeClass('hidden');
            $('#peweproxy_messageboard').fadeOut('fast');
            return false;
        });
    });

    $(document).scroll(function(){
        $('#peweproxy_messageboard_container').animate({
            'top':$(document).scrollTop()
        }, 'fast');
    });
} (adaptiveProxyJQuery);

function peweproxy_messageboard_send(){
    var temp = function($) {
        text_element = $('#peweproxy_messageboard_text');
        text = text_element.val();
        nick = $('#peweproxy_messageboard_change_nick input').val();
        if ($.trim(nick) == ""){
            alert("Pred odoslaním príspevku musíte nastaviť meno.");
            return;
        }
        if ($.trim(text) == '' || text == 'Sem vložte váš príspevok.'){
            alert('Správa nesmie byť prázdna.');
            return;
        }
        $.post(peweproxy_url+"?action=addMessage", {
            text: text,
            uid: __peweproxy_uid
        },
        function(response){
            if (response == 'OK'){
                text_element.val('Sem vložte váš príspevok.');
                peweproxy_getMessages(0);
            } else {
                alert("Správu sa nepodarilo pridať. Skúste to znova.")
            }
        });
    } (adaptiveProxyJQuery);
}

function peweproxy_changeNickInputHandleEnter(event){
    if (event.keyCode == 13){
        peweproxy_messageboardChangeNick()
    }
}

function peweproxy_messageboardChangeNick(){
    var temp = function($) {
        nick = $('#peweproxy_messageboard_change_nick input').val();
        if ($.trim(nick) == "") {
            alert("Meno nesmie byť prázdne.");
            return;
        }
        original_nick = $('#peweproxy_messageboard_nick span').html();
        if (nick == original_nick){
            $('#peweproxy_messageboard_change_nick').hide();
            $('#peweproxy_messageboard_nick').show();
            return;
        }
        $.post(peweproxy_url+"?action=setMessageboardNick", {
            nick: nick,
            uid: __peweproxy_uid
        }, function(response){
            if (response == 'OK'){
                peweproxy_getMessages(actual_from);
                $('#peweproxy_messageboard_nick_info').html("Prispievate pod menom ");
                $('#peweproxy_messageboard_nick > span').html(nick);
                $('#peweproxy_messageboard_change_nick').hide();
                $('#peweproxy_messageboard_nick').show();
            } else if (response == 'NICK_EXISTS'){
                alert("Meno \""+nick +"\" už je obsadené, zvoľte prosím iné.")
            } else {
                alert("Meno sa nepodarilo uložiť, skúste to ešte raz.");
            }
        });
    } (adaptiveProxyJQuery);
}

function peweproxy_getMessages(from){
    var temp = function($) {
        if ($('#peweproxy_messageboard').css('display') == 'block'){
          $.post(peweproxy_url+"?action=getMessages", {
              from : from,
              count: messages_per_page,
              decorateLinks: 1
          }, function(response){
              actual_from = from;
              response = eval('('+response+')');
              $('#peweproxy_messageboard div.lister').html(peweproxy_generateLister(from, response.total));
              messages = new String();
              for (var message in response.messages){
                  if (response.messages[message].text == undefined) continue;
                  messages += '<div class="block"><p>'+response.messages[message].text+'</p><span class="author">'+response.messages[message].nick+' | '+response.messages[message].time+'</span></div>';
              }
              $('#peweproxy_messages').html(messages);                
          });
        }
    } (adaptiveProxyJQuery);
}

function peweproxy_generateLister(from, total){
    from++;
    var page = Math.ceil(from / messages_per_page);
    var pages = Math.ceil (total / messages_per_page);
    if (page < 1) page = 1;

    var start_from = page - 2;


    if (pages - page < 2) start_from = page - 3;
    if (page == pages) start_from = page - 4;
    if (start_from < 1) start_from = 1;
    if (pages <= 5) start_from = 1;
	
    var lister = new Array();

    var j = 0;
    if (page > 3 && pages > 5) {
        lister[j] = '<a href="#" onclick="peweproxy_getMessages(0); return false;" title="Prvá strana">&laquo;</a>';
        j++;
    }

    var last;
    for (var i = start_from; i < start_from + 5 && i <= pages; i++){
        active = page == i ? ' class="active"' : '';
        lister[j] = '<a href="#" onclick="peweproxy_getMessages('+((i-1)*messages_per_page)+'); return false;"'+active+'>'+i+'</a>';
        last = i;
        j++;
    }

    if (last < pages && pages > 5) {
        lister[j] = '<a href="#" onclick="peweproxy_getMessages('+((pages-1)*messages_per_page)+'); return false;" title="Posledná strana">&raquo;</a>';
    }

    return lister.join('|');
}

function setShown(shown){
    var temp = function($) {
        shown = shown ? 1 : 0;
        $.post(peweproxy_url+'?action=setShown', {
            shown: shown,
            uid: __peweproxy_uid
        });

    } (adaptiveProxyJQuery);
}

function getMessageCount(){
    var retVal;
    var temp = function($) {
        retVal = $.ajax({
            async: false,
            url: peweproxy_url+"?action=getMessageCount",
            type: 'POST',
            data : {
                action : 'getMessageCount'
            }
        }).responseText;
    } (adaptiveProxyJQuery);
    return retVal;
}

function getUserPreferences(){
    var retVal;
    var temp = function($) {
        retVal = $.ajax({
            async: false,
            url: peweproxy_url+"?action=getUserPreferences",
            data: {
                uid: __peweproxy_uid
            },
            type: 'POST'
        }).responseText;
    } (adaptiveProxyJQuery);
    return eval('('+retVal+')');
}
