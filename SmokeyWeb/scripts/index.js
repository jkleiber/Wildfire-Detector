// Globals
var map = L.map('map').setView([35, -94.3], 5);

var fireIcon = L.icon({iconUrl: "/img/fire.png", iconSize: [48, 48], iconAnchor: [24, 48], popupAnchor: [0, -48], className: "fireMarker"});

function startMap()
{
    // Add tile engine
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);
}

function getFireReports()
{
    $.ajax({
        type: "POST",
        url: '/php/get_all_reports.php',
        success: function(data)
        {
            populateMap(data)
        },
        error: function()
        {
            console.log('AJAX fail')
        }
    });
}

function populateMap(fires)
{
    // Parse the JSON
    fireList = JSON.parse(fires)

    // Go through each fire
    fireList.forEach(function(fire)
    {
        // Convert the JSON string into an object
        fire_json = JSON.parse(fire)

        // Find the fire location from the JSON response
        var fireLocation = L.latLng(fire_json.lat, fire_json.lon)

        // TODO: do we want this behavior?
        // Center onto the most recent fire report
        map.setView(fireLocation, 15.0)

        // Create a marker object and add the image
        var fireMarker = L.marker(fireLocation, {icon: fireIcon});

        // Add a popup to the marker
        fireMarker.bindPopup("<b>Reported at: </b>" + fire_json.timestamp)

        // Add the marker to the map
        fireMarker.addTo(map)
    });
}

map.on('zoomend', function(){
    var newZoom = '' + (3*(map.getZoom())) +'px';
    $('#map .fireMarker').css({'width':newZoom,'height':newZoom});
})