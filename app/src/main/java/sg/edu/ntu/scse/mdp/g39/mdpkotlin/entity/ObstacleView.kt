package sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.R
import androidx.appcompat.widget.AppCompatTextView as TextView


public class ObstacleView : TextView{

    private var imageFace:Char = 'N'
    private var image:Char? = null
    constructor(context: Context) : super(context) {
        this.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        this.gravity=Gravity.CENTER
        this.setTextColor(Color.WHITE)
        this.text=image.toString()
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
    public fun setImage(img:Char){
        image=img;
        this.text = img.toString()
        invalidate()
    }

    public fun getImageFace():Char{
        return this.imageFace
    }

}