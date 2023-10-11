package sg.edu.ntu.scse.mdp.g39.mdpkotlin

import android.view.View

fun disableElement(view: View?) {
    view?.isEnabled = false
    view?.alpha = 0.7f
}

fun enableElement(view: View?) {
    view?.isEnabled = true
    view?.alpha = 1f
}