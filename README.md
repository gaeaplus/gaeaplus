gaeaplus
========

Gaea+ is an open source Virtual Globe based on NASA World Wind. 

The project extends the Worldwind Java core with features crucial for advanced real-time rendering of geospatial information, including optimized Render-to-Texture capabilities enabling visualization of large vector datasets, GLSL Shader support built directly into the Worldwind render loop making custom visual effects a breeze, deferred rendering, and a flexible WFS & GML support.

This project is part of the [NASA Worldwind Challenge](http://eurochallenge.como.polimi.it/). The source code will become available on June 21, 2013!

REALTIME VECTOR RENDERING ENGINE
--------------------------------
Visualise massive vector data sets at arbitrary level of detail. Gaea+ Render2Texture technology transforms all your lines and polygons into a single memory-friendly tile pyramid on the fly. You will never notice that lines and polygons are rendered as textures. The only thing you will notice is the speed and responsiveness!

CUSTOM SHADER SUPPORT
---------------------
Release the power of graphics processors. Make beautiful atmospheric effects, cast photo-realistic shadows, render water surface with supplied shaders or write your own GLSL or Cg shaders and watch the globe transform right in front of you.

SEAMLESS OGC WFS
----------------
Access your data as they appear in your data store. Web Feature Service layer provides access to any OGC WFS-compliant vector data set. The layer supports points, lines and polygons, all with custom symbology. Gaea+Render2Texture technology is used for efficient visualisation of lines and polygons.

MORE INFORMATION
----------------
Furhter information can be found at the [project's home page](http://gaeaplus.eu) or at info@gaeaplus.eu.
