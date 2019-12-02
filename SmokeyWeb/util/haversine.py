from math import radians as toRadians
from math import cos, sin, sqrt, atan2

def haversine(lat1, lon1, lat2, lon2):
    """ Haversine distance from GPS coordinates """
    radius = 6371000.0;  # Earth radius, 6371 km

    # Convert lat/lon pairs to radians
    lat1 = toRadians(lat1);
    lon1 = toRadians(lon1);
    lat2 = toRadians(lat2);
    lon2 = toRadians(lon2);

    # Calculate 'a' param in haversine formula
    a = pow(sin((lat2 - lat1) / 2.0), 2) + cos(lat1) * cos(lat2) * pow(sin((lon2 - lon1)/2), 2);

    # Use 'a' to calculate 'c'
    c = 2 * atan2(sqrt(a), sqrt(1 - a));

    # Find distance using 'c' and the earth's radius
    dist = radius * c;

    # Return the distance
    return dist


if __name__ == "__main__":
    # print(haversine(35.210547097366, -97.442983700084, 35.210549934177, -97.442987555592))
    print(haversine(35.210549934177, -97.442987555592, 35.210391220503, -97.435035370967))
