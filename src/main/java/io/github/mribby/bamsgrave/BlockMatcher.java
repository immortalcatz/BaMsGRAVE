package io.github.mribby.bamsgrave;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

public class BlockMatcher {
    public static BlockMatcher parse(String s) throws Exception {
        if (s.startsWith(BaMsConfig.CLASS_PREFIX)) {
            String className = s.substring(BaMsConfig.CLASS_PREFIX.length());
            className = FMLDeobfuscatingRemapper.INSTANCE.map(className).replace('/', '.');
            return new BlockMatcher(Class.forName(className));
        }

        ResourceLocation name = new ResourceLocation(s);
        if (BaMsConfig.ORE_DICT_ID.equals(name.getResourceDomain())) {
            return new BlockMatcher(name.getResourcePath());
        }

        IBlockState state = BlockHelper.parseBlockState(s);
        if (state != null) {
            return new BlockMatcher(state);
        }

        return null;
    }

    private final Object value;

    public BlockMatcher(IBlockState state) {
        this.value = state;
    }

    public BlockMatcher(String name) {
        this.value = name;
    }

    public BlockMatcher(Class clazz) {
        this.value = clazz;
    }

    public boolean matches(IBlockState state) {
        if (value instanceof IBlockState) {
            return value == state;
        }

        if (value instanceof String) {
            Block block = state.getBlock();
            int meta = block.getMetaFromState(state);
            ItemStack stack = new ItemStack(block, 1, meta);
            if (stack.getItem() == null) {
                return false;
            }
            if (!stack.getHasSubtypes()) {
                stack = new ItemStack(block);
            }
            List<ItemStack> ores = OreDictionary.getOres((String) value, false);
            for (ItemStack ore : ores) {
                if (ore.isItemEqual(stack)) {
                    return true;
                }
            }
        }

        if (value instanceof Class) {
            return ((Class) value).isInstance(state.getBlock());
        }

        return false;
    }
}
