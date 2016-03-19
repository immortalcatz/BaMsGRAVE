package io.github.mribby.bamsgrave;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GraveDigger {
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final String name;
    private final IInventory inventory;
    private final World world;
    private final BlockPos pos;
    private final EnumFacing facing;
    private final EnumFacing oppositeFacing;

    private BlockPos wallPos;
    private BlockPos signPos;
    private BlockPos chestPos;

    public GraveDigger(EntityPlayer player) {
        this(player.getDisplayName().getUnformattedText(), player.inventory, player.worldObj, new BlockPos(player.posX, player.posY, player.posZ), player.getHorizontalFacing());
    }

    /**
     * @param name      The name to put on the sign
     * @param inventory The inventory to store in the grave
     * @param world     The world
     * @param pos       The position of death
     * @param facing    The direction the player was facing upon death
     */
    private GraveDigger(String name, IInventory inventory, World world, BlockPos pos, EnumFacing facing) {
        this.name = name;
        this.inventory = inventory;
        this.world = world;
        this.pos = pos;
        this.facing = facing;
        this.oppositeFacing = facing.getOpposite();
        init();
    }

    private void init() {
        wallPos = pos.offset(facing);
        signPos = pos;
        chestPos = pos.down(2);
    }

    public void dig() {
        // Create a map of chests in the inventory
        HashMap<Block, Integer> chestCounts = new HashMap<Block, Integer>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null) {
                Block block = Block.getBlockFromItem(stack.getItem());
                if (block instanceof BlockChest) {
                    Integer count = chestCounts.get(block);
                    if (count != null) {
                        chestCounts.put(block, count + stack.stackSize);
                    } else {
                        chestCounts.put(block, stack.stackSize);
                    }
                }
            }
        }

        // Find the most abundant chest
        Block chestBlock = null;
        int chestCount = 0;
        for (Map.Entry<Block, Integer> entry : chestCounts.entrySet()) {
            if (entry.getValue() > chestCount) {
                chestBlock = entry.getKey();
                chestCount = entry.getValue();
            }
        }

        // Return if no chest found
        if (chestBlock == null) {
            return;
        }

        // Dig the grave
        setTombstone();
        setCoffin(chestBlock, chestCount);
    }

    private void setTombstone() {
        // Check for solid ground if no wall exists
        boolean needsWall = !getBlock(wallPos).getMaterial().isSolid();
        BlockPos groundPos = wallPos.down();
        Block groundBlock = getBlock(groundPos);
        boolean isSolidGround = groundBlock.getMaterial().isSolid();
        if (needsWall && !isSolidGround) {
            return;
        }

        // Find and take one sign
        boolean hasSign = false;
        if (BaMsGrave.needSignToMakeSign) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack != null && stack.getItem() == Items.sign) {
                    inventory.decrStackSize(i, 1);
                    hasSign = true;
                    break;
                }
            }
        }

        if (hasSign || !BaMsGrave.needSignToMakeSign) {
            // Place wall if needed
            if (needsWall && !setBlock(wallPos, groundBlock)) {
                // Place standing sign if wall can't be placed
                setBlockState(wallPos, Blocks.standing_sign.getDefaultState().withProperty(BlockStandingSign.ROTATION, 0));
                addSignEngraving(wallPos);
                return;
            }

            // Place the sign
            setBlockState(signPos, Blocks.wall_sign.getDefaultState().withProperty(BlockWallSign.FACING, oppositeFacing));
            addSignEngraving(signPos);
        }
    }

    private void addSignEngraving(BlockPos signPos) {
        TileEntity te = world.getTileEntity(signPos);
        if (te instanceof TileEntitySign) {
            Date date = new Date();
            IChatComponent[] text = ((TileEntitySign) te).signText;
            text[0].appendText(name);
            text[2].appendText(DAY_FORMAT.format(date));
            text[3].appendText(TIME_FORMAT.format(date));
        }
    }

    private void setCoffin(Block chestBlock, int chestCount) {
        boolean isDouble = chestCount > 1;

        // Remove the needed number of chests from inventory
        if (BaMsGrave.needChestToMakeGrave) {
            int chestsNeeded = isDouble ? 2 : 1;
            for (int i = 0; i < inventory.getSizeInventory() && chestsNeeded > 0; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack != null && Block.getBlockFromItem(stack.getItem()) == chestBlock) {
                    ItemStack removed = inventory.decrStackSize(i, chestsNeeded);
                    chestsNeeded -= removed.stackSize;
                }
            }
        }

        // Place chest(s)
        setBlock(chestPos, chestBlock);
        if (isDouble) {
            setBlock(chestPos.offset(oppositeFacing), chestBlock);
        }

        // Fill chest inventory
        TileEntity te = world.getTileEntity(chestPos);
        if (te instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) te;
            IInventory chestInv = isDouble ? getChestInventory(chest) : chest;
            int slot = 0;
            for (int i = 0; i < inventory.getSizeInventory() && slot < chestInv.getSizeInventory(); i++) {
                ItemStack stack = inventory.removeStackFromSlot(i);
                if (stack != null) {
                    chestInv.setInventorySlotContents(slot++, stack);
                }
            }
        }
    }

    private Block getBlock(BlockPos pos) {
        return getBlockState(pos).getBlock();
    }

    private IBlockState getBlockState(BlockPos pos) {
        return world.getBlockState(pos);
    }

    /**
     * Sets default block state at specified pos
     *
     * @return true if successful
     */
    private boolean setBlock(BlockPos pos, Block block) {
        return setBlockState(pos, block.getDefaultState());
    }

    /**
     * Sets block state at specified pos
     *
     * @return true if successful
     */
    private boolean setBlockState(BlockPos pos, IBlockState state) {
        return world.setBlockState(pos, state, 2);
    }

    private static IInventory getChestInventory(TileEntityChest chest) {
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
