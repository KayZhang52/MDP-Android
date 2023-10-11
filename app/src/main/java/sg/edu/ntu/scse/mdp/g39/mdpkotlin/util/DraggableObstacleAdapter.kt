package sg.edu.ntu.scse.mdp.g39.mdpkotlin.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.marginLeft
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.R
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.databinding.DraggableObstacleItemBinding
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.model.ObstacleModel

class DraggableObstacleAdapter(private val obstacleList: List<ObstacleModel>):
    RecyclerView.Adapter<DraggableObstacleAdapter.ViewHolder>() {

    inner class ViewHolder(binding: DraggableObstacleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val textView: TextView = binding.obstacleTextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DraggableObstacleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = obstacleList[position]
        holder.textView.text = model.getImgId().toString()
        holder.textView.setBackgroundResource(
            when(model.getFace()) {
                'N' -> R.drawable.obstacle_image_north
                'E' -> R.drawable.obstacle_image_east
                'S' -> R.drawable.obstacle_image_south
                'W' -> R.drawable.obstacle_image_west
                else -> R.drawable.obstacle_image_north
            }
        )

        // Set the row and column for the item
//        val layoutParams = holder.itemView.layoutParams as LinearLayout.LayoutParams
//        layoutParams.topMargin = model.getX() * MapDrawer.gridDimensions
//        layoutParams.leftMargin = model.getY() * MapDrawer.gridDimensions
//        holder.itemView.layoutParams = layoutParams

    }

    override fun getItemCount(): Int {

        return obstacleList.size
    }
}