package sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.MainActivity
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.R
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.util.MapDrawer
import androidx.appcompat.widget.AppCompatTextView as TextView


public class ObstacleView: TextView{

    private var imageFace:Char = 'N'
    private var image:Int = 0
    constructor(context: Context) : super(context) {
        this.setBackgroundColor(resources.getColor(android.R.color.black))
        this.gravity=Gravity.CENTER
        this.setTextColor(Color.WHITE)
        this.textSize = 8.0F

        this.text=image.toString()
        this.tag = "obstacle_tile_overlay"

        this.width = MapDrawer.gridDimensions
        this.height = MapDrawer.gridDimensions

        this.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val data = ClipData.newPlainText("", "")
                val shadowBuilder = DragShadowBuilder(
                    view
                )
                view.startDrag(data, shadowBuilder, view, 0)
                view.visibility = View.INVISIBLE
                true
            } else {
                false
            }
        }
    }
    public fun setImageFace(face:Char){
        imageFace=face;
        when(imageFace){
            'N'->{
                this.setBackgroundResource(R.drawable.obstacle_image_north);
            }
            'E'->{
                this.setBackgroundResource(R.drawable.obstacle_image_east);
            }
            'S'->{
                this.setBackgroundResource(R.drawable.obstacle_image_south);
            }
            'W'->{
                this.setBackgroundResource(R.drawable.obstacle_image_west);
            }
        }
        invalidate()
    }
    public fun setImage(img:Int){
        image=img;
        this.text = img.toString()
        invalidate()
    }

    public fun getImageFace():Char{
        return this.imageFace
    }

}