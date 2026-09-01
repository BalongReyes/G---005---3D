package SystemManager.Listeners;

import Core.Main;
import Maths.Matrix4;
import Data.Vertex;
import org.lwjgl.glfw.GLFW;
import java.awt.Point;

public class MouseListener {

    private final Main main;
    private Point mouseLocation = new Point(0, 0);
    private Point lastMouseLocation = null;
    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;

    public MouseListener(Main main) {
        this.main = main;
    }

    public Point getMouseLocation() {
        return mouseLocation;
    }

    public void invokeCursorPos(double xpos, double ypos) {
        mouseLocation.x = (int) xpos;
        mouseLocation.y = (int) ypos;
        
        if (lastMouseLocation != null && main.camera != null) {
            double dx = mouseLocation.x - lastMouseLocation.x;
            double dy = mouseLocation.y - lastMouseLocation.y;
            
            if (rightMouseDown) {
                double panX = -dx * 0.5;
                double panY = dy * 0.5;
                
                Matrix4 rX = Matrix4.getRotationX(main.camera.rotX);
                Matrix4 rY = Matrix4.getRotationY(main.camera.rotY);
                Matrix4 rZ = Matrix4.getRotationZ(main.camera.rotZ);
                
                Matrix4 rotMat = rZ.multiply(rY).multiply(rX);
                Vertex moveVec = new Vertex(panX, panY, 0, 0);
                Vertex worldMove = rotMat.multiply(moveVec);
                
                main.camera.posX += worldMove.x;
                main.camera.posY += worldMove.y;
                main.camera.posZ += worldMove.z;
            } else if (leftMouseDown) {
                main.camera.rotY += dx * 0.01;
                main.camera.rotX += dy * 0.01;
            }
        }
        
        if (leftMouseDown || rightMouseDown) {
            if (lastMouseLocation == null) {
                lastMouseLocation = new Point();
            }
            lastMouseLocation.setLocation(mouseLocation);
        } else {
            lastMouseLocation = null;
        }
    }

    public void invokeMouseButton(int button, int action, int mods) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            leftMouseDown = (action == GLFW.GLFW_PRESS);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rightMouseDown = (action == GLFW.GLFW_PRESS);
        }
        
        if (action == GLFW.GLFW_RELEASE) {
            lastMouseLocation = null;
        }
    }

    public void invokeScroll(double yoffset) {
        if (main.camera != null) {
            double zoomAmt = yoffset * 15.0;
            
            Matrix4 rX = Matrix4.getRotationX(main.camera.rotX);
            Matrix4 rY = Matrix4.getRotationY(main.camera.rotY);
            Matrix4 rZ = Matrix4.getRotationZ(main.camera.rotZ);
            
            Matrix4 rotMat = rZ.multiply(rY).multiply(rX);
            Vertex moveVec = new Vertex(0, 0, zoomAmt, 0);
            Vertex worldMove = rotMat.multiply(moveVec);
            
            main.camera.posX += worldMove.x;
            main.camera.posY += worldMove.y;
            main.camera.posZ += worldMove.z;
            
            if (main.camera.posY < -200) main.camera.posY = -200;
            if (main.camera.posY > 400) main.camera.posY = 400;
        }
    }
}
