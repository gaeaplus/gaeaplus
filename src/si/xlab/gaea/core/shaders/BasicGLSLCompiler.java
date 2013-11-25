package si.xlab.gaea.core.shaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import gov.nasa.worldwind.Disposable;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

/**
 *
 * @author vito
 */
public class BasicGLSLCompiler implements Disposable{

	protected static final Logger logger = Logger.getLogger(BasicGLSLCompiler.class.getName());

	private String shadersSupportedString = "";
	private float shadersVersion = 0.0f;
	private boolean init = false;

	public boolean isShaderVersionSupported(float version){
		return (version <= this.shadersVersion) ? true : false;
	}

	public void defineSupportedShaders(GL gl){

		if(init){
			return;
		}

		String out = "";

		String fullShaderVeersion = gl.glGetString(GL2.GL_SHADING_LANGUAGE_VERSION);
		float shaderVeersion = Float.parseFloat(fullShaderVeersion.split(" ")[0]);

		if(1.1 <= shaderVeersion)
			out += "#define v110\n";
		if(1.2 <= shaderVeersion)
			out += "#define v120\n";
		if(1.3 <= shaderVeersion)
			out += "#define v130\n";
		if(1.4 <= shaderVeersion)
			out += "#define v140\n";
		if(1.5 <= shaderVeersion)
			out += "#define v150\n";

		this.shadersVersion = shaderVeersion;

		String infoMessage = "Supported shader profiles: \n" + out;
		logger.info(infoMessage);

		this.shadersSupportedString = out;

		init = true;
	}

	private boolean checkProgramError(GL2 gl, int programId){

		if(!gl.glIsProgram(programId)){
			return false;
		}
		gl.glValidateProgram(programId);
		int[] isValid = new int[1];
		gl.glGetProgramiv(programId, GL2.GL_VALIDATE_STATUS, isValid, 0);

		if(isValid[0] == GL.GL_FALSE){
			return false;
		}

		return true;
	}

	private String combineLines(String[] strings){
		String out = "";
		int i=1;
		for(String s : strings){
			out += i+": "+s;
			i++;
		}
		return out;
	}

	private void printSource(String[] source) {
		System.out.println("Shader source:");
		System.out.println("");
		System.out.println(combineLines(source));
		System.out.println("");
	}
	
	private boolean checkShaderError(GL2 gl, int shaderId)
	{
		int[] status = new int[1];
		gl.glGetShaderiv(shaderId, GL2.GL_COMPILE_STATUS, status, 0);
		
		if (status[0] == GL2.GL_FALSE){
			int[] logLength = new int[1];
			gl.glGetShaderiv(shaderId, GL2.GL_INFO_LOG_LENGTH, logLength, 0);
			if (logLength[0] > 1) {
				byte[] log = new byte[logLength[0]];
				gl.glGetShaderInfoLog(shaderId, logLength[0], (int[])null, 0, log, 0);
                if (log[log.length-1] == 0)
                    log[log.length-1] = ' ';
				String msg = "Error compiling the shader: " + new String(log);
				logger.severe(msg);
			}
			return false;
		}
		else{
			int[] logLength = new int[1];
			gl.glGetShaderiv(shaderId, GL2.GL_INFO_LOG_LENGTH, logLength, 0);
			if (logLength[0] > 1) {
				byte[] log = new byte[logLength[0]];
				gl.glGetShaderInfoLog(shaderId, logLength[0], (int[])null, 0, log, 0);
                if (log[log.length-1] == 0)
                    log[log.length-1] = ' ';
				String msg = "Shader message: " + new String(log);
				logger.info(msg);
			}	
			//String msg = "Shader successfully compiled!";
			//logger.info(msg);
			return true;
		}
	}

	private boolean checkShaderLinkError(GL2 gl, int programId){
		int[] status = new int[1];
		gl.glGetProgramiv(programId, GL2.GL_LINK_STATUS, status, 0);

		if(status[0] == GL2.GL_FALSE){
			int[] logLength = new int[1];
			gl.glGetObjectParameterivARB(programId, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, logLength, 0);
			if (logLength[0] > 1) {
				byte[] log = new byte[logLength[0]];
				gl.glGetInfoLogARB(programId, logLength[0], (int[])null, 0, log, 0);
				String msg = "Error linking the shader program: " + new String(log);
				logger.severe(msg);
			}
			else{
				String msg = "Error linking the shader program: " + "NO ERROR REPORT!";
				logger.severe(msg);	
			}
			return false;
		}
		else{
			int[] logLength = new int[1];
			gl.glGetObjectParameterivARB(programId, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, logLength, 0);
			if (logLength[0] > 1) {
				byte[] log = new byte[logLength[0]];
				gl.glGetInfoLogARB(programId, logLength[0], (int[])null, 0, log, 0);
				String msg = "Shader successfully linked: " + new String(log);
				logger.info(msg);
			}
			else{
				//String msg = "Shader successfully linked!";
				//logger.info(msg);	
			}
			gl.glValidateProgram(programId);
			
			byte[] log = new byte[1000];
			gl.glGetProgramInfoLog(programId, 1000, logLength , 0, log, 0);
			if (logLength[0] > 0) {
				String msg = "Shader validation: " + new String(Arrays.copyOfRange(log, 0, logLength[0]));
				logger.info(msg);	
			}
			return true;
		}
	}

	private void printGLLog(GL2 gl, GLU glu){
		while(true){
			int error = gl.glGetError();
			
			if(error == GL2.GL_NO_ERROR)
			{
				break;
			}
			else{
				String msg = "OpenGL error number: " + error + " description: "+ glu.gluErrorString(error);
				logger.severe(msg);
			}
		}
	}

	private String readFile(URL fileUrl) throws IOException
	{
		HashSet<String> loadedFiles = new HashSet<String>();
		String cgCode = doReadFile(fileUrl, loadedFiles);
		loadedFiles.clear();
		return cgCode;
	}

	private String doReadFile(URL fileUrl, HashSet<String> loadedFiles) throws IOException
	{
		if(fileUrl == null){
			String msg = "Shader URL is null!!";
			logger.severe(msg);
			throw new IOException("Filepath: "+ fileUrl.getFile());
		}

		if(loadedFiles.contains(fileUrl.getFile())){
			return "";
		}


		BufferedReader brv = null;

		InputStream fileStream = null;

		fileStream = fileUrl.openStream();

		if(fileStream == null){
			String message = "Shader not found: " + fileUrl.getFile();
			logger.severe(message);
			throw new IOException("Filepath: "+ fileUrl.getFile());
		}

		brv = new BufferedReader(new InputStreamReader(fileStream));

		String src = "";
		String line;

		int lineNumber = 1;

			while ((line = brv.readLine()) != null)
			{
				if(line.contains("#include")){

					String[] stringArray = line.trim().split(" ");
					String includeFile = null;

					for(String s : stringArray){
						s = s.trim();
						if(s.startsWith("\"") && s.endsWith("\"")){
							includeFile = s;
						}
					}

					if(includeFile == null){
						String message = "ERROR in line: " + lineNumber;
						logger.severe(message);
						String msg = "can not parse #include line in shader!";
						logger.severe(msg);
						throw new IOException("Filepath: "+ fileUrl.getFile());
					}

					includeFile = includeFile.replaceAll("\"", "");
					String includeFilePath = fileUrl.toString().replaceFirst("[^/]+$", includeFile);
					URL includeURL = new URL(includeFilePath);

					src += doReadFile(includeURL, loadedFiles);
				}
				else{
					src += line + "\n";
					lineNumber = lineNumber + 1;
				}
			}
			brv.close();
			fileStream.close();
		loadedFiles.add(fileUrl.getFile());

		return src;
	}

	protected boolean loadProgram(BasicGLSLShader shader)
	{
		if (GLContext.getCurrent() == null) {
			String msg = "ShaderContext.getShader(): GLContext not current on this thread";
			logger.severe(msg);
		}

		try {
			GL2 gl = GLContext.getCurrent().getGL().getGL2();
			GLU glu = new GLU();

			URL fileUrl = shader.getURL();
			String startString = shader.getRuntimeCode();

			this.printGLLog(gl, glu);

			int programId = gl.glCreateProgram();
			if (!gl.glIsProgram(programId)) {
				return false;
			}

			int vertexShaderId = 0;
			int geometryShaderId = 0;
			int fragmentShaderId = 0;

			vertexShaderId = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
			fragmentShaderId = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);


			gl.glAttachShader(programId, vertexShaderId);
			gl.glAttachShader(programId, fragmentShaderId);

			String shaderSrc;
			try{
				shaderSrc = this.readFile(fileUrl);
			}
			catch(IOException e){
				logger.log(Level.SEVERE, "Error reading shader: {0}", fileUrl.getFile());
				logger.log(Level.SEVERE, "Error message: {0}", e.getMessage());
				return false;
			}
			
			Pattern p = Pattern.compile(".*\\_GEOMETRY\\_.*");
			Matcher m;

			boolean geo = false;
			m = p.matcher(shaderSrc);
			while (m.find()) {
				if (GLContext.getCurrent().isGL3()) {
					geo = true;
				} else {
					return false;
				}
			}

			String[] currentSrc;

			String infoMessage = "Compiling shader: " + fileUrl.getFile();
			logger.info(infoMessage);
			
			String vsSrc = "";
			vsSrc += startString;
			vsSrc += this.shadersSupportedString;
			vsSrc += "#define _VERTEX_\n";
			vsSrc += shaderSrc;
			currentSrc = vsSrc.split("\r?\n|\r");

			for (int i = 0; i < currentSrc.length; i++) {
				currentSrc[i] += "\n";
			}

			gl.glShaderSource(vertexShaderId, currentSrc.length, currentSrc, null, 0);
			gl.glCompileShader(vertexShaderId);

			if (!checkShaderError(gl, vertexShaderId)) {
				printSource(currentSrc);
				gl.glDeleteProgram(programId);
				return false;
			}

			if (geo) {
				geometryShaderId = gl.glCreateShader(GL3.GL_GEOMETRY_SHADER);
				gl.glAttachShader(programId, geometryShaderId);


				String gsSrc = "";
				gsSrc += startString;
				gsSrc += this.shadersSupportedString;
				gsSrc += "#define _GEOMETRY_\n";
				gsSrc += shaderSrc;
				currentSrc = gsSrc.split("\r?\n|\r");

				for (int i = 0; i < currentSrc.length; i++) {
					currentSrc[i] += "\n";
				}

				gl.glShaderSource(geometryShaderId, currentSrc.length, currentSrc, null, 0);
				gl.glCompileShader(geometryShaderId);

				if (!checkShaderError(gl, geometryShaderId)) {
					printSource(currentSrc);
					gl.glDeleteProgram(programId);
					return false;
				}
			}

			String fsSrc = "";
			fsSrc += startString;
			fsSrc += this.shadersSupportedString;
			fsSrc += "#define _FRAGMENT_\n";
			fsSrc += shaderSrc;
			currentSrc = fsSrc.split("\r?\n|\r");

			for (int i = 0; i < currentSrc.length; i++) {
				currentSrc[i] += "\n";
			}

			gl.glShaderSource(fragmentShaderId, currentSrc.length, currentSrc, null, 0);
			gl.glCompileShader(fragmentShaderId);

			if (!checkShaderError(gl, fragmentShaderId)) {
				printSource(currentSrc);
				gl.glDeleteProgram(programId);
				return false;
			}

			gl.glLinkProgram(programId);
			if (!checkShaderLinkError(gl, programId)) {
				gl.glDeleteProgram(programId);
				return false;
			}

			if (!checkProgramError(gl, programId)) {
				gl.glDeleteProgram(programId);
				return false;
			}

			shader.setFS(vertexShaderId);
			shader.setGS(geometryShaderId);
			shader.setVS(fragmentShaderId);
			shader.setProgram(programId);
			printGLLog(gl, glu);
		} 
		catch (GLException e) {
			return false;
		}

		return true;
	}

	@Override
	public void dispose()
	{
	}
}

