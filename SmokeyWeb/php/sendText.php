<?php
// Get the PHP helper library from twilio.com/docs/php/install
require_once '/path/to/vendor/autoload.php';
// Loads the library
use Twilio\Rest\Client;
function sendText($lat, $lng)
{
	//$gMaps = http://maps.google.com/?q=<$lat>,<$lng>;

	// Connect to the database
    require("db_connect.php");

	// Get list of phone numbers
	// TODO: Create a table that has phone numbers and their names to it
	$query_users = "SELECT phone_number, name FROM users";

	// Attempt to get the reports
    try
    {
		// Turns the query into something I can use
        $users = $pdo->query($queryusers);
    }
    // Couldn't get the users, so print an error
    catch(PDOException $e)
    {
        // Print error
        printf("%s\n", $e->getMessage());
        // Exit
        exit();
    }

	$account_sid = 'AC45f9537e089f4eb98e0c16e68f17320a';
	$auth_token = '4cd3545dc2e9894d16da7d617b2aa8e5';

	// A Twilio number you own with SMS capabilities
	$twilio_number = "+14053670830";
	$client = new Client($account_sid, $auth_token);

	while($user = $users->fetch())
	{
		$name = $users['name'];
		$client->messages->create(
			// Where to send a text message
			$user['phone_number'],
			array(
				'from' => $twilio_number,
				'body' => "Be careful {$name}! there was a fire detected in your area. {$gMaps}"
			)
		);
	}
}