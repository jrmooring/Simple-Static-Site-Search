 var searchString = location.search.substring(1);
    searchString = searchString.replace('SearchString=', '');
    var re = /&.*/i;
    searchString = searchString.replace(re, '').toLowerCase();
    var results = search(searchString);
    var resultsString = document.createElement('h3');
    if(results.length == 0){
      resultsString.innerText = "Your search - "+searchString.split("+").join(" ")+" - did not match any documents.";
    }
    else if (results.length == 1){
      resultsString.innerText = "Your search - "+searchString.split("+").join(" ")+" - did returned 1 document:";
    }
    else{
      resultsString.innerText = "Your search - "+searchString.split("+").join(" ")+" - returned "+results.length+" documents.";
    }
    document.getElementById("searchResults").appendChild(resultsString);
    results.map(function(x){
      $.get(x, function(response) {
        var doc = document.createElement('div');
        var page = document.createElement('p');
        var link = document.createElement('a');
        doc.innerHTML = response;
        link.href = x;
        link.textContent = doc.getElementsByTagName("title")[0].innerText;
        page.innerHTML+= "<h3>"+link.outerHTML+"</h3>";
        for(i = 0; i < searchString.split("+").length; i++){
          var previewText = "..."+doc.innerText.substring(doc.innerText.toLowerCase().indexOf(searchString.split("+")[i])-70,doc.innerText.toLowerCase().indexOf(searchString.split("+")[i])+70);
          for(j in searchString.split("+")){
            if(previewText.search(searchString.split("+")[j])!=-1){
              i++;
            }
            previewText = previewText.toLowerCase().replace(searchString.split("+")[j],'<span style="font:bold 100% sans-serif;">'+ searchString.split("+")[j] +'</span>');

          }
          page.innerHTML += previewText +"...<br>";;
        }
        page.innerHTML += "<a href='"+x+"'>"+x+"</a>";
        document.getElementById("searchResults").appendChild(page);
      });
});