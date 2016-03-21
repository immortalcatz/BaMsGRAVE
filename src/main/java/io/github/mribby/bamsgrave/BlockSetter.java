package io.github.mribby.bamsgrave;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class BlockSetter {
    public static BlockSetter parse(String s) {
        String[] args = s.split(" ");
        if (args.length == 1) {
            String name = args[0];
            Block block = BlockHelper.parseBlock(name);
            if (block == null) {
                BaMsGrave.logger.error("Invalid block: %s", name);
                return null;
            }
            return new BlockSetter(block);
        } else if (args.length >= 2) {
            String name = args[0];
            String dataValue = args[1];
            IBlockState state = BlockHelper.parseBlockState(name, dataValue);
            if (state == null) {
                BaMsGrave.logger.error("Invalid block state: %s %s", name, dataValue);
                return null;
            }
            if (args.length >= 3) {
                String dataTag = args[2];
                try {
                    return new BlockSetter(state, JsonToNBT.getTagFromJson(dataTag));
                } catch (NBTException e) {
                    BaMsGrave.logger.error("Invalid dataTag: %s", dataTag);
                }
            }
            return new BlockSetter(state);
        }
        return null;
    }

    private final IBlockState state;
    private final NBTTagCompound dataTag;

    public BlockSetter(Block block) {
        this(block, 0);
    }

    public BlockSetter(Block block, int dataValue) {
        this(block, dataValue, null);
    }

    public BlockSetter(Block block, int dataValue, NBTTagCompound dataTag) {
        this(block.getStateFromMeta(dataValue), dataTag);
    }

    public BlockSetter(IBlockState state) {
        this(state, null);
    }

    public BlockSetter(IBlockState state, NBTTagCompound dataTag) {
        this.state = state;
        this.dataTag = dataTag;
    }

    public boolean setBlock(World world, BlockPos pos, int flags) {
        //if (destroy) world.destroyBlock(pos, true);

        TileEntity oldte = world.getTileEntity(pos);

        if (oldte instanceof IInventory) {
            ((IInventory) oldte).clear();
        }

        if (!BaMsConfig.setBlockState(world, pos, state, flags)) {
            return false;
        } else {
            if (state.getBlock().hasTileEntity(state) && dataTag != null) {
                TileEntity te = world.getTileEntity(pos);
                if (te != null) {
                    dataTag.setInteger("x", pos.getX());
                    dataTag.setInteger("y", pos.getY());
                    dataTag.setInteger("z", pos.getZ());
                    te.readFromNBT(dataTag);
                }
            }
            return true;
        }
    }
}
