<?php
function getActiveFireIDs()
{
    // Connect to the database
    require("db_connect.php");

    // Get list of active fires
    $query = "SELECT id FROM active_fires";

    // Attempt to get the list of fires
    try
    {
        $active_fires = $pdo->query($query);
    }
    // We couldn't get the list of fires, so give up
    catch(PDOException $e)
    {
        // Print error
        printf("%s\n", $e->getMessage());

        // Exit
        exit();
    }

    return $active_fires;
}


function getActiveFireReports($id)
{
    // Connect to the database
    require("db_connect.php");

    // Form a query to get all the fire reports
    $query = "SELECT * FROM fire_reports WHERE active_fire_id = " . $pdo->quote($id);

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
?>

<?php
    // Convex hull library
    require_once("convex_hull/convexHull.php");

    // Get all the active fires
    $fires = getActiveFireIDs();

    $fire_boundaries = [];

    // Find the convex hull for each active fire
    while($fire = $fires->fetch())
    {
        // Get the fires associated with this one
        $reports = getActiveFireReports($fire['id']);

        $report_pts = [];

        // Form these reports into a list of points
        while ($report = $reports->fetch())
        {
            $report_pts[] = [$report['latitude'], $report['longitude']];
        }

        $fire_boundaries[] = json_encode(array("id"=>$fire['id'], "points"=>convexHull($report_pts)));;
    }

    echo json_encode($fire_boundaries);
?>