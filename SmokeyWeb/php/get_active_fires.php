<?php
function getAllFires()
{
    // Connect to the database
    require_once("db_connect.php");

    // Get list of active fires
    $query = "SELECT * FROM active_fires";

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
?>

<?php
    // Get all the active fires
    $fires = getAllFires();

    // TODO: parse to JSON
?>