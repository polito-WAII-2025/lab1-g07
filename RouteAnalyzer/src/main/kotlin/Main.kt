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

fun distance(point1: Waypoint, point2: Waypoint, earthRadius : Double): Double {
    //formula di harvesine --> considero terra sferica
    //val R = 6371.0 // Raggio medio della Terra in km //PARAMETRO DA GESTIRE
    val lat1Rad = Math.toRadians(point1.latitude)
    val lon1Rad = Math.toRadians(point1.longitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val lon2Rad = Math.toRadians(point2.longitude)

    val dlat = lat2Rad - lat1Rad
    val dlon = lon2Rad - lon1Rad

    val a = sin(dlat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dlon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c    //distanza in km tra i due punti
}

@Serializable
data class MaxDistanceFromStart(val waypoint: Waypoint, val distanceKm: Double)

fun maxDistanceFromStart(points : List<Waypoint>, earthRadius: Double ) : MaxDistanceFromStart {
    var maxDistance= 0.0
    var maxDistantPoint=points[0]
    // the start point is the point with the lower timestamp
    val startPoint=points.reduce{pointA,pointB-> if(pointA.timestamp<=pointB.timestamp) pointA else pointB }
    for(point in points){
        if(distance(startPoint,point,earthRadius)>maxDistance){
            maxDistance= distance(startPoint,point,earthRadius)
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

fun mostFrequentedArea(points: List<Waypoint>, maxDistance: Double, frequentedAreaRadius:Double?, earthRadius: Double): Area{
    // calcolo raggio dell'area
    //prenderlo come parametro o calcolarlo come: maxDistance/10
    var radius: Double
    if(frequentedAreaRadius != null){
        radius=frequentedAreaRadius
    }else {
        radius = maxDistance / 10
    }
    ////////////////////////

    //supposizione: l'area più frequentata deve avere come centro uno dei punti percorsi
    var centerPointMostFrequented=points[0]
    var maxCounter=0
    var counter=0

    for (center in points){
        counter=0
        for(point in points){
            if(distance(center,point, earthRadius)<=radius){
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

fun waypointsOutsideGeofence(points: List<Waypoint>, fenceArea: Area, earthRadius: Double): WaypointsOutsideGeofence{

    val outsidePoints= points.filter{point-> distance(point,fenceArea.centralWaypoint,earthRadius)>fenceArea.areaRadiusKm }
    return WaypointsOutsideGeofence(fenceArea.centralWaypoint,fenceArea.areaRadiusKm,outsidePoints.size,outsidePoints)
}

/* ADVANCED FUNCTIONS */

/*
############################################
EVENTUALI FUNZIONI AGGIUNTIVE

- distanza totale percorsa OK
- velocita media durante il percorso OK
- ---tempo totale (gia nel frontend)---
- numero cambi di direzione bruschi (angoli > 45°)
- direzione dello spostamento (punti cardinali) ?????

#############################################
 */

@Serializable
data class outputDataAdvanced (
    val totalDistanceKm: Double,
    val averageVelocityKmH: Double,
    val movingDirection: String,
    val sharpTurns: SharpTurns
)

//fun totalDistance(points: List<Waypoint>, earthRadius: Double): Double {
//    var totalDistance = 0.0
//    for (i in 0 until points.size - 1) {
//        totalDistance += distance(points[i], points[i + 1], earthRadius)
//    }
//    return totalDistance
//}

fun totalDistanceKm(points: List<Waypoint>, earthRadius: Double): Double{

    return points.asSequence().zip(points.asSequence().drop(1)).map { (pointA,pointB) -> distance(pointA,pointB,earthRadius) }.sum()
}

//fun averageVelocity(points: List<Waypoint>, earthRadius: Double): Double{
//    return totalDistance(points, earthRadius)/(points[points.size-1].timestamp-points[0].timestamp)
//}
fun avgVelocity(points: List<Waypoint>, earthRadius: Double): Double{
    val totalDistance = totalDistanceKm(points, earthRadius)
    val totalTime = (points.last().timestamp - points.first().timestamp)/(1000*3600)
    println(points.last())
    println(points.first())
    println(points.last().timestamp - points.first().timestamp)
    println(totalTime)

    // timestamp è in millis -> converto in ore
    return totalDistance / totalTime
}

@Serializable
data class SharpTurns(val sharpTurnsCount: Int, val sharpTurnPoints: List<Waypoint>)

fun sharpTurns(points: List<Waypoint>): SharpTurns{
    var sharpTurns=0
    var sharpTurnPoints : MutableList<Waypoint> = mutableListOf()

    for (i in 0 until points.size - 2) {
        val angle = atan2(points[i+1].latitude-points[i].latitude, points[i+1].longitude-points[i].longitude) - atan2(points[i+2].latitude-points[i+1].latitude, points[i+2].longitude-points[i+1].longitude)
        if (angle > PI/4 || angle < -PI/4) {
            // add point that makes a sharp turn to a list
            sharpTurnPoints += points[i+1]
            sharpTurns++
        }
    }
    return SharpTurns(sharpTurns, sharpTurnPoints)
}

fun movingDirection(points: List<Waypoint>): String{
    val angle = atan2(points[points.size-1].latitude-points[0].latitude, points[points.size-1].longitude-points[0].longitude)
    if (angle > -PI/8 && angle < PI/8) {
        return "East"
    } else if (angle > PI/8 && angle < 3*PI/8) {
        return "North-East"
    } else if (angle > 3*PI/8 && angle < 5*PI/8) {
        return "North"
    } else if (angle > 5*PI/8 && angle < 7*PI/8) {
        return "North-West"
    } else if (angle > 7*PI/8 || angle < -7*PI/8) {
        return "West"
    } else if (angle > -7*PI/8 && angle < -5*PI/8) {
        return "South-West"
    } else if (angle > -5*PI/8 && angle < -3*PI/8) {
        return "South"
    } else {
        return "South-East"
    }
}

/* END OF ADVANCED FUNCTIONS */



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

fun main(args: Array<String>) {
    val data: MutableList<Waypoint> = mutableListOf()
    File("./waypoints.csv").forEachLine { // src/main/kotlin/waypoints.csv
        val time=it.split(";")[0].toDouble()
        val lat=it.split(";")[1].toDouble()
        val lon=it.split(";")[2].toDouble()
        data.add(Waypoint(time,lat,lon))
    }


    //val file = File("./custom-parameters.yml") // src/main/custom-parameters.yml
    val filePath = if (args.isNotEmpty()) args[0] else "custom-parameters.yml"
    val file = File(filePath)
    if (!file.exists()) {
        println("Errore: Il file di configurazione '$filePath' non esiste!")
        return
    }
    val mapper = YAMLMapper().registerKotlinModule()
    val params: CustomParameters = mapper.readValue(file)

    println("Earth Radius: ${params.earthRadiusKm} km")
    println("Geofence Center: (${params.geofenceCenterLatitude}, ${params.geofenceCenterLongitude})")
    println("Geofence Radius: ${params.geofenceRadiusKm} km")
    println("Most Frequented Area Radius: ${params.mostFrequentedAreaRadiusKm ?: "Not defined"} km")


    //println(data)

    val maxDistanceFromStart = maxDistanceFromStart(data, params.earthRadiusKm)
    println("maxDistance: ")
    println(maxDistanceFromStart)

    /*val (maxDistance, maxDistantPoint) = maxDistanceFromStart(data)
    println("Max distance from start: $maxDistance km, at point: $maxDistantPoint")*/

    val mostFrequentedArea = mostFrequentedArea(data, maxDistanceFromStart.distanceKm,params.mostFrequentedAreaRadiusKm, params.earthRadiusKm)
    println("Most frequented area: center = ${mostFrequentedArea.centralWaypoint}, radius = ${mostFrequentedArea.areaRadiusKm}, entries = ${mostFrequentedArea.entriesCount}")
    /*println(mostFrequentedArea)*/

    val waypointsOutsideGeofence = waypointsOutsideGeofence(data, Area(Waypoint(0.0,params.geofenceCenterLatitude,params.geofenceCenterLongitude),params.geofenceRadiusKm),params.earthRadiusKm)
    /*println("Waypoints outside geofence: ${outsidePoints.size}")
    outsidePoints.forEach { println(it) }*/
    println("Waypoints outside geofence: GFcenter= ${waypointsOutsideGeofence.centralWaypoint}, GFradius= ${waypointsOutsideGeofence.areaRadiusKm}, count= ${waypointsOutsideGeofence.count}")

    //val waypointsOutsideGeofence = WaypointsOutsideGeofence(mostFrequentedArea.centralWaypoint, mostFrequentedArea.areaRadiusKm, outsidePoints.size, outsidePoints)

    val output = OutputData(maxDistanceFromStart, mostFrequentedArea, waypointsOutsideGeofence)

    val jsonOutput = Json.encodeToString(OutputData.serializer(), output)
    println("Output:")
    println(jsonOutput)

    File("./output/output.json").writeText(jsonOutput)

    /* ADVANCED FUNCTIONS */

    val totalDistanceKm = totalDistanceKm(data, params.earthRadiusKm)
    println("Total distance: $totalDistanceKm km")

    val averageVelocity = avgVelocity(data, params.earthRadiusKm)
    println("Average velocity: $averageVelocity km/h")

    val sharpTurns = sharpTurns(data)
    println("Sharp turns: ${sharpTurns.sharpTurnsCount}")
    println("Sharp turn points: ${sharpTurns.sharpTurnPoints}")

    val movingDirection = movingDirection(data)
    println("Moving direction: $movingDirection")


    val outputAdvanced = outputDataAdvanced(totalDistanceKm, averageVelocity, movingDirection, sharpTurns)

    val jsonOutputAdvanced = Json.encodeToString(outputDataAdvanced.serializer(), outputAdvanced)
    println("Output Advanced:")
    println(jsonOutputAdvanced)

    File("./output/output_advanced.json").writeText(jsonOutputAdvanced)

}