Gaea+
========

Gaea+ is an open source Virtual Globe based on the [NASA World Wind](http://goworldwind.org). It is provided by Gaea+ team from [XLAB](http://xlab.si), one of the fastest growing ICT companies in its region, with long and extensive experience leading and executing software projects.

The project extends the World Wind Java core with features crucial for advanced real-time rendering of geospatial information, including optimized Render-to-Texture capabilities enabling visualization of large vector datasets, GLSL Shader support built directly into the Worldwind render loop making custom visual effects a breeze, deferred rendering, and a flexible WFS & GML support.

This project has been awarded first place at the [NASA World Wind Challenge](http://eurochallenge.como.polimi.it/).

Realtime vector rendering engine
--------------------------------
Visualise massive vector data sets at arbitrary level of detail. Gaea+ Render2Texture technology transforms all your lines and polygons into a single memory-friendly tile pyramid on the fly. You will never notice that lines and polygons are rendered as textures. The only thing you will notice is the speed and responsiveness!

Custom shader support
---------------------
Unleash the power of graphics processors. Make beautiful atmospheric effects, cast photo-realistic shadows, render water surface with supplied shaders or write your own GLSL or Cg shaders and watch the globe transform right in front of you.

Seamless OGC WFS
----------------
Access your data as they appear in your data store. Web Feature Service layer provides access to any OGC WFS-compliant vector data set. The layer supports points, lines and polygons, all with custom symbology. Gaea+Render2Texture technology is used for efficient visualisation of lines and polygons.

References
----------
Gaea+ Open Source core is used in mission-critical emergency response systems with massive datasets deployed in several regional emergency call centers. It is also used by spatial planners for data visualization, manipulation and analysis, as well as for recordng stunning photo-realistic presentations of our World.

Getting started
---------------
Just type "ant run" and play with the example application! Note that this is just a means to demonstrate the rendering capabilities with a very basic user interface.

Known bugs and other info
---------------
1. Height map files in cache sometimes get corrupted and thus automatically deleted.
2. WFS polylines are transparent and/or black on some GPU/driver combinations, e.g. Intel GMA 4500MHD.
3. Shader compilation errors on some GPU/driver combinations. 

Gaea+ Open Source is currently based on World Wind version 2.0-daily-685.1405.

Reaching us
-----------
Furhter information can be found at the [project's home page](http://gaeaplus.eu) or at info@gaeaplus.eu.
