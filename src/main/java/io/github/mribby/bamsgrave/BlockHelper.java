package io.github.mribby.bamsgrave;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class BlockHelper {
    /**
     * @param name Block name (i.e. minecraft:stone)
     * @return null if block does not exist
     */
    public static Block parseBlock(String name) {
        ResourceLocation rl = new ResourceLocation(name);
        Block block = Block.blockRegistry.getObject(rl);
        if (block == Blocks.air && !Block.blockRegistry.containsKey(rl)) {
            return null;
        }
        return block;
    }

    public static IBlockState parseBlockState(String s) {
        return parseBlockState(s.split(" "));
    }

    public static IBlockState parseBlockState(String... args) {
        if (args.length == 0) return null;
        Block block = parseBlock(args[0]);
        if (block == null) return null;
        IBlockState state;
        if (args.length == 1) {
            state = block.getDefaultState();
        } else {
            String dataValue = args[1];
            try {
                state = block.getStateFromMeta(Integer.parseInt(dataValue));
            } catch (NumberFormatException e) {
                BaMsGrave.logger.error("Invalid metadata: %s", dataValue);
                state = block.getDefaultState();
            }
        }
        return state;
    }

    public static IInventory getChestInventory(TileEntityChest chest) {
        for (EnumFacing face : EnumFacing.Plane.HORIZONTAL) {
            BlockPos offsetPos = chest.getPos().offset(face);
            if (chest.getWorld().getBlockState(offsetPos).getBlock() == chest.getBlockType()) {
                TileEntity te = chest.getWorld().getTileEntity(offsetPos);
                if (te instanceof TileEntityChest) {
                    TileEntityChest chest2 = (TileEntityChest) te;
                    if (face != EnumFacing.WEST && face != EnumFacing.NORTH) {
                        return new InventoryLargeChest("container.chestDouble", chest, chest2);
                    } else {
                        return new InventoryLargeChest("container.chestDouble", chest2, chest);
                    }
                }
            }
        }
        return chest;
    }
}
