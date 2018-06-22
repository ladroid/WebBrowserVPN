console.log('hello');
var tr = require('tor-request');
tr.request('https://api.ipify.org', function (err, res, body) {
  if (!err && res.statusCode == 200) {
    console.log("Your public (through Tor) IP is: " + body);
  }
});