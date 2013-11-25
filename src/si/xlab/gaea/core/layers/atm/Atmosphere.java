package si.xlab.gaea.core.layers.atm;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.BasicShaderFactory;
import si.xlab.gaea.core.shaders.Shader;
import si.xlab.gaea.core.shaders.ShaderContext;
import si.xlab.gaea.core.shaders.ShaderFactory;
import gov.nasa.worldwind.util.WWIO;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import si.xlab.gaea.avlist.AvKeyExt;

/**
 *
 * @author vito
 */
public class Atmosphere extends BasicShaderFactory
{
    private final Object fileLock = new Object();
    private boolean texturesDone = false;
    
	private final float Rg = 6360.0f;
    private final float Rt = 6420.0f;
    private final float RL = 6421.0f;
    private final int TRANSMITTANCE_W = 256;
    private final int TRANSMITTANCE_H = 64;
    private final int SKY_W = 64;
    private final int SKY_H = 16;
    private final int RES_R = 32;
    private final int RES_MU = 128;
    private final int RES_MU_S = 32;
    private final int RES_NU = 8;

    private static Atmosphere Instance = null;

    public static Atmosphere getInstance()
    {
        if (Instance == null)
        {
            Instance = new Atmosphere();
            return Instance;
        } else
        {
            return Instance;
        }
    }

    public boolean isTexturesDone()
    {
        return texturesDone;
    }

    public float getEarthRadi()
    {
        return Rg;
    }

    public int getInscatterTexture()
    {
        return inscatterTexture[0];
    }

    public int getIrradianceTexture()
    {
        return irradianceTexture[0];
    }

    public int getTransmittanceTexture()
    {
        return transmittanceTexture[0];
    }

    public String getParamCode()
    {
        String sCode = "";
        String prefix = "const ";

        sCode += prefix + "float Rg = " + Rg + ";\n";
        sCode += prefix + "float Rt = " + Rt + ";\n";
        sCode += prefix + "float RL = " + RL + ";\n";

        sCode += prefix + "int TRANSMITTANCE_W = " + TRANSMITTANCE_W + ";\n";
        sCode += prefix + "int TRANSMITTANCE_H = " + TRANSMITTANCE_H + ";\n";

        sCode += prefix + "int SKY_W = " + SKY_W + ";\n";
        sCode += prefix + "int SKY_H = " + SKY_H + ";\n";

        sCode += prefix + "int RES_R = " + RES_R + ";\n";
        sCode += prefix + "int RES_MU = " + RES_MU + ";\n";
        sCode += prefix + "int RES_MU_S = " + RES_MU_S + ";\n";
        sCode += prefix + "int RES_NU = " + RES_NU + ";\n";

        return sCode;
    }

    void drawQuad(GL2 gl)
    {
        gl.glBegin(GL.GL_TRIANGLE_STRIP);
        gl.glVertex2f(-1.0f, -1.0f);
        gl.glVertex2f(+1.0f, -1.0f);
        gl.glVertex2f(-1.0f, +1.0f);
        gl.glVertex2f(+1.0f, +1.0f);
        gl.glEnd();
    }

    void drawQuadTex(GL2 gl)
    {
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glBegin(GL.GL_TRIANGLE_STRIP);
        gl.glTexCoord2f(+0.0f, +0.0f);
        gl.glVertex2f(-1.0f, -1.0f);
        gl.glTexCoord2f(+1.0f, +0.0f);
        gl.glVertex2f(+1.0f, -1.0f);
        gl.glTexCoord2f(+0.0f, +1.0f);
        gl.glVertex2f(-1.0f, +1.0f);
        gl.glTexCoord2f(+1.0f, +1.0f);
        gl.glVertex2f(+1.0f, +1.0f);
        gl.glEnd();
        gl.glDisable(GL.GL_TEXTURE_2D);
    }

    void drawQuadTex3D(GL2 gl)
    {
        gl.glEnable(GL2.GL_TEXTURE_3D);
        gl.glBegin(GL.GL_TRIANGLE_STRIP);

        gl.glTexCoord3f(+0.0f, +0.0f, 1.0f);
        gl.glVertex2f(-1.0f, -1.0f);

        gl.glTexCoord3f(+1.0f, +0.0f, 1.0f);
        gl.glVertex2f(+1.0f, -1.0f);

        gl.glTexCoord3f(+0.0f, +1.0f, 1.0f);
        gl.glVertex2f(-1.0f, +1.0f);

        gl.glTexCoord3f(+1.0f, +1.0f, 1.0f);
        gl.glVertex2f(+1.0f, +1.0f);

        gl.glEnd();
        gl.glDisable(GL2.GL_TEXTURE_3D);
    }
    // ----------------------------------------------------------------------------
    // PRECOMPUTATIONS
    // ----------------------------------------------------------------------------
    private final int reflectanceUnit = 0;
    private final int transmittanceUnit = 1;
    private final int irradianceUnit = 2;
    private final int inscatterUnit = 3;
    private final int deltaEUnit = 4;
    private final int deltaSRUnit = 5;
    private final int deltaSMUnit = 6;
    private final int deltaJUnit = 7;
    private int[] transmittanceTexture = new int[1];//unit 1, T table
    private int[] irradianceTexture = new int[1];//unit 2, E table
    private int[] inscatterTexture = new int[1];//unit 3, S table
    private int[] deltaETexture = new int[1];//unit 4, deltaE table
    private int[] deltaSRTexture = new int[1];//unit 5, deltaS table (Rayleigh part)
    private int[] deltaSMTexture = new int[1];//unit 6, deltaS table (Mie part)
    private int[] deltaJTexture = new int[1];//unit 7, deltaJ table
	
    Shader transmittanceProg;
    Shader irradiance1Prog;
    Shader inscatter1Prog;
    Shader copyIrradianceProg;
    Shader copyInscatter1Prog;
    Shader jProg;
    Shader irradianceNProg;
    Shader inscatterNProg;
    Shader copyInscatterNProg;
	
    private int[] fbo = new int[1];

    private void setLayer(Shader shader, int layer)
    {
        double r = layer / (RES_R - 1.0);
        r = r * r;
        r = Math.sqrt(Rg * Rg + r * (Rt * Rt - Rg * Rg)) + (layer == 0 ? 0.01 : (layer == RES_R - 1 ? -0.001 : 0.0));
        double dmin = Rt - r;
        double dmax = Math.sqrt(r * r - Rg * Rg) + Math.sqrt(Rt * Rt - Rg * Rg);
        double dminp = r - Rg;
        double dmaxp = Math.sqrt(r * r - Rg * Rg);

		shader.setParam("r", new float[]{(float)r});
		shader.setParam("dhdH", new float[]{(float) dmin, (float) dmax, (float) dminp, (float) dmaxp});
		shader.setParam("layer", new int[]{layer});
    }

    private boolean doPrecompute(DrawContext dc)
    {
		GL2 gl = dc.getGL().getGL2();
		
        gl.glActiveTexture(GL.GL_TEXTURE0 + transmittanceUnit);
        gl.glGenTextures(1, transmittanceTexture, 0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, transmittanceTexture[0]);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB32F, TRANSMITTANCE_W, TRANSMITTANCE_H, 0, GL.GL_RGB, GL.GL_FLOAT, null);

        gl.glActiveTexture(GL.GL_TEXTURE0 + irradianceUnit);
        gl.glGenTextures(1, irradianceTexture, 0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, irradianceTexture[0]);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB32F, SKY_W, SKY_H, 0, GL.GL_RGB, GL.GL_FLOAT, null);

        gl.glActiveTexture(GL.GL_TEXTURE0 + inscatterUnit);
        gl.glGenTextures(1, inscatterTexture, 0);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, inscatterTexture[0]);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_EDGE);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL.GL_RGBA32F, RES_MU_S * RES_NU, RES_MU, RES_R, 0, GL.GL_RGBA, GL.GL_FLOAT, null);

        gl.glActiveTexture(GL.GL_TEXTURE0 + deltaEUnit);
        gl.glGenTextures(1, deltaETexture, 0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, deltaETexture[0]);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB32F, SKY_W, SKY_H, 0, GL.GL_RGB, GL.GL_FLOAT, null);

        gl.glActiveTexture(GL.GL_TEXTURE0 + deltaSRUnit);
        gl.glGenTextures(1, deltaSRTexture, 0);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, deltaSRTexture[0]);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_EDGE);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL.GL_RGB32F, RES_MU_S * RES_NU, RES_MU, RES_R, 0, GL.GL_RGB, GL.GL_FLOAT, null);

        gl.glActiveTexture(GL.GL_TEXTURE0 + deltaSMUnit);
        gl.glGenTextures(1, deltaSMTexture, 0);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, deltaSMTexture[0]);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_EDGE);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL.GL_RGB32F, RES_MU_S * RES_NU, RES_MU, RES_R, 0, GL.GL_RGB, GL.GL_FLOAT, null);

        gl.glActiveTexture(GL.GL_TEXTURE0 + deltaJUnit);
        gl.glGenTextures(1, deltaJTexture, 0);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, deltaJTexture[0]);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_EDGE);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL.GL_RGB32F, RES_MU_S * RES_NU, RES_MU, RES_R, 0, GL.GL_RGB, GL.GL_FLOAT, null);

		ShaderContext sc = dc.getShaderContext();

		ShaderFactory lastShaderFactory = sc.getShaderFactory();
		sc.setShaderFactory(this);

		Shader[] shaders = new Shader[9];
		transmittanceProg  = shaders[0] = sc.getShader("transmittance.glsl", "#version 120\n"+getParamCode());
		irradiance1Prog    = shaders[1] = sc.getShader("irradiance1.glsl", "#version 120\n"+getParamCode());
		inscatter1Prog     = shaders[2] = sc.getShader("inscatter1.glsl", "#version 120\n"+getParamCode());
		copyIrradianceProg = shaders[3] = sc.getShader("copyIrradiance.glsl", "#version 120\n"+getParamCode());
		copyInscatter1Prog = shaders[4] = sc.getShader("copyInscatter1.glsl", "#version 120\n"+getParamCode());
		jProg 			   = shaders[5] = sc.getShader("inscatterS.glsl", "#version 120\n"+getParamCode());
		irradianceNProg    = shaders[6] = sc.getShader("irradianceN.glsl", "#version 120\n"+getParamCode());
		inscatterNProg     = shaders[7] = sc.getShader("inscatterN.glsl", "#version 120\n"+getParamCode());
		copyInscatterNProg = shaders[8] = sc.getShader("copyInscatterN.glsl", "#version 120\n"+getParamCode());

		for(Shader s : shaders){
			if(!s.isValid()){
				return false;
			}
			if(s.getGS() != 0){
				gl.glProgramParameteri(s.getProgram(), GL2.GL_GEOMETRY_INPUT_TYPE_EXT, GL.GL_TRIANGLES);
            	gl.glProgramParameteri(s.getProgram(), GL2.GL_GEOMETRY_OUTPUT_TYPE_EXT, GL.GL_TRIANGLE_STRIP);
            	gl.glProgramParameteri(s.getProgram(), GL2.GL_GEOMETRY_VERTICES_OUT_EXT, 3);	
			}
		}

		sc.setShaderFactory(lastShaderFactory);
		
        gl.glGenFramebuffers(1, fbo, 0);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo[0]);
        gl.glReadBuffer(GL.GL_COLOR_ATTACHMENT0);
        gl.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);

        // computes transmittance texture T (line 1 in algorithm 4.1)
        gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, transmittanceTexture[0], 0);
        gl.glViewport(0, 0, TRANSMITTANCE_W, TRANSMITTANCE_H);
		transmittanceProg.enable(sc);
        drawQuad(gl);
		transmittanceProg.disable(sc);

        // computes irradiance texture deltaE (line 2 in algorithm 4.1)
        gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, deltaETexture[0], 0);
        gl.glViewport(0, 0, SKY_W, SKY_H);
		irradiance1Prog.enable(sc);
		irradiance1Prog.setParam("transmittanceSampler", transmittanceUnit);
        drawQuad(gl);
		irradiance1Prog.disable(sc);

        // computes single scattering texture deltaS (line 3 in algorithm 4.1)
        // Rayleigh and Mie separated in deltaSR + deltaSM
        gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, deltaSRTexture[0], 0);
        gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT1, deltaSMTexture[0], 0);
        int[] bufs =
        {
            GL2.GL_COLOR_ATTACHMENT0, GL2.GL_COLOR_ATTACHMENT1
        };
        gl.glDrawBuffers(2, bufs, 0);
        gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
		
		inscatter1Prog.enable(sc);
		inscatter1Prog.setParam("transmittanceSampler", transmittanceUnit);
        for (int layer = 0; layer < RES_R; ++layer)
        {
            setLayer(inscatter1Prog, layer);
            drawQuad(gl);
        }
		inscatter1Prog.disable(sc);

        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT1, GL.GL_TEXTURE_2D, 0, 0);
        gl.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);

        // copies deltaE into irradiance texture E (line 4 in algorithm 4.1)
        gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, irradianceTexture[0], 0);
        gl.glViewport(0, 0, SKY_W, SKY_H);
		copyIrradianceProg.enable(sc);
		copyIrradianceProg.setParam("k", new float[]{0.0f});
		copyIrradianceProg.setParam("deltaESampler", deltaEUnit);
        drawQuad(gl);
		copyIrradianceProg.disable(sc);

        // copies deltaS into inscatter texture S (line 5 in algorithm 4.1)
        gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, inscatterTexture[0], 0);
        gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
		copyInscatter1Prog.enable(sc);
		copyInscatter1Prog.setParam("deltaSRSampler", deltaSRUnit);
		copyInscatter1Prog.setParam("deltaSMSampler", deltaSMUnit);
        for (int layer = 0; layer < RES_R; ++layer)
        {
            setLayer(copyInscatter1Prog, layer);
            drawQuad(gl);
        }
		copyInscatter1Prog.disable(sc);

        // loop for each scattering order (line 6 in algorithm 4.1)
        for (int order = 2; order <= 4; ++order)
        {
            // computes deltaJ (line 7 in algorithm 4.1)
            gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, deltaJTexture[0], 0);
            gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
			jProg.enable(sc);
			jProg.setParam("first", new float[]{order == 2 ? 1.0f : 0.0f});
			jProg.setParam("transmittanceSampler", transmittanceUnit);
			jProg.setParam("deltaESampler", deltaEUnit);
			jProg.setParam("deltaSRSampler", deltaSRUnit);
			jProg.setParam("deltaSMSampler", deltaSMUnit);
            for (int layer = 0; layer < RES_R; ++layer)
            {
                setLayer(jProg, layer);
                drawQuad(gl);
            }
			jProg.disable(sc);

            // computes deltaE (line 8 in algorithm 4.1)
            gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, deltaETexture[0], 0);
            gl.glViewport(0, 0, SKY_W, SKY_H);
			irradianceNProg.enable(sc);
			irradianceNProg.setParam("first", new float[]{order == 2 ? 1.0f : 0.0f});
			irradianceNProg.setParam("transmittanceSampler", transmittanceUnit);
			irradianceNProg.setParam("deltaSRSampler", deltaSRUnit);
			irradianceNProg.setParam("deltaSMSampler", deltaSMUnit);
            drawQuad(gl);
			irradianceNProg.disable(sc);

            // computes deltaS (line 9 in algorithm 4.1)
            gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, deltaSRTexture[0], 0);
            gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
			inscatterNProg.enable(sc);
			inscatterNProg.setParam("first", new float[]{order == 2 ? 1.0f : 0.0f});
			inscatterNProg.setParam("transmittanceSampler", transmittanceUnit);
			inscatterNProg.setParam("deltaJSampler", deltaJUnit);
            for (int layer = 0; layer < RES_R; ++layer)
            {
                setLayer(inscatterNProg, layer);
                drawQuad(gl);
            }
			inscatterNProg.disable(sc);

            gl.glEnable(GL.GL_BLEND);
            gl.glBlendEquationSeparate(GL.GL_FUNC_ADD, GL.GL_FUNC_ADD);
            gl.glBlendFuncSeparate(GL.GL_ONE, GL.GL_ONE, GL.GL_ONE, GL.GL_ONE);

            // adds deltaE into irradiance texture E (line 10 in algorithm 4.1)
            gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, irradianceTexture[0], 0);
            gl.glViewport(0, 0, SKY_W, SKY_H);
			copyIrradianceProg.enable(sc);
			copyIrradianceProg.setParam("k", new float[]{1.0f});
			copyIrradianceProg.setParam("deltaESampler", deltaEUnit);
            drawQuad(gl);
			copyIrradianceProg.disable(sc);

            // adds deltaS into inscatter texture S (line 11 in algorithm 4.1)
            gl.glFramebufferTextureEXT(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, inscatterTexture[0], 0);
            gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
			copyInscatterNProg.enable(sc);
			copyInscatterNProg.setParam("deltaSSampler", deltaSRUnit);
            for (int layer = 0; layer < RES_R; ++layer)
            {
                setLayer(copyInscatterNProg, layer);
                drawQuad(gl);
            }
			copyInscatterNProg.disable(sc);

            gl.glDisable(GL.GL_BLEND);
        }
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

		return true;
    }

    private void doRecompute(DrawContext dc)
    {
		GL2 gl = dc.getGL().getGL2();
        gl.glDeleteTextures(1, transmittanceTexture, 0);
        gl.glDeleteTextures(1, irradianceTexture, 0);
        gl.glDeleteTextures(1, inscatterTexture, 0);
        gl.glDeleteTextures(1, deltaETexture, 0);
        gl.glDeleteTextures(1, deltaSRTexture, 0);
        gl.glDeleteTextures(1, deltaSMTexture, 0);
        gl.glDeleteTextures(1, deltaJTexture, 0);
        gl.glDeleteFramebuffers(1, fbo, 0);
        doPrecompute(dc);
    }

    private void pushState(GL2 gl)
    {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
    }

    public void precompute(DrawContext dc)
    {
        GL2 gl = dc.getGL().getGL2();

		if(isTexturesDone()){
			return;
		}

        pushState(gl);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glViewport(0, 0, dc.getDrawableWidth(), dc.getDrawableHeight());

		URL trUrl = Atmosphere.class.getResource("transmittance.dat");
        URL irUrl = Atmosphere.class.getResource("irradiance.dat");
        URL inUrl = Atmosphere.class.getResource("inscatter.dat");
		
		if(loadDefaultTextures(gl, trUrl, irUrl, inUrl)){
			texturesDone = true;
		}
		else{
			logger.severe("precompute(): Can't load default textures!");
		}
			
        popState(gl);
    }

    public void popState(GL2 gl)
    {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    private boolean loadDefaultTextures(GL gl, URL transmitance, URL irradiance, URL inscatter)
    {
        synchronized (fileLock)
        {
            URL trUrl = transmitance;
            URL irUrl = irradiance;
            URL inUrl = inscatter;

            File trFile;
            File irFile;
            File inFile;

            if (trUrl == null || irUrl == null || inUrl == null)
            {
                return false;
            }

            try
            {
                trFile = new File(trUrl.toURI());
                irFile = new File(irUrl.toURI());
                inFile = new File(inUrl.toURI());
            } catch (URISyntaxException ex)
            {
				logger.log(Level.SEVERE, "loadData(): exception: {0}", ex.toString());
                return false;
            }

            if (trFile == null || irFile == null || inFile == null)
            {
                return false;
            }

            ByteBuffer trBuffer;
            ByteBuffer irBuffer;
            ByteBuffer inBuffer;

            try
            {
                trBuffer = WWIO.readGZipFileToBuffer(trFile);
                irBuffer = WWIO.readGZipFileToBuffer(irFile);
                inBuffer = WWIO.readGZipFileToBuffer(inFile);
            } catch (IOException ex)
            {
				logger.log(Level.SEVERE, "loadData(): exception: {0}", ex.toString());
                return false;
            }

            if (trBuffer == null || irBuffer == null || inBuffer == null)
            {
                return false;
            }

            gl.glGenTextures(1, transmittanceTexture, 0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, transmittanceTexture[0]);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL.GL_RGB32F, TRANSMITTANCE_W, TRANSMITTANCE_H, 0, GL.GL_RGB, GL.GL_FLOAT, trBuffer);

            gl.glGenTextures(1, irradianceTexture, 0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, irradianceTexture[0]);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB32F, SKY_W, SKY_H, 0, GL.GL_RGB, GL.GL_FLOAT, irBuffer);

            gl.glGenTextures(1, inscatterTexture, 0);
            gl.glBindTexture(GL2.GL_TEXTURE_3D, inscatterTexture[0]);
            gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_EDGE);
            gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
            gl.getGL2().glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL.GL_RGBA32F, RES_MU_S * RES_NU, RES_MU, RES_R, 0, GL.GL_RGBA, GL.GL_FLOAT, inBuffer);

            trBuffer.clear();
            irBuffer.clear();
            inBuffer.clear();

            return true;
        }
    }

    public void saveTextures(GL2 gl)
    {
		synchronized(fileLock){
			Buffer transmittanceBuffer = Buffers.newDirectByteBuffer(8 * 3 * this.TRANSMITTANCE_H * this.TRANSMITTANCE_W);
			Buffer irradinaceBuffer = Buffers.newDirectByteBuffer(8 * 3 * this.SKY_H * this.SKY_W);
			Buffer inscatterBuffer = Buffers.newDirectByteBuffer(8 * 4 * RES_MU_S * RES_NU * RES_MU * RES_R);

			gl.glBindTexture(GL.GL_TEXTURE_2D, transmittanceTexture[0]);
			gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, GL.GL_RGB, GL.GL_FLOAT, transmittanceBuffer);
			gl.glBindTexture(GL.GL_TEXTURE_2D, irradianceTexture[0]);
			gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, GL.GL_RGB, GL.GL_FLOAT, irradinaceBuffer);
			gl.glBindTexture(GL2.GL_TEXTURE_3D, inscatterTexture[0]);
			gl.glGetTexImage(GL2.GL_TEXTURE_3D, 0, GL.GL_RGBA, GL.GL_FLOAT, inscatterBuffer);

			File fileTransmittance = WorldWind.getDataFileStore().newFile(getFilePath("transmittance.dat"));
			File fileIrradinace = WorldWind.getDataFileStore().newFile(getFilePath("irradiance.dat"));
			File fileInscatter = WorldWind.getDataFileStore().newFile(getFilePath("inscatter.dat"));

			try
			{
				WWIO.saveBufferToGZipFile((ByteBuffer) transmittanceBuffer, fileTransmittance);
				transmittanceBuffer.clear();
				WWIO.saveBufferToGZipFile((ByteBuffer) irradinaceBuffer, fileIrradinace);
				irradinaceBuffer.clear();
				WWIO.saveBufferToGZipFile((ByteBuffer) inscatterBuffer, fileInscatter);
				inscatterBuffer.clear();
			} catch (IOException ex)
			{
				logger.log(Level.SEVERE, "loadData(): exception: {0}", ex.toString());
			}
		}
    }

    private static String getFilePath(String file)
    {
        return "Earth/Atmosphere/" + file;
    }
}
