package net.bteuk.network.gui.progressmap;

import me.bteuk.progressmapper.guis.ColourPicker;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.utils.NetworkUser;

public class ColourPickerGUI extends NetworkRefreshableGui {
    private final ColourPicker colourPicker;
    private final FeaturePageGUI parentFeatureMenu;

    public ColourPickerGUI(GuiProvider provider, ColourPicker colourPicker, FeaturePageGUI parentFeatureMenu) {
        super(provider, colourPicker.getGUI());
        this.colourPicker = colourPicker;
        this.parentFeatureMenu = parentFeatureMenu;
    }

    protected void createGui() {
        setItemsFromInventory(colourPicker.getGUI());
        setActions();
    }

    private void setActions() {
        // Slots here are 0 indexed

        //----------------------------------------------------
        //----------- Top Row - Predefined colours -----------
        //----------------------------------------------------

        // Default 1
        setAction(0, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Dark_Red);
            refresh();
        });

        // Default 2
        setAction(1, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Red);
            refresh();
        });

        // Default 3
        setAction(2, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Dark_Orange);
            refresh();
        });

        // Default 4
        setAction(3, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Orange);
            refresh();
        });

        // Default 5
        setAction(4, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Light_Orange);
            refresh();
        });

        // Default 6
        setAction(5, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Yellow);
            refresh();
        });

        // Default 7
        setAction(6, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Bright_Yellow);
            refresh();
        });

        // Default 8
        setAction(7, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Sick_Green);
            refresh();
        });

        // Default 9
        setAction(8, (NetworkUser u) -> {
            colourPicker.updateColour(ColourPicker.Complete_Green);
            refresh();
        });

        //-----------------------------------------------------
        //---------------- Line 2 - Red Editor ----------------
        //-----------------------------------------------------

        // Lower red 16
        setAction(11, (NetworkUser u) -> {
            colourPicker.lowerRed16();
            refresh();
        });

        // Lower red 1
        setAction(12, (NetworkUser u) -> {
            colourPicker.lowerRed1();
            refresh();
        });

        // Raise red 1
        setAction(14, (NetworkUser u) -> {
            colourPicker.raiseRed1();
            refresh();
        });

        // Raise red 16
        setAction(15, (NetworkUser u) -> {
            colourPicker.raiseRed16();
            refresh();
        });

        //-----------------------------------------------------
        //--------------- Line 3 - Green Editor ---------------
        //-----------------------------------------------------

        // Lower green 16
        setAction(20, (NetworkUser u) -> {
            colourPicker.lowerGreen16();
            refresh();
        });

        // Lower green 1
        setAction(21, (NetworkUser u) -> {
            colourPicker.lowerGreen1();
            refresh();
        });

        // Raise green 1
        setAction(23, (NetworkUser u) -> {
            colourPicker.raiseGreen1();
            refresh();
        });

        // Raise green 16
        setAction(24, (NetworkUser u) -> {
            colourPicker.raiseGreen16();
            refresh();
        });

        //----------------------------------------------------
        //--------------- Line 4 - Blue Editor ---------------
        //----------------------------------------------------

        // Lower blue 16
        setAction(29, (NetworkUser u) -> {
            colourPicker.lowerBlue16();
            refresh();
        });

        // Lower blue 1
        setAction(30, (NetworkUser u) -> {
            colourPicker.lowerBlue1();
            refresh();
        });

        // Raise blue 1
        setAction(32, (NetworkUser u) -> {
            colourPicker.raiseBlue1();
            refresh();
        });

        // Raise blue 16
        setAction(33, (NetworkUser u) -> {
            colourPicker.raiseBlue16();
            refresh();
        });

        //-----------------------------------------------------
        //-------------- Line 5 - Colour display --------------
        //-----------------------------------------------------

        // Return/confirm
        setAction(40, (NetworkUser u) -> {
            // Saves the selected colour to the feature
            colourPicker.confirmColour();

            // Delete this gui.
            this.delete();
            u.mainGui = null;

            // Switch to feature list menu.
            parentFeatureMenu.refresh();
            u.mainGui = parentFeatureMenu;
            u.mainGui.open(u.player);
        });
    }
}
