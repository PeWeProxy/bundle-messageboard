var peweproxy_url_keywords = 'adaptive-proxy/key_words_call.html'

var temp = function($) {
	$(document).ready(function(){
		$('div#peweproxy_icon_banner a.__peweproxy_keywords_button').click(function(){
			$(this).blur();
			renewSmallButton = false;
			$('#peweproxy_keywords').hide().removeClass('hidden').fadeIn('fast');
			$(peweproxy_addonIconBannerSelector).addClass('hidden');
			$(smallButtonSelector).addClass('hidden');
			peweproxy_get_keywords();
			return false;
		});
		$('div#peweproxy_keywords a.__peweproxy_keywords_closebutton').click(function(){
			$(this).blur();
			renewSmallButton = true;
			$(smallButtonSelector).removeClass('hidden');
			$('#peweproxy_keywords').fadeOut('fast');
			return false;
		});
		$('form#peweproxy_keywords_add_form').submit(function(){
			post_data = $(this).serialize();
			$.post(peweproxy_url_keywords+'?action=addKeyWord',post_data+'&checksum='+_ap_checksum,function(data){
				response = $.trim(data);
				if (response == 'OK'){
					peweproxy_get_keywords();
					$('input#peweproxy_keywords_add_term').val('Slovo').focus();
					$('input#peweproxy_keywords_add_type').val('Typ');
					$('input#peweproxy_keywords_add_relevance').val('Váha');
				} else {
					alert('Pridávanie sa nepodarilo, skontrolujte správnosť údajov a akciu opakujte.');
				}
			})
			return false;
		});
	});
} (adaptiveProxyJQuery);

function peweproxy_keywords_edit(id){
	var temp = function($) {
		$('div#peweproxy_keywords_content table tr.row'+id+' span.static').hide();
		$('div#peweproxy_keywords_content table tr.row'+id+' span.editable').show();
	} (adaptiveProxyJQuery);
}

function peweproxy_keywords_delete(id){
	var temp = function($) {
		if (confirm('Skutočne odstrániť kľúčové slovo '+$('div#peweproxy_keywords_content table tr.row'+id+' span.term').html()+'?')){
			$.post(peweproxy_url_keywords+'?action=removeKeyWord',{
				id:id
			},function(data){
				response = $.trim(data);
				if (response == 'OK'){
					peweproxy_get_keywords();
				} else {
					alert('Pri mazaní kľúčového slova nastala chyba.');
				}
			});
		}
	} (adaptiveProxyJQuery);
}

function peweproxy_keywords_save(id){
	var temp = function($) {
		table = $('div#peweproxy_keywords_content table');
		post_data = {
			id:id,
			term:table.find("tr.row"+id+" input.term").val(),
			relevance: table.find("tr.row"+id+" input.relevance").val(),
			type: table.find("tr.row"+id+" input.type").val()
		}
		$.post(peweproxy_url_keywords+'?action=editKeyWord',post_data, function(data){
			response = $.trim(data);
			if (response == 'OK'){
				peweproxy_get_keywords()
			} else if (response == 'TERM_EXISTS'){
				alert('Kľúčové slovo '+post_data.term+' už existuje');
			} else {
				alert('Pri ukladaní kľúčového slova nastala chyba. Skonstrolujte správnosť údajov.');
			}
		});
	} (adaptiveProxyJQuery);
}

function peweproxy_get_keywords(){
	var template = '<tr class="row[:id:] keyword_row">'+
						'<td style="width: 179px">'+
							'<span class="static term">[:term:]</span>'+
							'<span class="editable"><input type="text" class="term" value="[:term:]"/></span>'+
						'</td>'+
						'<td style="width: 56px">'+
							'<span class="static type">[:type:]</span>'+
							'<span class="editable"><input type="text" class="type" value="[:type:]"/></span>'+
						'</td>'+
						'<td style="width: 43px">'+
							'<span class="static relevance">[:relevance:]</span>'+
							'<span class="editable"><input type="text" class="relevance" value="[:relevance:]"/></span>'+
						'</td>'+
						'<td style="width: 57px">[:source:]</td>'+
						'<td style="width: 46px" class="buttons">'+
							'<span class="static">'+
								'<a href="#" onclick="peweproxy_keywords_edit([:id:]); return false"><img src="'+peweproxy_keywords_htdocs_dir+'/edit_icon.png" alt="edit" /></a>'+
							'</span>'+
							'<span class="editable">'+
								'<a href="#" onclick="peweproxy_keywords_save([:id:]); return false;"><img src="'+peweproxy_keywords_htdocs_dir+'/ok_icon.png" alt="save" /></a>'+
							'</span>'+
							'<a href="#" onclick="peweproxy_keywords_delete([:id:]); return false;"><img src="'+peweproxy_keywords_htdocs_dir+'/delete_icon.png" alt="delete" /></a>'+
						'</td>'+
					'</tr>';
	var temp = function($) {
		$.post(peweproxy_url_keywords+'?action=getKeyWords','checksum='+_ap_checksum,function(data){
			keywords = eval('('+data+')');
			table = $('div#peweproxy_keywords_content table');
			table.find("tr.keyword_row").remove();
			for (var keyword in keywords.keywords){
			  if (keywords.keywords[keyword].term == undefined) continue;
				row = template.replace(/\[:id:\]/g, keywords.keywords[keyword].id);
				row = row.replace(/\[:term:\]/g, keywords.keywords[keyword].term);
				row = row.replace(/\[:type:\]/g, keywords.keywords[keyword].type);
				row = row.replace(/\[:relevance:\]/g, keywords.keywords[keyword].relevance);
				row = row.replace(/\[:source:\]/g, keywords.keywords[keyword].source);
				table.append(row);
			}
			$('div#peweproxy_keywords_content table tr:even').addClass('odd');
		})
	} (adaptiveProxyJQuery);
}