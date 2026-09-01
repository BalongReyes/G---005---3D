package SystemManager.Handlers;

import Data.Object3D;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class ObjectHandler {
    private List<Object3D> objects = new CopyOnWriteArrayList<>();

    public void addObject(Object3D object) {
        objects.add(object);
    }
    
    public void removeObject(Object3D object) {
        objects.remove(object);
    }
    
    public List<Object3D> getObjects() {
        return objects;
    }
}
