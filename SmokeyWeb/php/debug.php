<?php

function arrayToString($array)
{
    foreach ($array as $key => $value)
    {
        // Add element to the array string
        echo "$key => $value";

        // If $value is an array, print it as well!
        if(is_array($value))
        { 
            printArray($value);
        }  
    }
}

?>