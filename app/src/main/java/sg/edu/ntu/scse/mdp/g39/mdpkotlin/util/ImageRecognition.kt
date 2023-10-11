package sg.edu.ntu.scse.mdp.g39.mdpkotlin.util

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View

class ImageRecognition{
    companion object{
        // State variables stored here in similar format as MapDrawer.
        var obstaclesMap = HashMap<Pair<Int, Int>, Obstacle>()
        var currentIdx = 0
        var coordinatesInSequence:MutableList<Pair<Int,Int>> = mutableListOf()
        fun addObstacle(x:Int, y:Int, face:Char, imgId:Int){
            obstaclesMap.put(Pair(x,y),Obstacle(face,imgId))
        }
        fun removeObstacle(x:Int, y:Int){
            obstaclesMap.remove(Pair(x,y))
        }
        fun addObstacle(x:Int, y:Int){
            obstaclesMap.put(Pair(x,y), Obstacle(null, null))
        }
        fun updateObstacle(x:Int, y:Int, face:Char, imgId:Int){
            val key = Pair(x,y)
            if(obstaclesMap.containsKey(key)){
                obstaclesMap[key]!!.face = face
                obstaclesMap[key]!!.imgId = imgId
            }
        }
        fun handleNewImage(imgId:Int){
            if(coordinatesInSequence.size != obstaclesMap.size || currentIdx> coordinatesInSequence.size-1){
                Log.e("imageRecognition", "failed to handle new image: ${coordinatesInSequence.size} coordinates, ${obstaclesMap.size} map, currentIdx is ${currentIdx}")
            } else{
                try{
                val coor = coordinatesInSequence[currentIdx]
                val obstacle = obstaclesMap[coor]
                obstacle?.imgId = imgId
                currentIdx++
                Log.d("imageRecognition", "image updated")}
                catch(e:Exception){
                    Log.e("imageRecognition", "failed due to: ${e.toString()}")
                }
            }
        }
    }

}
class Obstacle(var face:Char?, var imgId:Int?)