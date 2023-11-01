var exec = require('cordova/exec');

exports.takePictures = function (arg0, success, error) {
    exec(success, error, 'ScoplanCamera', 'takePictures', []);
};
