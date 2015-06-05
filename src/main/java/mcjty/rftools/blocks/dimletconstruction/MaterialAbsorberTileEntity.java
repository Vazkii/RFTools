package mcjty.rftools.blocks.dimletconstruction;

import mcjty.entity.GenericTileEntity;
import mcjty.rftools.blocks.RFToolsTools;
import mcjty.rftools.items.dimlets.DimletKey;
import mcjty.rftools.items.dimlets.DimletObjectMapping;
import mcjty.varia.BlockMeta;
import mcjty.varia.Coordinate;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.*;

public class MaterialAbsorberTileEntity extends GenericTileEntity {
    private static final int ABSORB_SPEED = 2;

    private int absorbing = 0;
    private int blockID = -1;
    private int meta = 0;
    private int timer = ABSORB_SPEED;
    private Set<Coordinate> toscan = new HashSet<Coordinate>();

    @Override
    protected void checkStateClient() {
        if (absorbing > 0) {
            Random rand = worldObj.rand;

            double u = rand.nextFloat() * 2.0f - 1.0f;
            double v = (float) (rand.nextFloat() * 2.0f * Math.PI);
            double x = Math.sqrt(1 - u * u) * Math.cos(v);
            double y = Math.sqrt(1 - u * u) * Math.sin(v);
            double z = u;
            double r = 1.0f;

            worldObj.spawnParticle("portal", xCoord + 0.5f + x * r, yCoord + 0.5f + y * r, zCoord + 0.5f + z * r, -x, -y, -z);
        }
    }

    private void checkBlock(Coordinate c, ForgeDirection direction) {
        Coordinate c2 = c.addDirection(direction);
        Block block = blockMatches(c2);
        if (block != null) {
            toscan.add(c2);
        }
    }

    private Block blockMatches(Coordinate c2) {
        Block block = worldObj.getBlock(c2.getX(), c2.getY(), c2.getZ());
        if (block == null || block.getMaterial() == Material.air) {
            return null;
        }
        int m = worldObj.getBlockMetadata(c2.getX(), c2.getY(), c2.getZ());
        int id = Block.blockRegistry.getIDForObject(block);
        return (id == blockID && m == meta) ? block : null;
    }

    private Block isValidSourceBlock(Coordinate coordinate) {
        Block block = worldObj.getBlock(coordinate.getX(), coordinate.getY(), coordinate.getZ());
        if (block == null || block.getMaterial() == Material.air) {
            return null;
        }
        int m = worldObj.getBlockMetadata(coordinate.getX(), coordinate.getY(), coordinate.getZ());
        boolean ok = isValidDimletBlock(block, m);
        return ok ? block : null;
    }


    @Override
    protected void checkStateServer() {
        if (absorbing > 0 || blockID == -1) {
            timer--;
            if (timer <= 0) {
                timer = ABSORB_SPEED;
                Block b = isValidSourceBlock(new Coordinate(xCoord, yCoord - 1, zCoord));
                if (b != null) {
                    int id = Block.blockRegistry.getIDForObject(b);
                    if (blockID == -1) {
                        absorbing = DimletConstructionConfiguration.maxBlockAbsorbtion;
                        blockID = id;
                        meta = worldObj.getBlockMetadata(xCoord, yCoord - 1, zCoord);
                        toscan.clear();
                    }
                    toscan.add(new Coordinate(xCoord, yCoord - 1, zCoord));
                }

                if (!toscan.isEmpty()) {
                    int r = worldObj.rand.nextInt(toscan.size());
                    Iterator<Coordinate> iterator = toscan.iterator();
                    Coordinate c = null;
                    for (int i = 0 ; i <= r ; i++) {
                        c = iterator.next();
                    }
                    toscan.remove(c);
                    checkBlock(c, ForgeDirection.DOWN);
                    checkBlock(c, ForgeDirection.UP);
                    checkBlock(c, ForgeDirection.EAST);
                    checkBlock(c, ForgeDirection.WEST);
                    checkBlock(c, ForgeDirection.SOUTH);
                    checkBlock(c, ForgeDirection.NORTH);

                    Block block = blockMatches(c);
                    if (block != null) {
                        RFToolsTools.playSound(worldObj, block.stepSound.getBreakSound(), xCoord, yCoord, zCoord, 1.0f, 1.0f);
                        worldObj.setBlockToAir(c.getX(), c.getY(), c.getZ());
                        absorbing--;
                        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    }
                }
            }
            markDirty();
        }
    }

    private boolean isValidDimletBlock(Block block, int m) {
        boolean ok = false;
        for (Map.Entry<DimletKey, BlockMeta> entry : DimletObjectMapping.idToBlock.entrySet()) {
            BlockMeta blockMeta = entry.getValue();
            if (blockMeta != null && blockMeta.getBlock() == block && blockMeta.getMeta() == m) {
                ok = true;
                break;
            }
        }
        return ok;
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        int[] x = new int[toscan.size()];
        int[] y = new int[toscan.size()];
        int[] z = new int[toscan.size()];
        int i = 0;
        for (Coordinate c : toscan) {
            x[i] = c.getX();
            y[i] = c.getY();
            z[i] = c.getZ();
            i++;
        }
        tagCompound.setIntArray("toscanx", x);
        tagCompound.setIntArray("toscany", y);
        tagCompound.setIntArray("toscanz", z);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        tagCompound.setInteger("absorbing", absorbing);
        tagCompound.setInteger("block", blockID);
        tagCompound.setInteger("meta", meta);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        int[] x = tagCompound.getIntArray("toscanx");
        int[] y = tagCompound.getIntArray("toscany");
        int[] z = tagCompound.getIntArray("toscanz");
        toscan.clear();
        for (int i = 0 ; i < x.length ; i++) {
            toscan.add(new Coordinate(x[i], y[i], z[i]));
        }
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        absorbing = tagCompound.getInteger("absorbing");
        blockID = tagCompound.getInteger("block");
        meta = tagCompound.getInteger("meta");
    }


}

