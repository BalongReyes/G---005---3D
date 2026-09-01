package Core;
import Settings.*;
import Core.World.*;
public class HeadlessTest {
    public static void main(String[] args) {
        Main main = new Main();
        main.objectHandler = new SystemManager.Handlers.ObjectHandler(); 
        
        main.scalarField = ScalarField.generate(
            Settings.WorldSettings.GRID_SIZE_X, 
            Settings.WorldSettings.GRID_SIZE_Y, 
            Settings.WorldSettings.GRID_SIZE_Z, 
            Settings.WorldSettings.SPACING);
            
        main.wfcGenerator = new Core.WFC.WFCGenerator();
        main.wfcGenerator.init(main, main.scalarField);
        
        System.out.println("Headless WFC init done. Running steps...");
        for (int i=0; i<100; i++) {
            main.wfcGenerator.tick();
        }
    }
}