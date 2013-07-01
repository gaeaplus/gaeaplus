/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.avlist;

import gov.nasa.worldwind.avlist.AVKey;

/**
 *
 * @author marjan
 */
public interface AvKeyExt extends AVKey
{
    final String WFS_LAYER_CACHE_SIZE = "si.xlab.gaea.avkeyext.WFSLayerCacheSize";
    final String HTTP_USER_AGENT = "si.xlab.gaea.avkeyext.UserAgent";
    
	final String ENABLE_SUNLIGHT = "si.xlab.gaea.core.GaeaSceneController.enableSunlight";
	final String ENABLE_SHADOWS = "si.xlab.gaea.core.GaeaSceneController.enableShadows";
	final String ENABLE_SHADOWS_ON_CAMERA_STOP = "si.xlab.gaea.core.GaeaSceneController.enableShadowsOnCameraStop";
	final String ENABLE_ATMOSPHERE = "si.xlab.gaea.core.GaeaSceneController.enableAtmosphere";
	final String ENABLE_ATMOSPHERE_WITH_AERIAL_PERSPECTIVE = "si.xlab.gaea.core.GaeaSceneController.enableAerialPerspective";
	final String ENABLE_RECORDING_MODE = "si.xlab.gaea.core.GaeaSceneController.enableRecordingMode";
}
