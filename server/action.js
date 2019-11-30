var exec = require('child_process').exec;
var fs = require('fs');

function createSourceJson(callback){
	fs.exists('source.json', function (exists) {
        if(exists) callback();
        else fs.writeFile('source.json', 
        "{" + "\n" +
		    "\"students\": {}," + "\n" +
		    "\"num_pics\": 0," + "\n" +
		    "\"faces_path\": \"./faces/\"" + "\n" +
		"}", 
		{ flag: 'wx' }, function (err) { 
        	if(err) throw err;
            callback();
        });
    });
}

exports.registerNewClass = function(req, res, callback){
	createSourceJson(function(){
		fs.readFile('source.json', (err, data) => {
		    if (err) throw err;
		    var old_source = JSON.parse(data);
		    var old_num_pics = old_source.num_pics;
			exec("python ../src/api/demo.py -a --source-image ./photo.jpg --model ../recognition/models/vargfacenet-arcface-retina/model,1", function(err, stdout, stderr){
				if(err) throw err;
				fs.readFile('source.json', (err, data) => {
				    if (err) throw err;
				    var new_source = JSON.parse(data);
				    var new_num_pics = new_source.num_pics;
				    if(old_num_pics == new_num_pics){
				    	fs.writeFile("register.html",
				    		"<!DOCTYPE html>" + "\n" +
							"<html>" + "\n" +
							"<head> </head>" + "\n" +
							"<body>" + "\n" +
							"<script type=\"text/javascript\">" + "\n" +
							    "var hostname = \"localhost\";" + "\n" +
							    "var port = \"3000\";" + "\n" +
							    "alert(\"There is no face detected in this picture.\");" + "\n" +
							    "window.location.replace(\"http://\"+hostname+\":\"+port+\"/\");" + "\n" +
							"</script>" + "\n" +
							"</body>" + "\n" +
							"</html>",
				    		function(err) {
						    if(err) throw err;
						    callback();
						}); 
				    }
				    else{
				    	var content = 
					    "<!DOCTYPE html>" + "\n" +
						"<html>" + "\n" +
							"<head>" + "\n" +
								"<title> introAI - Register class</title>" + "\n" +
								"<link href = \"css/register.css\" rel = \"stylesheet\" type = \"text/css\">" + "\n" +
								"<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js\"></script>" + "\n" +
							"</head>" + "\n" +

							"<body>" + "\n" +
								"<div id=\"wrapper\" class=\"wrapper\">" + "\n" +
									//"<img id=\"background\" class=\"background\" src=\"media/background.jpg\">" + "\n" +
									"<div id=\"main-content\" class=\"main-content\">" + "\n" +
										"<h1>Fill in student information:</h1>" + "\n" +
										"<form id=\"assign\" method=\"post\" class=\"assign\">" + "\n";
					    
					    var cnt = 0;

					    for(var i = old_num_pics + 1; i <= new_num_pics; i++){
					    	content += 
					    	"<div>" + "\n" +
								"<img class=\"student_img\" src=\"faces/" + i + ".png\">" + "\n" +
								"<div class=\"input-div\">" + "\n" +
									"<input type=\"text\" class=\"typing-box\" name=\"name_" + i + "\" placeholder=\"Name\">" + "\n" +
								"</div>" + "\n" +
								"<div class=\"input-div\">" + "\n" +
									"<input type=\"text\" class=\"typing-box\" name=\"studentId_" + i + "\" placeholder=\"Student ID\">" + "\n" +
								"</div>" + "\n" +
							"</div>" + "\n";

					    	cnt++;
					    	if(cnt == new_num_pics - old_num_pics){
					    		content +=
											    	"<div class=\"assign-button-frame\">" + "\n" +
														"<input type=\"submit\" value=\"Submit\">" + "\n" +
													"</div>" + "\n" +
												"</form>" + "\n" +
											"</div>" + "\n" +
										"</div>" + "\n" +
										"<script src=\"js/register.js\"></script>" + "\n" +
									"</body>" + "\n" +
								"</html>";

					    		fs.writeFile("register.html", content, function(err) {
								    if(err) throw err;
								    callback();
								}); 
					    	}
					    }
				    }
				});
			});
		});
	});
}

exports.assign = function(req, res, callback){
	createSourceJson(function(){
		fs.readFile('source.json', (err, data) => {
		    if (err) throw err;
		    var num_pics = JSON.parse(data).num_pics;
		    if(num_pics == 0) callback();
		    else{
		    	var cnt = 0;
			    var content = "";
			    for(var i = 1; i <= num_pics; i++){
			    	if(req.body["name_" + i.toString()] !== undefined && req.body["name_" + i.toString()] != "" 
			    		&& req.body["studentId_" + i.toString()] !== undefined && req.body["studentId_" + i.toString()] != "")
			    		content += "y\n" + 
			    					req.body["name_" + i.toString()] + "\n" + 
			    					req.body["studentId_" + i.toString()] + "\n" +
			    					i.toString() + "\n";

			    	cnt++;
			    	if(cnt == num_pics){
			    		content += "n";
			    		fs.writeFile("input.txt", content, function(err) {
						    if(err) throw err;
						    exec("python ../src/api/demo.py -e --model ../recognition/models/vargfacenet-arcface-retina/model,1 < input.txt", function(err, stdout, stderr){
				    			if(err) throw err;
				    			exec("rm input.txt", function(err, stdout, stderr){
				    				if(err) throw err;
				    				callback();
				    			});
				    		});
						}); 
			    	}
			    }
		    }
		});
	});
}

exports.checkAttendance = function(req, res, callback){
	createSourceJson(function(){
		exec("python ../src/api/demo.py -c --source-image ./photo.jpg --model ../recognition/models/vargfacenet-arcface-retina/model,1", function(err, stdout, stderr){
			if(err) throw err;
			var content = 
			"<!DOCTYPE html>" + "\n" +
			"<html>" + "\n" +
				"<head>" + "\n" +
					"<title> introAI - Register class</title>" + "\n" +
					"<link href = \"css/attendance.css\" rel = \"stylesheet\" type = \"text/css\">" + "\n" +
					"<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js\"></script>" + "\n" +
				"</head>" + "\n" +

				"<body>" + "\n" +
					"<div id=\"wrapper\" class=\"wrapper\">" + "\n" +
						"<div id=\"main-content\" class=\"main-content\">" + "\n" +
							"<h1>Students detected:</h1>" + "\n" +
							stdout.replace(/\n/g, "</br>\n") +
						"</div>" + "\n" +
					"</div>" + "\n" +
					"<script src=\"js/attendance.js\"></script>" + "\n" +
				"</body>" + "\n" +
			"</html>";

			fs.writeFile("attendance.html", content, function(err) {
			    if(err) throw err;
			    callback();
			}); 
		});
	});
}