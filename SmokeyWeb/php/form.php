<?php
    // Connect to the database
    require_once("db_connect.php");

    // Get the name and number of the user
	$fullName = $_POST['fullName'];
	$phoneNumber = $_POST['phoneNumber'];

    // Form a statement to execute
    $query = "INSERT INTO users (name, phone_number) VALUES (" . $pdo->quote($fullName) . ", " . $pdo->quote($phoneNumber) . ")";

    // Insert the user information
    try
    {
        $pdo->exec($query);
    }
    // Inserting data failed, so print an error
    catch(PDOException $e)
    {
        // Print error
        printf("%s\n", $e->getMessage());

        ?>

        Something went wrong, <a href="/index.html"> head back to the homepage </a> and try again.

        <?php

        // Exit the script
        exit();
    }

    // Redirect if successful
    header("Location: https://www.smokey.pw");
?>
