<?php
function toRadians($deg)
{
    return $deg * pi() / 180.0;
}

// Get distance between two points using the haversine formula
function getDistance($lat1, $lon1, $lat2, $lon2)
{
    $radius = 6371000.0;  // Earth radius, 6371 km

    // Convert lat/lon pairs to radians
    $lat1 = toRadians($lat1);
    $lon1 = toRadians($lon1);
    $lat2 = toRadians($lat2);
    $lon2 = toRadians($lon2);

    // Calculate 'a' param in haversine formula
    $a = pow(sin(($lat2 - $lat1) / 2.0),2) + cos($lat1) * cos($lat2) * pow(sin(($lon2 - $lon1)/2), 2);

    // Use 'a' to calculate 'c'
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

    // Find distance using 'c' and the earth's radius
    $dist = $radius * $c;

    // Return the distance
    return $dist;
}

function getActiveReports()
{
    // Connect to the database
    require("db_connect.php");

    // Form a query to get all the fire reports
    $query = "SELECT * FROM fire_reports WHERE active_fire_id > 0";

    // Attempt to get the reports
    try
    {
        $reports = $pdo->query($query);
    }
    // Couldn't get the reports, so print an error
    catch(PDOException $e)
    {
        // Print error
        echo $e->getMessage();
    }

    return $reports;
}

// Helper function for associating reports with active fires
function getNearestActiveFire($lat, $lon)
{
    // Constants for distance association
    $max_same_fire_dist = 200; // meters

    // Get the active fire reports only
    $reports = getActiveReports();

    // Set the current active fire
    $active_fire_id = 0;

    // Go through each report and see if this report is close enough
    while($report = $reports->fetch())
    {
        // Get the distance to the selected report
        $dist = getDistance($lat, $lon, $report["latitude"], $report["longitude"]);

        // If this fire is close enough to the report, consider it part of the same active fire
        // TODO: handle case where two fires combine together to become one big fire
        if($dist <= $max_same_fire_dist)
        {
            $active_fire_id = $report["active_fire_id"];
            break;
        }
    }

    return $active_fire_id;
}

?>