# MDP Android App

## Commands android app send to RPi

**Moving around**
```
;{"from":"Android","com":"W"}
;{"from":"Android","com":"A"}
;{"from":"Android","com":"S"}
;{"from":"Android","com":"D"}
```

**Clear all obstacles and navigated path**
```;{"from":"Android","com":"clr"}```

**Command robot to move to coordinate and face a direction**
(last element is direction robot is facing, 0, 90, 180, 270 for top, left, bottom right)
```;{"from":"Android","com":"startingPoint","startingPoint":[1,1,0]}```

## Commands RPi send to Android
```
{"pos":[5,5,0]} // move robot to position and face direction
{"img":[5,3,3]} // mark (3,3) as obstacle with image id 5
```







