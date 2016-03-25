package io.github.mribby.bamsgrave;

import io.github.mribby.bamsgrave.repackage.baubles.api.BaublesApi;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.*;

public class GraveDigger {
    private final String name;
    private final IInventory inventory;
    private final World world;
    private final BlockPos pos;
    private final EnumFacing facing;
    private final EnumFacing oppositeFacing;

    private BlockPos wallPos;
    private BlockPos signPos;
    private BlockPos flowerPotPos;
    private BlockPos chestPos1;
    private BlockPos chestPos2;

    private EntityPlayer player;
    private boolean isCreative;
    private List<IInventory> inventories = new ArrayList<IInventory>();

    public GraveDigger(EntityPlayer player) {
        this(player.getDisplayName().getUnformattedText(), player.inventory, player.worldObj, new BlockPos(player.posX, player.posY, player.posZ), player.getHorizontalFacing());
        setPlayer(player);
        setCreative(player.capabilities.isCreativeMode);
        addInventory(BaublesApi.getBaubles(player));
    }

    /**
     * @param name      The name to put on the sign
     * @param inventory The inventory to store in the grave
     * @param world     The world
     * @param pos       The position of death
     * @param facing    The direction the player was facing upon death
     */
    public GraveDigger(String name, IInventory inventory, World world, BlockPos pos, EnumFacing facing) {
        this.name = name;
        this.inventory = inventory;
        this.world = world;
        this.pos = pos.getY() >= BaMsConfig.minimumHeight ? pos : new BlockPos(pos.getX(), BaMsConfig.minimumHeight, pos.getZ());
        this.facing = facing;
        this.oppositeFacing = facing.getOpposite();
        init();
    }

    private void init() {
        wallPos = pos.offset(facing);
        signPos = pos;
        flowerPotPos = wallPos.up();
        chestPos1 = pos.down(2);
        chestPos2 = chestPos1.offset(oppositeFacing);
    }

    public void setPlayer(EntityPlayer player) {
        this.player = player;
    }

    public void setCreative(boolean isCreative) {
        this.isCreative = isCreative;
    }

    public void addInventory(IInventory inventory) {
        if (inventory != null) {
            inventories.add(inventory);
        }
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
            if (!BaMsConfig.needChestToMakeCoffin || isCreative) {
                chestBlock = Blocks.chest;
                chestCount = 2;
            } else {
                return;
            }
        }

        // Dig the grave
        setTombstone();
        setCoffin(chestBlock, chestCount);
    }

    private void setTombstone() {
        // Check for solid ground if no wall exists
        boolean needsWall = !world.getBlockState(wallPos).getBlock().getMaterial().isSolid();
        BlockPos groundPos = wallPos.down();
        IBlockState groundState = world.getBlockState(groundPos);
        boolean isSolidGround = groundState.getBlock().getMaterial().isSolid();
        if (needsWall && !isSolidGround) {
            return;
        }

        // Find and take one sign
        boolean hasSign = false;
        if (BaMsConfig.needSignToMakeSign || isCreative) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack != null && stack.getItem() == Items.sign) {
                    inventory.decrStackSize(i, 1);
                    hasSign = true;
                    break;
                }
            }
        } else {
            hasSign = true;
        }

        // Place wall if needed
        if (needsWall && !BaMsConfig.setTombstoneWall(world, wallPos, groundState, 2)) {
            // Place standing sign if wall can't be placed
            if (hasSign) {
                int oppositeRotation = oppositeFacing.getHorizontalIndex() * 4;
                BaMsConfig.setBlockState(world, wallPos, Blocks.standing_sign.getDefaultState().withProperty(BlockStandingSign.ROTATION, oppositeRotation), 2);
                addSignEngraving(wallPos);
            }
            return;
        }

        // Place wall sign
        if (hasSign) {
            BaMsConfig.setBlockState(world, signPos, Blocks.wall_sign.getDefaultState().withProperty(BlockWallSign.FACING, oppositeFacing), 2);
            addSignEngraving(signPos);
        }

        // Place flower pot
        BaMsConfig.setFlowerPot(world, flowerPotPos, 2);
    }

    private void addSignEngraving(BlockPos signPos) {
        TileEntity te = world.getTileEntity(signPos);
        if (te instanceof TileEntitySign) {
            Date date = new Date();
            IChatComponent[] text = ((TileEntitySign) te).signText;
            text[0] = new ChatComponentText(name);
            text[2] = new ChatComponentText(BaMsConfig.getFormattedDate(date));
            text[3] = new ChatComponentText(BaMsConfig.getFormattedTime(date));
        }
    }

    private void setCoffin(Block chestBlock, int chestCount) {
        boolean isDouble = chestCount > 1;

        // Place chest 1
        if (BaMsConfig.setCoffin(world, chestPos1, chestBlock.getDefaultState(), 2)) {
            takeChest(chestBlock);
        } else {
            // Return because it's wrong to fill some other person's coffin
            return;
        }

        // Place chest 2 if double chest
        if (isDouble && BaMsConfig.setCoffin(world, chestPos2, chestBlock.getDefaultState(), 2)) {
            takeChest(chestBlock);
        }

        // Fill chest inventory
        fillChest();
    }

    private void takeChest(Block chestBlock) {
        if (BaMsConfig.needChestToMakeCoffin || isCreative) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack != null && Block.getBlockFromItem(stack.getItem()) == chestBlock) {
                    inventory.decrStackSize(i, 1);
                    break;
                }
            }
        }
    }

    private void fillChest() {
        TileEntityChest chest = null;

        // Get inventory from chest 1 or chest 2
        TileEntity te;
        if ((te = world.getTileEntity(chestPos1)) instanceof TileEntityChest) {
            chest = (TileEntityChest) te;
        } else if ((te = world.getTileEntity(chestPos2)) instanceof TileEntityChest) {
            chest = (TileEntityChest) te;
        }

        if (chest != null) {
            // Add inventory contents to chest inventory
            IInventory chestInv = BlockHelper.getChestInventory(chest);
            int slot = 0;
            for (int i = 0; i < inventory.getSizeInventory() && slot < chestInv.getSizeInventory(); i++) {
                ItemStack stack = inventory.removeStackFromSlot(i);
                if (stack != null) {
                    chestInv.setInventorySlotContents(slot++, stack);
                }
            }

            for (IInventory inventory : inventories) {
                for (int i = 0; i < inventory.getSizeInventory() && slot < chestInv.getSizeInventory(); i++) {
                    ItemStack stack = inventory.removeStackFromSlot(i);
                    if (stack != null) {
                        chestInv.setInventorySlotContents(slot++, stack);
                    }
                }
            }

            if (BaMsConfig.storeXP && player != null) {
                int xp = 0;

                // Force player to level up
                while (player.experience >= 1.0F) {
                    player.addExperience(0);
                }

                // Add XP from the XP bar
                if (player.experience < 0.0F) {
                    BaMsGrave.logger.warn("%s has %d experience!", player.getDisplayName().getUnformattedText(), player.experience);
                } else if (player.experienceLevel < 15) {
                    xp += (int) (player.experience * 17.0F + 0.5F);
                } else if (player.experienceLevel < 30) {
                    xp += (int) (player.experience * (17 + (player.experienceLevel - 15) * 3) + 0.5F);
                } else {
                    xp += (int) (player.experience * (62 + (player.experienceLevel - 30) * 7) + 0.5F);
                }

                // Add XP levels
                for (int level = player.experienceLevel; level > 0; level--){
                    if (level < 15) {
                        xp += 17;
                    } else if (level < 30) {
                        xp += 17 + (level - 15) * 3;
                    } else {
                        xp += 62 + (level - 30) * 7;
                    }
                }

                // For every 3-11 XP points, add an XP bottle
                while (xp > 0 && slot < chestInv.getSizeInventory()) {
                    ItemStack stack = chestInv.getStackInSlot(slot);
                    if (stack == null) {
                        chestInv.setInventorySlotContents(slot, stack = new ItemStack(Items.experience_bottle));
                    } else {
                        stack.stackSize++;
                    }
                    if (stack.stackSize == stack.getMaxStackSize()) {
                        slot++;
                    }
                    xp -= MathHelper.getRandomIntegerInRange(player.getRNG(), 3, 11);
                }

                // Reset XP and re-add leftovers
                player.experienceLevel = 0;
                player.experienceTotal = 0;
                player.experience = 0.0F;
                player.addExperience(Math.max(0, xp));
            }
        }
    }
}
