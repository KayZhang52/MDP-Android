package sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity

import android.content.Context
import android.view.Gravity
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.R
import androidx.appcompat.widget.AppCompatTextView as TextView


public class ObstacleView : TextView{

    private var imageFace:Char = 'N'
    private var image:Char? = null
    constructor(context: Context) : super(context) {
        this.setBackgroundColor(R.color.colorPrimary)
        this.gravity=Gravity.CENTER
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
        when(imageFace){
            '1'->{
                this.text = "1"
            }
            '2'->{
                this.text = "2"
            }
            '3'->{
                this.text = "3"
            }
            '4'->{
                this.text = "4"
            }
            '5'->{
                this.text = "5"
            }
            '6'->{
                this.text = "6"
            }
            '7'->{
                this.text = "7"
            }
            '8'->{
                this.text = "8"
            }
            '9'->{
                this.text = "9"
            }


        }
        invalidate()
    }

}