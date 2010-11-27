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
	});
} (adaptiveProxyJQuery);

function peweproxy_keywords_edit(id){
	var temp = function($) {
		$('div#peweproxy_keywords_content table tr.row'+id+' span.static').hide();
		$('div#peweproxy_keywords_content table tr.row'+id+' span.editable').show();
	} (adaptiveProxyJQuery);
}

function peweproxy_keywords_save(id){
	var temp = function($) {
		$('div#peweproxy_keywords_content table tr.row'+id+' span.editable').hide();
		$('div#peweproxy_keywords_content table tr.row'+id+' span.static').show();
	} (adaptiveProxyJQuery);
}

function peweproxy_get_keywords(){
	var template = '<tr class="row[:id:]">'+
						'<td style="width: 179px">'+
							'<span class="static">[:term:]</span>'+
							'<span class="editable"><input type="text" class="term" value="[:term:]"/></span>'+
						'</td>'+
						'<td style="width: 56px">'+
							'<span class="static">[:type:]</span>'+
							'<span class="editable"><input type="text" class="type" value="[:type:]"/></span>'+
						'</td>'+
						'<td style="width: 43px">'+
							'<span class="static">[:relevance:]</span>'+
							'<span class="editable"><input type="text" class="relevance" value="[:relevance:]"/></span>'+
						'</td>'+
						'<td style="width: 57px">[:source:]</td>'+
						'<td style="width: 46px">'+
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
		$.post(peweproxy_url+'?action=getKeyWords','data=1',function(data){
			keywords = eval('('+data+')');
			table = $('div#peweproxy_keywords_content table');
			for (var keyword in keywords.keywords){
				row = template.replace(/\[:id:\]/g, keywords.keywords[keyword].id);
				row = row.replace(/\[:term:\]/g, keywords.keywords[keyword].term);
				row = row.replace(/\[:type:\]/g, keywords.keywords[keyword].type);
				row = row.replace(/\[:relevance:\]/g, keywords.keywords[keyword].relevance);
				row = row.replace(/\[:source:\]/g, keywords.keywords[keyword].source);
				table.append(row);
			}
		})
	} (adaptiveProxyJQuery);
}