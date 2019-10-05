<?php

function arrayToString($array, $arr_str)
{
    foreach ($array as $key => $value)
    {
        // Add element to the array string
        $arr_str .= "$key => $value \n";

        // If $value is an array, print it as well!
        if(is_array($value))
        { 
            $arr_str = printArray($value, $arr_str);
        }  
    }

    return $arr_str;
}

?>