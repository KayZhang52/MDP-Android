package sg.edu.ntu.scse.mdp.g39.mdpkotlin.model

data class MapModel(val obstacleList: ArrayList<ObstacleModel>){
    init {
        obstacleList.add(ObstacleModel(0,0,36,'N'))
        obstacleList.add(ObstacleModel(19,17,36,'N'))
        obstacleList.add(ObstacleModel(10,10,36,'N'))
        obstacleList.add(ObstacleModel(5,5,36,'N'))
    }
}