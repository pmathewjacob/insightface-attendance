var hostname = "localhost";
var port = "3000";

function httpGet(theUrl){
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", theUrl, false ); // false for synchronous request
    xmlHttp.send( null );
    return xmlHttp.responseText;
}

document.getElementById("register").setAttribute("action","http://"+hostname+":"+port+"/register");
document.getElementById("attendance").setAttribute("action","http://"+hostname+":"+port+"/attendance");