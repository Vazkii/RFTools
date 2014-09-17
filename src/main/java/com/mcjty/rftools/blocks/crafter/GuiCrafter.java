package com.mcjty.rftools.blocks.crafter;

import com.mcjty.gui.Window;
import com.mcjty.gui.events.ChoiceEvent;
import com.mcjty.gui.events.SelectionEvent;
import com.mcjty.gui.layout.HorizontalAlignment;
import com.mcjty.gui.layout.HorizontalLayout;
import com.mcjty.gui.layout.PositionalLayout;
import com.mcjty.gui.layout.VerticalAlignment;
import com.mcjty.gui.widgets.*;
import com.mcjty.gui.widgets.Label;
import com.mcjty.gui.widgets.Panel;
import com.mcjty.rftools.BlockInfo;
import com.mcjty.rftools.RFTools;
import com.mcjty.rftools.network.PacketHandler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import java.awt.*;

public class GuiCrafter extends GuiContainer {
    public static final int CRAFTER_WIDTH = 256;
    public static final int CRAFTER_HEIGHT = 224;

    private Window window;
    private EnergyBar energyBar;
    private WidgetList recipeList;
    private ChoiceLabel keepItem;
    private ChoiceLabel internalRecipe;

    private final CrafterBlockTileEntity crafterBlockTileEntity;

    private static final ResourceLocation iconLocation = new ResourceLocation(RFTools.MODID, "textures/gui/crafter.png");

    @Override
    public void initGui() {
        super.initGui();

        int maxEnergyStored = crafterBlockTileEntity.getMaxEnergyStored(ForgeDirection.DOWN);
        energyBar = new EnergyBar(mc, this).setVertical().setMaxValue(maxEnergyStored).setLayoutHint(new PositionalLayout.PositionalHint(12, 141, 8, 76)).setShowText(false);
        energyBar.setValue(crafterBlockTileEntity.getCurrentRF());

        keepItem = new ChoiceLabel(mc, this).
                addChoices("All", "Keep").
                setHorizontalAlignment(HorizontalAlignment.ALIGN_CENTER).
                setVerticalAlignment(VerticalAlignment.ALIGN_CENTER).
                addChoiceEvent(new ChoiceEvent() {
                    @Override
                    public void choiceChanged(Widget parent, String newChoice) {
                        updateRecipe();
                    }
                }).
                setLayoutHint(new PositionalLayout.PositionalHint(150, 7, 38, 14));
        internalRecipe = new ChoiceLabel(mc, this).
                addChoices("Ext", "Int").
                setHorizontalAlignment(HorizontalAlignment.ALIGN_CENTER).
                setVerticalAlignment(VerticalAlignment.ALIGN_CENTER).
                addChoiceEvent(new ChoiceEvent() {
                    @Override
                    public void choiceChanged(Widget parent, String newChoice) {
                        updateRecipe();
                    }
                }).
                setLayoutHint(new PositionalLayout.PositionalHint(150, 24, 38, 14));

        recipeList = new WidgetList(mc, this).
                setRowheight(16).
                addSelectionEvent(new SelectionEvent() {
                    @Override
                    public void select(Widget parent, int index) {
                        selectRecipe();
                    }
                }).
                setLayoutHint(new PositionalLayout.PositionalHint(10, 7, 125, 80));
        populateList();

        Slider listSlider = new Slider(mc, this).setVertical().setScrollable(recipeList).setLayoutHint(new PositionalLayout.PositionalHint(137, 7, 11, 80));

        Widget toplevel = new Panel(mc, this).setBackground(iconLocation).setLayout(new PositionalLayout()).addChild(energyBar).addChild(keepItem).addChild(internalRecipe).
                addChild(recipeList).addChild(listSlider);
        toplevel.setBounds(new Rectangle(guiLeft, guiTop, xSize, ySize));

        selectRecipe();
        sendChangeToServer(-1, null, false, false);

        window = new Window(this, toplevel);
    }

    private void populateList() {
        recipeList.removeChildren();
        for (int i = 0 ; i < 8 ; i++) {
            CraftingRecipe recipe = crafterBlockTileEntity.getRecipe(i);
            ItemStack stack = recipe.getItemStack(9);
            addRecipeLine(stack);
        }
    }

    private void addRecipeLine(Object craftingResult) {
        Panel panel = new Panel(mc, this).setLayout(new HorizontalLayout()).
                addChild(new BlockRender(mc, this).setRenderItem(craftingResult)).
                addChild(new Label(mc, this).setText(BlockInfo.getReadableName(craftingResult, 0)));
        recipeList.addChild(panel);
    }

    private void selectRecipe() {
        int selected = recipeList.getSelected();
        if (selected == -1) {
            for (int i = 0 ; i < 10 ; i++) {
                inventorySlots.getSlot(i).putStack(null);
            }
            keepItem.setChoice("All");
            internalRecipe.setChoice("Ext");
            return;
        }
        CraftingRecipe craftingRecipe = crafterBlockTileEntity.getRecipe(selected);
        for (int i = 0 ; i < 10 ; i++) {
            inventorySlots.getSlot(i).putStack(craftingRecipe.getItemStack(i));
        }
        keepItem.setChoice(craftingRecipe.isKeepOne() ? "Keep" : "All");
        internalRecipe.setChoice(craftingRecipe.isCraftInternal() ? "Int" : "Ext");
    }

    private void rememberRecipe() {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer var1) {
                return false;
            }
        }, 3, 3);

        for(int i=0 ; i<9 ; i++) {
            inv.setInventorySlotContents(i, inventorySlots.getSlot(i).getStack());
        }
        ItemStack matches = CraftingManager.getInstance().findMatchingRecipe(inv, mc.theWorld);
        // Compare current contents to avoid unneeded slot update.
        ItemStack oldStack = inventorySlots.getSlot(9).getStack();
        if (!itemStacksEqual(matches, oldStack)) {
            inventorySlots.getSlot(9).putStack(matches);
        }

        int selected = recipeList.getSelected();
        if (selected == -1) {
            return;
        }
        CraftingRecipe craftingRecipe = crafterBlockTileEntity.getRecipe(selected);
        boolean dirty = false;
        ItemStack[] items = new ItemStack[10];
        for (int i = 0 ; i < 10 ; i++) {
            items[i] = inventorySlots.getSlot(i).getStack();
            ItemStack oldItem = craftingRecipe.getItemStack(i);
            if (!itemStacksEqual(oldItem, items[i])) {
                dirty = true;
            }
        }

        if (dirty) {
            craftingRecipe.setRecipe(items);
            updateRecipe();
            populateList();
        }
    }

    private void updateRecipe() {
        int selected = recipeList.getSelected();
        if (selected == -1) {
            return;
        }
        CraftingRecipe craftingRecipe = crafterBlockTileEntity.getRecipe(selected);
        boolean keepOne = "Keep".equals(keepItem.getCurrentChoice());
        boolean craftInternal = "Int".equals(internalRecipe.getCurrentChoice());
        craftingRecipe.setKeepOne(keepOne);
        craftingRecipe.setCraftInternal(craftInternal);
        sendChangeToServer(selected, craftingRecipe.getItems(), keepOne, craftInternal);
    }

    private boolean itemStacksEqual(ItemStack matches, ItemStack oldStack) {
        if (matches == null) {
            return oldStack == null;
        } else if (oldStack == null) {
            return false;
        } else {
            return matches.isItemEqual(oldStack);
        }
    }

    private void sendChangeToServer(int index, ItemStack[] items, boolean keepOne, boolean craftInternal) {
        PacketHandler.INSTANCE.sendToServer(new PacketCrafter(crafterBlockTileEntity.xCoord, crafterBlockTileEntity.yCoord, crafterBlockTileEntity.zCoord, index, items,
                keepOne, craftInternal));
    }

    public GuiCrafter(CrafterBlockTileEntity crafterBlockTileEntity, CrafterContainer container) {
        super(container);
        this.crafterBlockTileEntity = crafterBlockTileEntity;
        crafterBlockTileEntity.setOldRF(-1);
        crafterBlockTileEntity.setCurrentRF(crafterBlockTileEntity.getEnergyStored(ForgeDirection.DOWN));

        xSize = CRAFTER_WIDTH;
        ySize = CRAFTER_HEIGHT;
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3) {
        super.drawScreen(par1, par2, par3);
        rememberRecipe();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int i2) {
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int i, int i2) {
        window.draw();
        int currentRF = crafterBlockTileEntity.getCurrentRF();
        energyBar.setValue(currentRF);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        window.mouseClicked(x, y, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        window.handleMouseInput();
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        super.mouseMovedOrUp(x, y, button);
        window.mouseMovedOrUp(x, y, button);
    }
}
