package si.sed.msh.web.gui.entities;

import java.util.Arrays;
import java.util.List;

public class User {

    private final String username;
    private String currentSedBox;
    private  List<String> mlstBoxes = Arrays.asList(new String[]{"izvrsba@sed-court.si", "k-vpisnik@sed-court.si", "eINS-vpisnik@sed-court.si"});

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getUserSedBoxes() {

        return mlstBoxes;
    }

    public String getCurrentSEDBox() {
        return currentSedBox ==null&& !mlstBoxes.isEmpty()?mlstBoxes.get(0):currentSedBox;
    }

    public void setCurrentSEDBox(String sedBox) {
        currentSedBox = sedBox;
    }

    @Override
    public String toString() {
        return this.getUsername();
    }

}
