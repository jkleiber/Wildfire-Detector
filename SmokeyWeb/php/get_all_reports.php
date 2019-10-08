<?php
    function getAllReports()
    {
        // Connect to the database
        require_once("db_connect.php");

        // Form a query to get all the fire reports
        $query = "SELECT * FROM fire_reports";

        // Attempt to get the reports
        try
        {
            $reports = $pdo->query($query);
        }
        // Couldn't get the reports, so print an error
        catch(PDOException $e)
        {
            // Print error
            printf("%s\n", $e->getMessage());

            // Exit
            exit();
        }

        return $reports;
    }

    $reports = getAllReports();

    // Set up a list to put the reports into
    $reports_list = [];

    // Collect data from each report and compile it into a JSON packet
    while($report = $reports->fetch())
    {
        $reports_list[] = json_encode(array("timestamp"=>$report['timestamp'], "lat"=>$report['latitude'], "lon"=>$report['longitude']));
    }

    echo json_encode($reports_list)
?>