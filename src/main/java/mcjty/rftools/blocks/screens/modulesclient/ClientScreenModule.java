package mcjty.rftools.blocks.screens.modulesclient;

import mcjty.lib.gui.widgets.Panel;
import mcjty.rftools.blocks.screens.ModuleGuiChanged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public interface ClientScreenModule {
    public enum TransformMode {
        NONE,
        TEXT,
        TEXTLARGE,
        ITEM
    }

    TransformMode getTransformMode();

    int getHeight();

    void render(FontRenderer fontRenderer, int currenty, Object[] screenData, float factor);

    void mouseClick(World world, int x, int y, boolean clicked);

    Panel createGui(Minecraft mc, Gui gui, NBTTagCompound currentData, ModuleGuiChanged moduleGuiChanged);

    void setupFromNBT(NBTTagCompound tagCompound, int dim, int x, int y, int z);

    // Return true if this module needs server data.
    boolean needsServerData();
}
