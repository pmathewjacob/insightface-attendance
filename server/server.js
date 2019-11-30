var hostname = 'localhost';
var port = 3000;

var http = require('http');
var fs = require('fs');
var path = require('path');
var bodyParser = require('body-parser');
var express = require('express');
var app = express();
var multer = require('multer');

var action = require('./action.js');

const upload = multer({dest: __dirname});

const handleError = (err, res) => {
  res
    .status(500)
    .contentType("text/plain")
    .end("Oops! Something went wrong!");
};

app.use(bodyParser());
app.use(bodyParser.json()); // if request body has json data, 
// then convert it to a simpler form to use in javascript

function givefile(req,res,fileUrl){
	var filePath = path.resolve('.'+fileUrl);
	var fileExt = path.extname(filePath);

	fs.exists(filePath, function(exists){
		if(!exists) res.redirect("/");
		else fs.createReadStream(filePath).pipe(res);
	});
}

app.get('/', function(req,res){
	res.redirect('/index.html');
});

app.get('/index.html', function(req,res){
	givefile(req,res,'/index.html');
});

app.get('/register.html', function(req,res){
	givefile(req,res,'/register.html');
});

app.get('/attendance.html', function(req,res){
	givefile(req,res,'/attendance.html');
});

app.get('/*', function(req,res){
	givefile(req,res,req.url);
});

app.post('/register', upload.single("classPhoto"), function(req, res){
	const tempPath = req.file.path;
    const targetPath = path.join(__dirname, "./photo.jpg");

    fs.rename(tempPath, targetPath, err => {
	    if (err) return handleError(err, res);
	    else action.registerNewClass(req, res, function(){
	    	givefile(req,res,'/register.html');
	    });
	});
});

app.post('/assign', function(req, res){
    action.assign(req, res, function(){
    	givefile(req,res,'/assigned.html');
    });
});

app.post('/attendance', upload.single("classPhoto"), function(req, res){
	const tempPath = req.file.path;
    const targetPath = path.join(__dirname, "./photo.jpg");

    fs.rename(tempPath, targetPath, err => {
	    if (err) return handleError(err, res);
	    else action.checkAttendance(req, res, function(){
	    	givefile(req,res,'/attendance.html');
	    });
	});
});

//-------------------------------

app.listen(port, hostname, function(){
	console.log('Server running at http://' + hostname + ':' + port + '/');
}); // start the server and print the status to the console