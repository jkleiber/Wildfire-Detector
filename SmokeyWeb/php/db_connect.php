<?php
    // Read the configuration file
    include("config.php");

    // Get database credentials
    $host = $cfg['host'];
    $user = $cfg['user'];
    $pass = $cfg['password'];
    $dbname = $cfg['db_name'];
    
    // Attempt to connect to the database using PDO
    try
    {
        // Connect to the database
        $pdo = new PDO('mysql:host=' . $host . ';dbname=' . $dbname, $user, $pass);

        // Set all PDO errors to throw exceptions
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    }
    // Connecting to the database failed, so print an error and give up
    catch(PDOException $e)
    {
        printf("Connection to database failed. %s\n", $e->getMessage());
        exit();
    }
?>