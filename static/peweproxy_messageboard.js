peweproxy.register_module('messageboard', function($) {

	var smallButtonSelector = 'div#peweproxy_addons_container a.__peweproxy_addons_button';
	var peweproxy_addonIconBannerSelector = 'div#peweproxy_icon_banner';
	var messageboardButonSelector = 'div#peweproxy_icon_banner a.__peweproxy_msgboard_button';
	var renewSmallButtontonSelector = 'div#peweproxy_icon_banner a.__peweproxy_msgboard_button';
	var renewSmallButton = true;
	var messages_per_page = 5;
	var actual_from = 0;
	
	var peweproxy_url_messageboard = 'adaptive-proxy/messageboard_call.html'
	
	    $(document).ready(function(){
	    	peweproxy.on_uid_ready(function(){
	        var userPreferences = $.parseJSON($.ajax({
	            async: false,
	            url: peweproxy_url_messageboard+"?action=getUserPreferences",
	            data: {
	                uid: peweproxy.uid
	            },
	            type: 'POST'
	        }).responseText);
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
	        setInterval("peweproxy.modules.messageboard.getMessages("+actual_from+")",8000);
	        $('#peweproxy_messageboard_preloader').ajaxStart(function(){
	          $(this).show();
	        });
	        $('#peweproxy_messageboard_preloader').ajaxStop(function(){
	          $(this).hide();
	        });
	        if (userPreferences.visibility == 1){
	            renewSmallButton = false;
	            $('#peweproxy_messageboard').show();
	            $(peweproxy_addonIconBannerSelector).addClass('hidden');
	            $(smallButtonSelector).addClass('hidden');   
	            getMessages(0);
	        }
	        $(messageboardButonSelector).click(function(){
	            setShown(true);
	            $(this).blur();
	            renewSmallButton = false;
	            $('#peweproxy_messageboard').hide().removeClass('hidden').fadeIn('fast');
	            $(peweproxy_addonIconBannerSelector).addClass('hidden');
	            $(smallButtonSelector).addClass('hidden');
	            getMessages(0);
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
	    });
	
	this.send = function(){
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
	        $.post(peweproxy_url_messageboard+"?action=addMessage", {
	            text: text,
	            uid: peweproxy.uid
	        },
	        function(response){
	            if (response == 'OK'){
	                text_element.val('Sem vložte váš príspevok.');
	                getMessages(0);
	            } else {
	                alert("Správu sa nepodarilo pridať. Skúste to znova.")
	            }
	        });
	}
	
	var send = this.send;
	
	this.changeNickInputHandleEnter = function(event){
	    if (event.keyCode == 13){
	        this.messageboardChangeNick()
	    }
	}
	
	this.messageboardChangeNick = function(){
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
	        $.post(peweproxy_url_messageboard+"?action=setMessageboardNick", {
	            nick: nick,
	            uid: peweproxy.uid
	        }, function(response){
	            if (response == 'OK'){
	                getMessages(actual_from);
	                $('#peweproxy_messageboard_nick_info').html("Prispievate pod menom ");
	                $('#peweproxy_messageboard_nick > span').html(nick);
	                $('#peweproxy_messageboard_change_nick').hide();
	                $('#peweproxy_messageboard_nick').show();
	                $('#messageboard_nick .__peweproxy_preference_table_value .__peweproxy_preference_row_display').text(nick);
	                $('#messageboard_nick .__peweproxy_preference_table_value .__peweproxy_preference_row_updating input').val(nick);
	            } else if (response == 'NICK_EXISTS'){
	                alert("Meno \""+nick +"\" už je obsadené, zvoľte prosím iné.")
	            } else {
	                alert("Meno sa nepodarilo uložiť, skúste to ešte raz.");
	            }
	        });
	}
	
	this.getMessages = function(from){
	        if ($('#peweproxy_messageboard').css('display') == 'block'){
	          $.post(peweproxy_url_messageboard+"?action=getMessages", {
	              from : from,
	              decorateLinks: 1
	          }, function(response){
	              actual_from = from;
	              response = $.parseJSON(response);
	              $('#peweproxy_messageboard div.lister').html(generateLister(from, response.total));
	              messages = new String();
	              for (var message in response.messages){
	                  if (response.messages[message].text == undefined) continue;
	                  messages += '<div class="block"><p>'+response.messages[message].text+'</p><span class="author">'+response.messages[message].nick+' | '+response.messages[message].time+'</span></div>';
	              }
	              $('#peweproxy_messages').html(messages);                
	          });
	        }
	}
	
	var getMessages = this.getMessages;
	
	var generateLister = function(from, total){
		messages_per_page = $.trim($('#messageboard_count .__peweproxy_preference_table_value .__peweproxy_preference_row_display').text());
		messages_per_page = messages_per_page == "" ? 5 : messages_per_page;
	
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
	        lister[j] = '<a href="#" onclick="peweproxy.modules.messageboard.getMessages(0); return false;" title="Prvá strana">&laquo;</a>';
	        j++;
	    }
	
	    var last;
	    for (var i = start_from; i < start_from + 5 && i <= pages; i++){
	        active = page == i ? ' class="active"' : '';
	        lister[j] = '<a href="#" onclick="peweproxy.modules.messageboard.getMessages('+((i-1)*messages_per_page)+'); return false;"'+active+'>'+i+'</a>';
	        last = i;
	        j++;
	    }
	
	    if (last < pages && pages > 5) {
	        lister[j] = '<a href="#" onclick="peweproxy.modules.messageboard.getMessages('+((pages-1)*messages_per_page)+'); return false;" title="Posledná strana">&raquo;</a>';
	    }
	
	    return lister.join('|');
	}
	
	var setShown = function(shown){
	        shown = shown ? 1 : 0;
	        $.post(peweproxy_url_messageboard+'?action=setShown', {
	            shown: shown,
	            uid: peweproxy.uid
	        });
	
	}
	
	var getMessageCount = function(){
	    var retVal;
	        retVal = $.ajax({
	            async: false,
	            url: peweproxy_url_messageboard+"?action=getMessageCount",
	            type: 'POST',
	            data : {
	                action : 'getMessageCount'
	            }
	        }).responseText;
	    return retVal;
	}
	
	this.getUserPreferences = function(){
	    var retVal;
	        retVal = $.ajax({
	            async: false,
	            url: peweproxy_url_messageboard+"?action=getUserPreferences",
	            data: {
	                uid: peweproxy.uid
	            },
	            type: 'POST'
	        }).responseText;
	    return $.parseJSON(retVal);
	}

	this.refreshNick = function(){
		var new_nick = $.trim($("#messageboard_nick .__peweproxy_preference_table_value .__peweproxy_preference_row_display").text());
		$("#peweproxy_messageboard_change_nick input").val(new_nick);
		$("#peweproxy_messageboard_nick span").text(new_nick);
	}

});