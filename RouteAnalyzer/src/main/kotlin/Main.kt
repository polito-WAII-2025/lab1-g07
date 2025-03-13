package org.example

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.*

@Serializable
data class Waypoint(val timestamp: Double, val latitude: Double, val longitude: Double)

fun distance(point1: Waypoint, point2: Waypoint): Double {
    //formula di harvesine --> considero terra sferica
    val R = 6371.0 // Raggio medio della Terra in km //PARAMETRO DA GESTIRE
    val lat1Rad = Math.toRadians(point1.latitude)
    val lon1Rad = Math.toRadians(point1.longitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val lon2Rad = Math.toRadians(point2.longitude)

    val dlat = lat2Rad - lat1Rad
    val dlon = lon2Rad - lon1Rad

    val a = sin(dlat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dlon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return R * c    //distanza in km tra i due punti
}

@Serializable
data class MaxDistanceFromStart(val waypoint: Waypoint, val distanceKm: Double)

fun maxDistanceFromStart(points : List<Waypoint>) : MaxDistanceFromStart {
    var maxDistance= 0.0
    var maxDistantPoint=points[0]
    // the start point is the point with the lower timestamp
    val startPoint=points.reduce{pointA,pointB-> if(pointA.timestamp<=pointB.timestamp) pointA else pointB }
    for(point in points){
        if(distance(startPoint,point)>maxDistance){
            maxDistance= distance(startPoint,point)
            maxDistantPoint=point
        }
    }
    ///////////////////////////
    //bisogna tornare anche il punto a cui si riferisce la distanza
    ////////////////////////////
    val maxDistanceFromStart = MaxDistanceFromStart(maxDistantPoint, maxDistance)

    return maxDistanceFromStart
}

@Serializable
data class Area(val centralWaypoint: Waypoint, val areaRadiusKm: Double, var entriesCount: Int=0)

fun mostFrequentedArea(points: List<Waypoint>, maxDistance: Double): Area{
    // calcolo raggio dell'area
    //prenderlo come parametro o calcolarlo come: maxDistance/10
    val radius=maxDistance/10
    ////////////////////////

    //supposizione: l'area più frequentata deve avere come centro uno dei punti percorsi
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
@Serializable
data class WaypointsOutsideGeofence(val centralWaypoint: Waypoint, val areaRadiusKm: Double, val count: Int, val waypoints: List<Waypoint>)

fun waypointsOutsideGeofence(points: List<Waypoint>, fenceArea: Area): List<Waypoint>{
    return points.filter{point-> distance(point,fenceArea.centralWaypoint)>fenceArea.areaRadiusKm }
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

@Serializable
data class OutputData(
    val maxDistanceFromStart: MaxDistanceFromStart,
    val mostFrequentedArea: Area,
    val waypointsOutsideGeofence: WaypointsOutsideGeofence
)

fun main() {
    val data: MutableList<Waypoint> = mutableListOf()
    File("src/main/kotlin/waypoints.csv").forEachLine {
        val time=it.split(";")[0].toDouble()
        val lat=it.split(";")[1].toDouble()
        val lon=it.split(";")[2].toDouble()
        data.add(Waypoint(time,lat,lon))
    }


    val file = File("src/main/custom-parameters.yml")
    val mapper = YAMLMapper().registerKotlinModule()
    val params: CustomParameters = mapper.readValue(file)

    println("Earth Radius: ${params.earthRadiusKm} km")
    println("Geofence Center: (${params.geofenceCenterLatitude}, ${params.geofenceCenterLongitude})")
    println("Geofence Radius: ${params.geofenceRadiusKm} km")
    println("Most Frequented Area Radius: ${params.mostFrequentedAreaRadiusKm ?: "Not defined"} km")


    //println(data)

    val maxDistanceFromStart = maxDistanceFromStart(data)
    println("maxDistance: ")
    println(maxDistanceFromStart)

    /*val (maxDistance, maxDistantPoint) = maxDistanceFromStart(data)
    println("Max distance from start: $maxDistance km, at point: $maxDistantPoint")*/

    val mostFrequentedArea = mostFrequentedArea(data, maxDistanceFromStart.distanceKm)
    println("Most frequented area: center = ${mostFrequentedArea.centralWaypoint}, radius = ${mostFrequentedArea.areaRadiusKm}, entries = ${mostFrequentedArea.entriesCount}")
    println(mostFrequentedArea)

    // DA RIVEDERE BENE
    val outsidePoints = waypointsOutsideGeofence(data, mostFrequentedArea)
    println("Waypoints outside geofence: ${outsidePoints.size}")
    outsidePoints.forEach { println(it) }

    val waypointsOutsideGeofence = WaypointsOutsideGeofence(mostFrequentedArea.centralWaypoint, mostFrequentedArea.areaRadiusKm, outsidePoints.size, outsidePoints)

    val output = OutputData(maxDistanceFromStart, mostFrequentedArea, waypointsOutsideGeofence)

    val jsonOutput = Json.encodeToString(OutputData.serializer(), output)

    File("src/main/kotlin/output.json").writeText(jsonOutput)


}