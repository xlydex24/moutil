// ==UserScript==
// @name        反和谐
// @version      0.1
// @description     直接替换掉原来的r18mask图片
// @match        https://hanime1.me/*
//@require        http://code.jquery.com/jquery-1.11.0.min.js
// @namespace https://greasyfork.org/users/139007
// ==/UserScript==

$(setInterval(function(){
        var a=$("img");
        a.each(function(){

            var onerror=$(this).attr("onerror");
            if(onerror==undefined){
                var src=$(this).attr("src");
                var result=src;
                $(this).attr("src",'https://pic1.xuehuaimg.com/proxy/'+result);
                $(this).attr("onerror",'javascript:this.src=\'https://search.pstatic.net/common?src='+result+'\'');
            }
        });

    //alert("script over");
},200));