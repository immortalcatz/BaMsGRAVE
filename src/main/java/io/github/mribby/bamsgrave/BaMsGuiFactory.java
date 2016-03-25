package io.github.mribby.bamsgrave;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BaMsGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return BaMsGuiFactoryConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class BaMsGuiFactoryConfigGui extends GuiConfig {
        public BaMsGuiFactoryConfigGui(GuiScreen parentScreen) {
            super(parentScreen, getConfigElements(), BaMsGrave.MOD_ID, false, false, I18n.format("bamsgrave.config.title"));
        }

        private static List<IConfigElement> getConfigElements() {
            List<IConfigElement> list = new ArrayList<IConfigElement>();
            list.addAll((new ConfigElement(BaMsGrave.config.getCategory(BaMsConfig.CATEGORY_GENERAL))).getChildElements());
            list.add(new DummyConfigElement.DummyCategoryElement("bamsgraveTombstoneCfg", "bamsgrave.config.ctgy.tombstone", TombstoneEntry.class));
            list.add(new DummyConfigElement.DummyCategoryElement("bamsgraveCoffinCfg", "bamsgrave.config.ctgy.coffin", CoffinEntry.class));
            return list;
        }

        public static class TombstoneEntry extends GuiConfigEntries.CategoryEntry {
            public TombstoneEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement prop) {
                super(owningScreen, owningEntryList, prop);
            }

            @Override
            protected GuiScreen buildChildScreen() {
                return new GuiConfig(owningScreen,
                        (new ConfigElement(BaMsGrave.config.getCategory(BaMsConfig.CATEGORY_TOMBSTONE))).getChildElements(),
                        owningScreen.modID, BaMsConfig.CATEGORY_TOMBSTONE, configElement.requiresWorldRestart() || owningScreen.allRequireWorldRestart,
                        configElement.requiresMcRestart() || owningScreen.allRequireMcRestart,
                        GuiConfig.getAbridgedConfigPath(BaMsGrave.config.toString()));
            }
        }

        public static class CoffinEntry extends GuiConfigEntries.CategoryEntry {
            public CoffinEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement prop) {
                super(owningScreen, owningEntryList, prop);
            }

            @Override
            protected GuiScreen buildChildScreen() {
                return new GuiConfig(owningScreen,
                        (new ConfigElement(BaMsGrave.config.getCategory(BaMsConfig.CATEGORY_COFFIN))).getChildElements(),
                        owningScreen.modID, BaMsConfig.CATEGORY_COFFIN, configElement.requiresWorldRestart() || owningScreen.allRequireWorldRestart,
                        configElement.requiresMcRestart() || owningScreen.allRequireMcRestart,
                        GuiConfig.getAbridgedConfigPath(BaMsGrave.config.toString()));
            }
        }
    }
}

