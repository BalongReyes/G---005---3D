package SystemManager.Handlers;

import Core.Main;
import Data.Object3D;
import SystemManager.Listeners.KeyListener;
import Maths.Matrix4;
import Data.Vertex;

public class TickHandler {
    public void tick(Main main) {
        if (main.camera != null) {
            double speed = Settings.WorldSettings.CAMERA_SPEED;
            if (KeyListener.shift)
                speed *= 2.0;
            double dx = 0;
            double dy = 0;
            double dz = 0;

            if (KeyListener.w)
                dz += speed;
            if (KeyListener.s)
                dz -= speed;
            if (KeyListener.a)
                dx -= speed;
            if (KeyListener.d)
                dx += speed;
            if (KeyListener.r)
                dy += speed; // Up
            if (KeyListener.f)
                dy -= speed; // Down

            double rotSpeed = 0.015;
            if (KeyListener.q)
                main.camera.rotY -= rotSpeed;
            if (KeyListener.e)
                main.camera.rotY += rotSpeed;

            if (dx != 0 || dy != 0 || dz != 0) {
                Matrix4 rX = Matrix4.getRotationX(main.camera.rotX);
                Matrix4 rY = Matrix4.getRotationY(main.camera.rotY);
                Matrix4 rZ = Matrix4.getRotationZ(main.camera.rotZ);

                Matrix4 rotMat = rZ.multiply(rY).multiply(rX);
                Vertex moveVec = new Vertex(dx, dy, dz, 0);
                Vertex worldMove = rotMat.multiply(moveVec);

                main.camera.posX += worldMove.x;
                main.camera.posY += worldMove.y;
                main.camera.posZ += worldMove.z;
                
                // Cap camera Y level
                if (main.camera.posY < -200) main.camera.posY = -200;
                if (main.camera.posY > 400) main.camera.posY = 400;
            }
        }
        
        if (main.wfcGenerator != null) {
            main.wfcGenerator.tick();
        }
        
        // Animate Water!
        if (main.waterObject != null) {
            double time = System.currentTimeMillis() / 1000.0;
            double waterLevel = Settings.WorldSettings.WATER_LEVEL;
            double intensity = Settings.WorldSettings.WAVE_INTENSITY;
            for (Vertex v : main.waterObject.vertices) {
                v.y = waterLevel + (Math.sin(v.x * 0.1 + time * 2.0) * 1.2 + Math.cos(v.z * 0.08 + time * 1.5) * 1.2) * intensity;
            }
        }
    }
}
