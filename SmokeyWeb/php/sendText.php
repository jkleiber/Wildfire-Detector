<?php

require __DIR__ . '/twilio-php/src/Twilio/autoload.php';
use Twilio\Rest\Client;

function sendText($lat, $lng)
{
    $gMaps = 'http://maps.google.com/?q=' . $lat . ',' . $lng;

	// Connect to the database
    require("db_connect.php");
    require("config.php");

	// Get list of phone numbers
	// TODO: Create a table that has phone numbers and their names to it
    $query_users = "SELECT * FROM users";

	// Attempt to get the user information
    try
    {
		// Turns the query into something I can use
        $users = $pdo->query($query_users);
    }
    // Couldn't get the users, so print an error
    catch(PDOException $e)
    {
        // Print error
        printf("%s\n", $e->getMessage());
        // Exit
        exit();
    }

    // Get Twilio information from config file
	$account_sid = $cfg['twilio_account_id'];
    $auth_token = $cfg['twilio_auth_token'];
    $twilio_number = $cfg['twilio_phone_number'];

	// Create a twilio client
    $client = new Client($account_sid, $auth_token);

    // Send a message to all the subscribed users
	while($user = $users->fetch())
	{
        // Get user name and phone number to form the SMS message
        $name = $user['name'];
        $message_body = "Be careful " . $name . "! there was a fire detected in your area. " . $gMaps;

		$client->messages->create(
			$user['phone_number'],
			array(
				'from' => $twilio_number,
				'body' => $message_body
			)
        );
	}
}