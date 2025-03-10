package org.example

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import kotlin.math.*

data class Point(val time: Double, val lat: Double, val lon: Double)

fun distance(point1: Point, point2: Point): Double {
    //formula di harvesine --> considero terra sferica
    val R = 6371.0 // Raggio medio della Terra in km //PARAMETRO DA GESTIRE
    val lat1Rad = Math.toRadians(point1.lat)
    val lon1Rad = Math.toRadians(point1.lon)
    val lat2Rad = Math.toRadians(point2.lat)
    val lon2Rad = Math.toRadians(point2.lon)

    val dlat = lat2Rad - lat1Rad
    val dlon = lon2Rad - lon1Rad

    val a = sin(dlat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dlon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return R * c    //distanza in km tra i due punti
}

fun maxDistanceFromStart(points : List<Point>) : Double {
    var maxDistance= 0.0
    var maxDistantPoint=points[0]
    // the start point is the point with the lower timestamp
    val startPoint=points.reduce{pointA,pointB-> if(pointA.time<=pointB.time) pointA else pointB }
    for(point in points){
        if(distance(startPoint,point)>maxDistance){
            maxDistance= distance(startPoint,point)
            maxDistantPoint=point
        }
    }
    ///////////////////////////
    //bisogna tornare anche il punto a cui si riferisce la distanza
    ////////////////////////////
    return maxDistance
}

class Area(val centralPoint: Point, val radius: Double, var entriesCount: Int=0)

fun mostFrequentedArea(points: List<Point>, maxDistance: Double): Area{
    // calcolo raggio dell'area
    //prenderlo come parametro o calcolarlo come: maxDistance/10
    val radius=maxDistance/10;
    ////////////////////////

    //supposizione l'aria più frequentata deve avere come centro uno dei punti percorsi
    var centerPointMostFrequented=points[0]
    var maxCounter=0
    var counter=0

    for (center in points){
        counter=0
        for(point in points){
            if(distance(center,point)<=radius){
                counter++
            }
        }
        if(counter>maxCounter){
            centerPointMostFrequented=center
            maxCounter=counter
        }
    }

    return Area(centerPointMostFrequented,radius,counter)
}

fun waypointsOutsideGeofence(points: List<Point>, fenceArea: Area): List<Point>{
    return points.filter{point-> distance(point,fenceArea.centralPoint)>fenceArea.radius }
}

/*
############################################
EVENTUALI FUNZIONI AGGIUNTIVE

- distanza totale percorsa
- velocita media durante il percoso
- tempo totale (gia nel frontend)
- numero cambi di direzione bruschi
- direzione dello spostamento (punti cardinali) ?????

#############################################
 */
data class CustomParameters(
    val earthRadiusKm: Double,
    val geofenceCenterLatitude: Double,
    val geofenceCenterLongitude: Double,
    val geofenceRadiusKm: Double,
    val mostFrequentedAreaRadiusKm: Double? = null  // Optional
)

fun main() {
    val data: MutableList<Point> = mutableListOf()
    File("RouteAnalyzer/src/main/kotlin/waypoints.csv").forEachLine {
        val time=it.split(";")[0].toDouble()
        val lat=it.split(";")[1].toDouble()
        val lon=it.split(";")[2].toDouble()
        data.add(Point(time,lat,lon))
    }


    val file = File("RouteAnalyzer/src/main/custom-parameters.yml")
    val mapper = YAMLMapper().registerKotlinModule()
    val params: CustomParameters = mapper.readValue(file)

    println("Earth Radius: ${params.earthRadiusKm} km")
    println("Geofence Center: (${params.geofenceCenterLatitude}, ${params.geofenceCenterLongitude})")
    println("Geofence Radius: ${params.geofenceRadiusKm} km")
    println("Most Frequented Area Radius: ${params.mostFrequentedAreaRadiusKm ?: "Not defined"} km")


//println(data)
    val maxDistance=maxDistanceFromStart(data)
    println(maxDistance)

    /*val (maxDistance, maxDistantPoint) = maxDistanceFromStart(data)
    println("Max distance from start: $maxDistance km, at point: $maxDistantPoint")*/

    val area = mostFrequentedArea(data, maxDistance)
    println("Most frequented area: center=${area.centralPoint}, radius=${area.radius}, entries=${area.entriesCount}")

    val outsidePoints = waypointsOutsideGeofence(data, area)
    println("Waypoints outside geofence: ${outsidePoints.size}")
    //outsidePoints.forEach { println(it) }

}