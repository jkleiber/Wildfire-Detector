<?php
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
            echo $e->getMessage();
        }
    }
?>