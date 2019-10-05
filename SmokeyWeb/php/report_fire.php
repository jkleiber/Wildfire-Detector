<?php
    //Connect to the database
    require_once("db_connect.php");

    // Ensure all required fields are filled out
    if(isset($_POST['latitude']) 
    && isset($_POST['longitude']))
    {
        // Get report information
        $lat = $_POST['latitude'];
        $lon = $_POST['longitude'];

        // Form a point from the latitude and longitude
        $point = "POINT(". $pdo->quote($lat) . ", " . $pdo->quote($lon) .")";

        // Form the MySQL query
        $query = "INSERT INTO fire_reports (latitude, longitude, position) 
                VALUES(" . $pdo->quote($lat) . ", " . $pdo->quote($lon) ."," . $point . ")";

        // Attempt to insert data into database
        try
        {
            // Execute the database query for inserting the data
            $pdo->exec($query);
        }
        // Inserting data failed, so print an error
        catch(PDOException $e)
        {
            // Print error
            printf("%s\n", $e->getMessage());

            // Respond with error
            echo json_encode(array('message' => 'ERROR: '.  $e->getMessage()));
            
            // Exit the script
            exit();
        }

        // Respond to request with success
        echo json_encode(array('message' => 'SUCCESS: Fire added to database successfully'));
        exit();
    }
    
    // Debugging output
    require_once("debug.php");

    // Respond to request with error due to invalid post
    $post_contents = arrayToString($_POST, "");
    echo json_encode(array('message' => 'ERROR: Incorrect POST format', 'received' => $post_contents));
?>