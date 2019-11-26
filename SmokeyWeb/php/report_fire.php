
<?php
    $post_data = json_decode(file_get_contents('php://input'), true);

    // Ensure all required fields are filled out
    if(isset($post_data['latitude'])
    && isset($post_data['longitude']))
    {
        // Connect to the database
        require_once("db_connect.php");

        // Get helper functions for fire associations
        require_once("report_helper.php");
		require_once("sendText.php");

        // Get report information
        $lat = $post_data['latitude'];
        $lon = $post_data['longitude'];

        // Form a point from the latitude and longitude
        $point = "POINT(". $pdo->quote($lat) . ", " . $pdo->quote($lon) .")";

        // Find the nearest active fire to this one
        $fire_id = getNearestActiveFire($lat, $lon);
        $fire_order = 1;

        // If no active fires close to this one were found, start an active fire from this report
        if($fire_id == 0)
        {
            $fire_query = "INSERT INTO active_fires (initial_pos, fires, num_reports) VALUES (" . $point . ", MULTIPOINT(" . $point . "), 1)";

            // Add the active fire
            try {
                $pdo->exec($fire_query);
            }
            catch (PDOException $e)
            {
                // Print error
                echo "New Active Fire ERR:" . $e->getMessage() . "\n";

                // Indicate this fire report was going to start an active fire, but then the insert failed
                $fire_id = -1;
            }

            // Get the ID of the fire we just created
            $active_fire_query = "SELECT id FROM active_fires WHERE initial_pos =" . $point;
            try {
                $fire_id_row = $pdo->query($active_fire_query);

                $fire_id = $fire_id_row->fetch()['id'];
            }
            catch(PDOException $e)
            {
                // Print error
                echo "New Active Fire ID ERR: " . $e->getMessage() . "\n";

                // Indicate this fire report was going to start an active fire, but then the lookup failed
                $fire_id = -2;
            }
        }
        // Otherwise update the active fire this is associated with
        else
        {
            // Find the number of fires in the active fire
            $num_fires_query = "SELECT * FROM fire_reports WHERE active_fire_id = " . $pdo->quote($fire_id);
            try{
                $num_fires = $pdo->query($num_fires_query);

                $fire_order = $num_fires->rowCount() + 1;
            }
            catch(PDOException $e)
            {
                // Print error
                echo "Active fire update ERR: " . $e->getMessage() . "\n";
            }
        }

        // Form the MySQL query
        $query = "INSERT INTO fire_reports (latitude, longitude, position, active_fire_id, active_fire_order)
                VALUES(" . $pdo->quote($lat) . ", " . $pdo->quote($lon) ."," . $point . "," . $pdo->quote($fire_id) . "," . $pdo->quote($fire_order) . ")";

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
		
		// send texts
		sendText($lat, $lon);

        // Respond to request with success
        echo json_encode(array('message' => 'SUCCESS: Fire added to database successfully'));
        exit();
    }

    // Debugging output
    require_once("debug.php");

    // Respond to request with error due to invalid post
    echo json_encode(array('message' => 'ERROR: Incorrect POST format'));
?>