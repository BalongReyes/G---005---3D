package SystemManager.Handlers;

import Core.Main;
import Data.Face;
import Data.Object3D;
import Data.Vertex;
import Maths.Matrix4;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * GPU replacement for RenderHandler. Uses OpenGL (via LWJGL) instead of a
 * software rasterizer.
 *
 * WHAT STAYED THE SAME (ported as-is from RenderHandler):
 * - Chunk-level bounding-box frustum cull (isChunkCulled)
 * - Per-face object-space backface cull (isFaceBackfacing)
 * - The identity-transform shortcut for static objects
 * - Opaque-then-transparent draw ordering for correct water blending
 *
 * WHAT GOT DELETED (the GPU now does this in hardware):
 * - Near-plane clipping + clipVertexPool
 * - Perspective divide / screen-space projection math
 * - Per-pixel barycentric rasterization (rasterizeTriangle)
 * - The z-buffer array and the thread-band multithreading
 *
 * INTEGRATION NOTES (things this file can't do for you, since Main/Window/
 * Camera weren't provided):
 * 1. Your window must be a GLFW window with a current OpenGL context, not a
 * Swing Canvas. Create it once at startup:
 *
 * long win = GLFW.glfwCreateWindow(w, h, "Game", 0, 0);
 * GLFW.glfwMakeContextCurrent(win);
 * GL.createCapabilities();
 * glRenderHandler.init(w, h);
 *
 * 2. Your main loop becomes:
 *
 * while (!GLFW.glfwWindowShouldClose(win)) {
 * glRenderHandler.render(main);
 * GLFW.glfwSwapBuffers(win);
 * GLFW.glfwPollEvents();
 * }
 *
 * 3. Debug overlays (F1-F5 HUD text, chunk-bounds wireframes, scalar grid
 * boxes) are NOT ported here. They were drawn via Graphics2D, which no
 * longer applies. Get the 3D view working first; overlay debug drawing
 * (as GL_LINES + a simple text renderer) is a separate follow-up.
 *
 * 4. Winding order: GL_CULL_FACE is left DISABLED below because your mesh
 * generator's vertex winding relative to OpenGL's convention hasn't been
 * verified. The CPU-side backface cull (ported from your original code)
 * already discards ~half the triangles before they reach the GPU, so
 * this only costs a bit of extra GPU vertex work, not correctness. Once
 * you confirm winding (enable it and see if terrain disappears or
 * reverses), flip GL33.glCullFace(GL33.GL_FRONT) if needed, or leave
 * CPU-side culling as the only cull and skip hardware culling entirely.
 */
public class GLRenderHandler {

    public static boolean showChunkBounds = false;
    public static boolean showScalarGridBounds = false;

    private int shaderProgram;
    private int mvpUniformLocation;
    private int matWorldUniformLocation;
    private int drawModeUniformLocation;
    private int depthMapUniformLocation;
    private int nearUniformLocation;
    private int farUniformLocation;
    private int resolutionUniformLocation;
    private int vao, vbo;
    
    private int fbo = 0;
    private int colorTex = 0;
    private int depthTex = 0;
    
    private int lineShaderProgram;
    private int lineMvpUniformLocation;
    private int lineColorUniformLocation;

    private int width, height;

    // Set at the top of each render() call, read by the per-face backface
    // cull (same values the original read from main.camera.posX/Y/Z).
    private double cameraPosX, cameraPosY, cameraPosZ;

    // Reusable scratch buffer for building one object's vertex data per
    // frame. Sized generously; grows only if a single object ever exceeds
    // this many floats (10 floats per vertex: x,y,z, nx,ny,nz, r,g,b,a).
    private FloatBuffer scratch = MemoryUtil.memAllocFloat(10 * 60000);

    // Scratch for the chunk-bounds cull test, same purpose as
    // RenderHandler.boundsScratch.
    private final Vertex[] boundsScratch = new Vertex[8];

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout(location = 0) in vec3 inPos;
            layout(location = 1) in vec3 inNormal;
            layout(location = 2) in vec4 inColor;

            uniform mat4 matWorld;
            uniform mat4 mvp;

            out vec4 vColor;
            out vec3 vNormal;
            out vec3 vWorldPos;

            void main() {
                gl_Position = mvp * vec4(inPos, 1.0);
                vColor = inColor;
                vNormal = mat3(transpose(inverse(matWorld))) * inNormal;
                vWorldPos = (matWorld * vec4(inPos, 1.0)).xyz;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec4 vColor;
            in vec3 vNormal;
            in vec3 vWorldPos;
            
            uniform int drawMode; // 0 = Terrain, 1 = Everything Else
            uniform sampler2D depthMap;
            uniform float near;
            uniform float far;
            uniform vec2 resolution;
            
            out vec4 fragColor;
            
            // --- 3D Simplex Noise ---
            vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
            vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
            vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }
            vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
            float snoise(vec3 v) {
              const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
              const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);
              vec3 i  = floor(v + dot(v, C.yyy) );
              vec3 x0 = v - i + dot(i, C.xxx) ;
              vec3 g = step(x0.yzx, x0.xyz);
              vec3 l = 1.0 - g;
              vec3 i1 = min( g.xyz, l.zxy );
              vec3 i2 = max( g.xyz, l.zxy );
              vec3 x1 = x0 - i1 + C.xxx;
              vec3 x2 = x0 - i2 + C.yyy;
              vec3 x3 = x0 - D.yyy;
              i = mod289(i);
              vec4 p = permute( permute( permute(
                         i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
                       + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
                       + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));
              float n_ = 0.142857142857;
              vec3  ns = n_ * D.wyz - D.xzx;
              vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
              vec4 x_ = floor(j * ns.z);
              vec4 y_ = floor(j - 7.0 * x_ );
              vec4 x = x_ *ns.x + ns.yyyy;
              vec4 y = y_ *ns.x + ns.yyyy;
              vec4 h = 1.0 - abs(x) - abs(y);
              vec4 b0 = vec4( x.xy, y.xy );
              vec4 b1 = vec4( x.zw, y.zw );
              vec4 s0 = floor(b0)*2.0 + 1.0;
              vec4 s1 = floor(b1)*2.0 + 1.0;
              vec4 sh = -step(h, vec4(0.0));
              vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
              vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;
              vec3 p0 = vec3(a0.xy,h.x);
              vec3 p1 = vec3(a0.zw,h.y);
              vec3 p2 = vec3(a1.xy,h.z);
              vec3 p3 = vec3(a1.zw,h.w);
              vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
              p0 *= norm.x;
              p1 *= norm.y;
              p2 *= norm.z;
              p3 *= norm.w;
              vec4 m = max(0.5 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
              m = m * m;
              return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3) ) );
            }
            // ------------------------

            void main() {
                vec3 norm = normalize(vNormal);
                vec3 baseColor = vColor.rgb;
                float alpha = vColor.a;

                // Procedural Texturing for Terrain
                if (drawMode == 0) {
                    // Height and Slope
                    float h = vWorldPos.y;
                    float slope = 1.0 - abs(norm.y); // 0 = flat, 1 = vertical cliff
                    
                    // Add some noise to the slope to make blending organic
                    float n1 = snoise(vWorldPos * 0.1) * 0.5 + 0.5;
                    float n2 = snoise(vWorldPos * 0.02) * 0.5 + 0.5;
                    
                    float noisySlope = slope + (n1 * 0.3 - 0.15);
                    float noisyHeight = h + (n2 * 20.0 - 10.0);
                    
                    // Base Colors
                    vec3 sandColor = vec3(0.93, 0.79, 0.69);
                    vec3 grassColor = vec3(0.13, 0.55, 0.13) * (0.8 + n1 * 0.4); // mottling
                    vec3 rockColor = vec3(0.45, 0.43, 0.42) * (0.9 + n1 * 0.2);
                    vec3 snowColor = vec3(0.95, 0.95, 0.98);
                    
                    vec3 terrainColor = grassColor; // default
                    
                    // Height rules
                    if (noisyHeight < -10.0) { // Near water
                        terrainColor = mix(sandColor, grassColor, smoothstep(-18.0, -5.0, noisyHeight));
                    } else if (noisyHeight > 20.0) { // Mountain tops
                        terrainColor = mix(rockColor, snowColor, smoothstep(20.0, 40.0, noisyHeight));
                    }
                    
                    // Slope rules (cliff faces)
                    if (noisySlope > 0.35) {
                        terrainColor = mix(terrainColor, rockColor, smoothstep(0.35, 0.45, noisySlope));
                    }
                    
                    baseColor = terrainColor;
                }

                // Phong Lighting
                vec3 lightDir = normalize(vec3(0.5, 0.8, -0.3));
                float diff = max(dot(norm, lightDir), 0.0);
                
                // Specular highlight and depth blending for water
                vec3 specular = vec3(0.0);
                if (drawMode == 2) { 
                    vec3 viewDir = normalize(vec3(0.0, 1.0, -1.0)); // Fake view vector
                    vec3 halfDir = normalize(lightDir + viewDir);
                    float spec = pow(max(dot(norm, halfDir), 0.0), 32.0);
                    specular = vec3(1.0) * spec * 0.5;
                    
                    vec2 uv = gl_FragCoord.xy / resolution;
                    float bgDepth = texture(depthMap, uv).r;
                    float zBg = bgDepth * 2.0 - 1.0;
                    float linearBgDepth = (2.0 * near * far) / (far + near - zBg * (far - near));
                    
                    float zWater = gl_FragCoord.z * 2.0 - 1.0;
                    float linearWaterDepth = (2.0 * near * far) / (far + near - zWater * (far - near));
                    float depthDiff = linearBgDepth - linearWaterDepth;
                    
                    vec3 deepColor = vec3(0.0, 0.1, 0.4);
                    vec3 shallowColor = vec3(0.2, 0.7, 0.8);
                    
                    baseColor = mix(shallowColor, deepColor, clamp(depthDiff / 25.0, 0.0, 1.0));
                    alpha = mix(0.3, 0.95, clamp(depthDiff / 15.0, 0.0, 1.0));
                }
                
                vec3 ambient = 0.35 * baseColor;
                vec3 diffuse = diff * baseColor * 0.8;
                
                vec3 result = ambient + diffuse + specular;
                
                // Fog (Removed)
                // float dist = length(vWorldPos.xz - vec2(0.0)); // simple distance
                // float fogFactor = smoothstep(200.0, 500.0, dist);
                // vec3 fogColor = vec3(0.6, 0.7, 0.8);
                
                fragColor = vec4(result, alpha);
            }
            """;
            
    private static final String LINE_VERTEX_SHADER = """
            #version 330 core
            layout(location = 0) in vec3 inPos;
            uniform mat4 mvp;
            void main() {
                gl_Position = mvp * vec4(inPos, 1.0);
            }
            """;
            
    private static final String LINE_FRAGMENT_SHADER = """
            #version 330 core
            uniform vec4 color;
            out vec4 fragColor;
            void main() {
                fragColor = color;
            }
            """;

    public GLRenderHandler() {
        for (int i = 0; i < 8; i++) {
            boundsScratch[i] = new Vertex(0, 0, 0);
        }
    }

    /** Call once after your GL context is current. */
    public void init(int width, int height) {
        this.width = width;
        this.height = height;

        GL33.glEnable(GL33.GL_DEPTH_TEST);
        GL33.glDepthFunc(GL33.GL_LESS);
        GL33.glEnable(GL33.GL_BLEND);
        GL33.glBlendFunc(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA);
        GL33.glEnable(GL33.GL_CULL_FACE);
        GL33.glCullFace(GL33.GL_FRONT);

        shaderProgram = linkProgram(
                compileShader(GL33.GL_VERTEX_SHADER, VERTEX_SHADER),
                compileShader(GL33.GL_FRAGMENT_SHADER, FRAGMENT_SHADER));
        mvpUniformLocation = GL33.glGetUniformLocation(shaderProgram, "mvp");
        matWorldUniformLocation = GL33.glGetUniformLocation(shaderProgram, "matWorld");
        drawModeUniformLocation = GL33.glGetUniformLocation(shaderProgram, "drawMode");
        depthMapUniformLocation = GL33.glGetUniformLocation(shaderProgram, "depthMap");
        nearUniformLocation = GL33.glGetUniformLocation(shaderProgram, "near");
        farUniformLocation = GL33.glGetUniformLocation(shaderProgram, "far");
        resolutionUniformLocation = GL33.glGetUniformLocation(shaderProgram, "resolution");

        lineShaderProgram = linkProgram(
                compileShader(GL33.GL_VERTEX_SHADER, LINE_VERTEX_SHADER),
                compileShader(GL33.GL_FRAGMENT_SHADER, LINE_FRAGMENT_SHADER));
        lineMvpUniformLocation = GL33.glGetUniformLocation(lineShaderProgram, "mvp");
        lineColorUniformLocation = GL33.glGetUniformLocation(lineShaderProgram, "color");

        vao = GL33.glGenVertexArrays();
        vbo = GL33.glGenBuffers();
        GL33.glBindVertexArray(vao);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);

        int stride = 10 * Float.BYTES;
        GL33.glVertexAttribPointer(0, 3, GL33.GL_FLOAT, false, stride, 0);
        GL33.glEnableVertexAttribArray(0);
        GL33.glVertexAttribPointer(1, 3, GL33.GL_FLOAT, false, stride, 3L * Float.BYTES);
        GL33.glEnableVertexAttribArray(1);
        GL33.glVertexAttribPointer(2, 4, GL33.GL_FLOAT, false, stride, 6L * Float.BYTES);
        GL33.glEnableVertexAttribArray(2);

        GL33.glBindVertexArray(0);
    }

    /** Call when the window/framebuffer is resized. */
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        GL33.glViewport(0, 0, width, height);
        
        if (fbo == 0) {
            fbo = GL33.glGenFramebuffers();
            colorTex = GL33.glGenTextures();
            depthTex = GL33.glGenTextures();
        }
        
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, fbo);

        GL33.glBindTexture(GL33.GL_TEXTURE_2D, colorTex);
        GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, width, height, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer)null);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
        GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0, GL33.GL_TEXTURE_2D, colorTex, 0);

        GL33.glBindTexture(GL33.GL_TEXTURE_2D, depthTex);
        GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_DEPTH_COMPONENT24, width, height, 0, GL33.GL_DEPTH_COMPONENT, GL33.GL_FLOAT, (java.nio.ByteBuffer)null);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
        GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_DEPTH_ATTACHMENT, GL33.GL_TEXTURE_2D, depthTex, 0);

        if (GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER) != GL33.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("FBO is not complete!");
        }
        
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, 0);
    }

    public void shutdown() {
        MemoryUtil.memFree(scratch);
        GL33.glDeleteBuffers(vbo);
        GL33.glDeleteVertexArrays(vao);
        GL33.glDeleteProgram(shaderProgram);
        GL33.glDeleteProgram(lineShaderProgram);
    }

    private record VisibleObject(Object3D obj, Matrix4 matWorld, Matrix4 matWorldView, Matrix4 mvp) {
    }

    public void render(Main main) {
        if (main.objectHandler == null || main.camera == null)
            return;

        cameraPosX = main.camera.posX;
        cameraPosY = main.camera.posY;
        cameraPosZ = main.camera.posZ;

        GL33.glClearColor(0f, 0f, 0f, 1f);
        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);

        double aspectRatio = (double) height / (double) width;
        // Use near=2.0 to give massive depth buffer precision boost to far terrain
        Matrix4 matProj = Matrix4.getProjection(90.0, aspectRatio, 2.0, 1000.0);

        Matrix4 camTrans = Matrix4.getTranslation(-main.camera.posX, -main.camera.posY, -main.camera.posZ);
        Matrix4 camRotX = Matrix4.getRotationX(-main.camera.rotX);
        Matrix4 camRotY = Matrix4.getRotationY(-main.camera.rotY);
        Matrix4 camRotZ = Matrix4.getRotationZ(-main.camera.rotZ);
        Matrix4 matView = camRotX.multiply(camRotY).multiply(camRotZ).multiply(camTrans);

        // -----------------------------------------------------
        // PASS 0: cull, and cache per-object matrices for the two draw
        // passes below (avoids recomputing matWorldView / the cull test
        // twice).
        // -----------------------------------------------------
        List<VisibleObject> visible = new ArrayList<>();

        for (Object3D obj : main.objectHandler.getObjects()) {
            boolean isIdentityTransform = obj.posX == 0 && obj.posY == 0 && obj.posZ == 0
                    && obj.rotX == 0 && obj.rotY == 0 && obj.rotZ == 0;

            Matrix4 matWorldView;
            Matrix4 matWorld;
            if (isIdentityTransform) {
                matWorld = Matrix4.getIdentity();
                matWorldView = matView;
            } else {
                Matrix4 matRotZ = Matrix4.getRotationZ(obj.rotZ);
                Matrix4 matRotX = Matrix4.getRotationX(obj.rotX);
                Matrix4 matRotY = Matrix4.getRotationY(obj.rotY);
                Matrix4 matTrans = Matrix4.getTranslation(obj.posX, obj.posY, obj.posZ);
                matWorld = matTrans.multiply(matRotY).multiply(matRotX).multiply(matRotZ);
                matWorldView = matView.multiply(matWorld);
            }

            if (isChunkCulled(obj, matWorldView, matProj)) {
                continue;
            }
            if (obj.faces == null) {
                continue;
            }

            Matrix4 mvp = matProj.multiply(matWorldView);
            visible.add(new VisibleObject(obj, matWorld, matWorldView, mvp));
        }

        // -----------------------------------------------------
        // PASS 1: OPAQUE (Render to FBO)
        // -----------------------------------------------------
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, fbo);
        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);

        GL33.glUseProgram(shaderProgram);
        GL33.glBindVertexArray(vao);
        
        GL33.glUniform1f(nearUniformLocation, 2.0f);
        GL33.glUniform1f(farUniformLocation, 1000.0f);
        GL33.glUniform2f(resolutionUniformLocation, (float)width, (float)height);
        GL33.glUniform1i(depthMapUniformLocation, 0);

        for (VisibleObject vo : visible) {
            drawObjectPass(vo, false);
        }
        
        // -----------------------------------------------------
        // PASS 2: BLIT OPAQUE TO SCREEN
        // -----------------------------------------------------
        GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER, fbo);
        GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER, 0);
        GL33.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT, GL33.GL_NEAREST);

        // -----------------------------------------------------
        // PASS 3: TRANSPARENT (Render directly to screen, reading depth)
        // -----------------------------------------------------
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, 0);
        
        GL33.glActiveTexture(GL33.GL_TEXTURE0);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, depthTex);

        GL33.glDepthMask(false);
        for (VisibleObject vo : visible) {
            drawObjectPass(vo, true);
        }
        GL33.glDepthMask(true);

        GL33.glBindVertexArray(0);
        GL33.glUseProgram(0);
        
        drawDebugPass(main, matProj, matView);
    }

    private void drawDebugPass(Main main, Matrix4 matProj, Matrix4 matView) {
        if (!showChunkBounds && !showScalarGridBounds) return;
        
        GL33.glUseProgram(lineShaderProgram);
        GL33.glBindVertexArray(vao);
        GL33.glDisable(GL33.GL_DEPTH_TEST);
        
        if (showChunkBounds) {
            GL33.glUniform4f(lineColorUniformLocation, 1f, 1f, 0f, 1f); // Yellow
            for (Object3D obj : main.objectHandler.getObjects()) {
                if (obj.minX == Double.MAX_VALUE) continue;
                
                scratch.clear();
                
                Matrix4 matWorldView;
                boolean isIdentityTransform = obj.posX == 0 && obj.posY == 0 && obj.posZ == 0
                        && obj.rotX == 0 && obj.rotY == 0 && obj.rotZ == 0;
                if (isIdentityTransform) {
                    matWorldView = matView;
                } else {
                    Matrix4 matRotZ = Matrix4.getRotationZ(obj.rotZ);
                    Matrix4 matRotX = Matrix4.getRotationX(obj.rotX);
                    Matrix4 matRotY = Matrix4.getRotationY(obj.rotY);
                    Matrix4 matTrans = Matrix4.getTranslation(obj.posX, obj.posY, obj.posZ);
                    Matrix4 matWorld = matTrans.multiply(matRotY).multiply(matRotX).multiply(matRotZ);
                    matWorldView = matView.multiply(matWorld);
                }
                Matrix4 mvp = matProj.multiply(matWorldView);
                
                double mx = obj.minX, my = obj.minY, mz = obj.minZ;
                double Mx = obj.maxX, My = obj.maxY, Mz = obj.maxZ;
                
                putLineVertex(mx, my, mz); putLineVertex(Mx, my, mz);
                putLineVertex(Mx, my, mz); putLineVertex(Mx, my, Mz);
                putLineVertex(Mx, my, Mz); putLineVertex(mx, my, Mz);
                putLineVertex(mx, my, Mz); putLineVertex(mx, my, mz);
                
                putLineVertex(mx, My, mz); putLineVertex(Mx, My, mz);
                putLineVertex(Mx, My, mz); putLineVertex(Mx, My, Mz);
                putLineVertex(Mx, My, Mz); putLineVertex(mx, My, Mz);
                putLineVertex(mx, My, Mz); putLineVertex(mx, My, mz);
                
                putLineVertex(mx, my, mz); putLineVertex(mx, My, mz);
                putLineVertex(Mx, my, mz); putLineVertex(Mx, My, mz);
                putLineVertex(Mx, my, Mz); putLineVertex(Mx, My, Mz);
                putLineVertex(mx, my, Mz); putLineVertex(mx, My, Mz);
                
                scratch.flip();
                GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
                GL33.glBufferData(GL33.GL_ARRAY_BUFFER, scratch, GL33.GL_DYNAMIC_DRAW);
                uploadMvpLine(mvp);
                GL33.glDrawArrays(GL33.GL_LINES, 0, 24);
            }
        }
        
        if (showScalarGridBounds && main.scalarField != null) {
            GL33.glUniform4f(lineColorUniformLocation, 0f, 1f, 1f, 1f); // Cyan
            Matrix4 mvp = matProj.multiply(matView);
            int sx = main.scalarField.sizeX;
            int sy = main.scalarField.sizeY;
            int sz = main.scalarField.sizeZ;
            double sp = Settings.WorldSettings.SPACING;
            
            double mx = -((sx - 1) * sp) / 2.0;
            double my = -((sy - 1) * sp) / 2.0;
            double mz = -((sz - 1) * sp) / 2.0;
            double Mx = mx + (sx - 1) * sp;
            double My = my + (sy - 1) * sp;
            double Mz = mz + (sz - 1) * sp;
            
            putLineVertex(mx, my, mz); putLineVertex(Mx, my, mz);
            putLineVertex(Mx, my, mz); putLineVertex(Mx, my, Mz);
            putLineVertex(Mx, my, Mz); putLineVertex(mx, my, Mz);
            putLineVertex(mx, my, Mz); putLineVertex(mx, my, mz);
            
            putLineVertex(mx, My, mz); putLineVertex(Mx, My, mz);
            putLineVertex(Mx, My, mz); putLineVertex(Mx, My, Mz);
            putLineVertex(Mx, My, Mz); putLineVertex(mx, My, Mz);
            putLineVertex(mx, My, Mz); putLineVertex(mx, My, mz);
            
            putLineVertex(mx, my, mz); putLineVertex(mx, My, mz);
            putLineVertex(Mx, my, mz); putLineVertex(Mx, My, mz);
            putLineVertex(Mx, my, Mz); putLineVertex(Mx, My, Mz);
            putLineVertex(mx, my, Mz); putLineVertex(mx, My, Mz);
            
            scratch.flip();
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
            GL33.glBufferData(GL33.GL_ARRAY_BUFFER, scratch, GL33.GL_DYNAMIC_DRAW);
            uploadMvpLine(mvp);
            GL33.glDrawArrays(GL33.GL_LINES, 0, 24);
            scratch.clear();
        }
        
        GL33.glEnable(GL33.GL_DEPTH_TEST);
        GL33.glBindVertexArray(0);
        GL33.glUseProgram(0);
    }
    
    private void putLineVertex(double x, double y, double z) {
        if (scratch.remaining() < 10) growScratch();
        scratch.put((float) x).put((float) y).put((float) z);
        scratch.put(0f).put(0f).put(0f);
        scratch.put(0f).put(0f).put(0f).put(0f);
    }
    
    private void uploadMvpLine(Matrix4 mvp) {
        float[] flat = new float[16];
        int k = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                flat[k++] = (float) mvp.m[i][j];
            }
        }
        GL33.glUniformMatrix4fv(lineMvpUniformLocation, true, flat);
    }

    /**
     * Builds this object's vertex buffer for one pass (opaque or
     * transparent) and issues the draw call. Faces are filtered by
     * backface-cull and by alpha, exactly like the original's per-face
     * loop -- just without clipping/projection, since the GPU does that.
     */
    private void drawObjectPass(VisibleObject vo, boolean transparentPass) {
        Object3D obj = vo.obj();

        scratch.clear();
        int vertexCount = 0;

        for (Face face : obj.faces) {
            boolean faceIsTransparent = face.color.getAlpha() < 255;
            if (faceIsTransparent != transparentPass) {
                continue;
            }

            if (isFaceBackfacing(obj, face, cameraPosX, cameraPosY, cameraPosZ)) {
                continue;
            }

            int edgeCount = face.edges.length;
            float a = face.color.getAlpha() / 255f;

            // Fan triangulation, same pattern as the original's
            // rasterizePolygon: (0, i, i+1) for i in [1, edgeCount-2].
            for (int i = 1; i < edgeCount - 1; i++) {
                vertexCount += putTriangleVertex(obj.vertices[face.edges[0].v1], face.color, a);
                vertexCount += putTriangleVertex(obj.vertices[face.edges[i].v1], face.color, a);
                vertexCount += putTriangleVertex(obj.vertices[face.edges[i + 1].v1], face.color, a);
            }
        }

        if (vertexCount == 0) {
            return;
        }

        scratch.flip();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, scratch, GL33.GL_DYNAMIC_DRAW);

        uploadMvp(vo.mvp());
        uploadMatWorld(vo.matWorld());
        
        int drawMode = obj.isTerrain ? 0 : (transparentPass ? 2 : 1); 
        
        GL33.glUniform1i(drawModeUniformLocation, drawMode);
        
        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, vertexCount);
    }

    /**
     * Appends one vertex (object-space position + normalized color) to the scratch
     * buffer. Returns 1.
     */
    private int putTriangleVertex(Vertex v, java.awt.Color faceColor, float alpha) {
        if (scratch.remaining() < 10) {
            growScratch();
        }
        scratch.put((float) v.x).put((float) v.y).put((float) v.z);
        scratch.put((float) v.nx).put((float) v.ny).put((float) v.nz);
        float r = (v.r / 255f) * (faceColor.getRed() / 255f);
        float g = (v.g / 255f) * (faceColor.getGreen() / 255f);
        float b = (v.b / 255f) * (faceColor.getBlue() / 255f);
        scratch.put(r).put(g).put(b).put(alpha);
        return 1;
    }

    private void growScratch() {
        FloatBuffer bigger = MemoryUtil.memAllocFloat(scratch.capacity() * 2);
        scratch.flip();
        bigger.put(scratch);
        MemoryUtil.memFree(scratch);
        scratch = bigger;
    }

    /**
     * Ported from RenderHandler's chunk-level frustum cull (lines 192-254 of
     * the original). Debug box collection was dropped since debug overlays
     * aren't ported yet.
     */
    private boolean isChunkCulled(Object3D obj, Matrix4 matWorldView, Matrix4 matProj) {
        if (obj.minX == Double.MAX_VALUE) {
            return false; // no bounding box on this object -- never culled here
        }

        Vertex[] bounds = boundsScratch;
        setXYZ(bounds[0], obj.minX, obj.minY, obj.minZ);
        setXYZ(bounds[1], obj.maxX, obj.minY, obj.minZ);
        setXYZ(bounds[2], obj.maxX, obj.maxY, obj.minZ);
        setXYZ(bounds[3], obj.minX, obj.maxY, obj.minZ);
        setXYZ(bounds[4], obj.minX, obj.minY, obj.maxZ);
        setXYZ(bounds[5], obj.maxX, obj.minY, obj.maxZ);
        setXYZ(bounds[6], obj.maxX, obj.maxY, obj.maxZ);
        setXYZ(bounds[7], obj.minX, obj.maxY, obj.maxZ);

        boolean allBehind = true, someBehind = false;
        boolean allLeft = true, allRight = true, allTop = true, allBottom = true;

        for (Vertex v : bounds) {
            Vertex vw = matWorldView.multiply(v);

            if (vw.z > 0.1) {
                allBehind = false;
            } else {
                someBehind = true;
            }

            Vertex pv = matProj.multiply(vw);
            if (pv.w != 0) {
                pv.x /= pv.w;
                pv.y /= pv.w;
            }

            double sx = (pv.x + 1.0) * 0.5 * width;
            double sy = (1.0 - pv.y) * 0.5 * height;

            if (sx >= 0)
                allLeft = false;
            if (sx <= width)
                allRight = false;
            if (sy >= 0)
                allTop = false;
            if (sy <= height)
                allBottom = false;
        }

        if (allBehind) {
            return true;
        }
        if (!someBehind && (allLeft || allRight || allTop || allBottom)) {
            return true;
        }
        return false;
    }

    /**
     * Ported from RenderHandler's 3D backface cull (lines 283-312 of the
     * original), including its existing assumption that object-space
     * vertices can be compared directly against the world-space camera
     * position -- true for identity-transform objects (the common case
     * here), same as in the original.
     */
    private boolean isFaceBackfacing(Object3D obj, Face face,
            double cameraX, double cameraY, double cameraZ) {
        Vertex objV1 = obj.vertices[face.edges[0].v1];
        Vertex objV2 = obj.vertices[face.edges[0].v2];
        Vertex objV3 = obj.vertices[face.edges[1].v1];
        if (objV3 == objV1 || objV3 == objV2) {
            objV3 = obj.vertices[face.edges[1].v2];
        }

        double ax = objV2.x - objV1.x, ay = objV2.y - objV1.y, az = objV2.z - objV1.z;
        double bx = objV3.x - objV1.x, by = objV3.y - objV1.y, bz = objV3.z - objV1.z;

        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;

        // Camera view vector, compared in the same (object-space-as-if-world)
        // frame the original used -- correct for identity-transform objects,
        // which the render loop's isIdentityTransform shortcut confirms is
        // the common case here.
        double cx = objV1.x - cameraX;
        double cy = objV1.y - cameraY;
        double cz = objV1.z - cameraZ;

        return (nx * cx + ny * cy + nz * cz) >= 0;
    }

    private static void setXYZ(Vertex v, double x, double y, double z) {
        v.x = x;
        v.y = y;
        v.z = z;
    }

    private void uploadMvp(Matrix4 mvp) {
        float[] flat = new float[16];
        int k = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                flat[k++] = (float) mvp.m[i][j];
            }
        }
        // transpose = true: Matrix4 stores row-major (m[row][col]) same as
        // the original engine's convention; GLSL mat4 expects column-major,
        // so we let glUniformMatrix4fv do the transpose instead of flipping
        // it ourselves.
        GL33.glUniformMatrix4fv(mvpUniformLocation, true, flat);
    }
    
    private void uploadMatWorld(Matrix4 matWorld) {
        float[] flat = new float[16];
        int k = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                flat[k++] = (float) matWorld.m[i][j];
            }
        }
        GL33.glUniformMatrix4fv(matWorldUniformLocation, true, flat);
    }

    private int compileShader(int type, String source) {
        int shader = GL33.glCreateShader(type);
        GL33.glShaderSource(shader, source);
        GL33.glCompileShader(shader);
        if (GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Shader compile failed: " + GL33.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private int linkProgram(int vertexShader, int fragmentShader) {
        int program = GL33.glCreateProgram();
        GL33.glAttachShader(program, vertexShader);
        GL33.glAttachShader(program, fragmentShader);
        GL33.glLinkProgram(program);
        if (GL33.glGetProgrami(program, GL33.GL_LINK_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Program link failed: " + GL33.glGetProgramInfoLog(program));
        }
        GL33.glDeleteShader(vertexShader);
        GL33.glDeleteShader(fragmentShader);
        return program;
    }
}